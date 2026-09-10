#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONTEND_DIR="$PROJECT_DIR/frontend"
RUNTIME_DIR="$PROJECT_DIR/.runtime"
RUNTIME_ENV="$RUNTIME_DIR/demo.env"
COMPOSE_FILE="$PROJECT_DIR/compose.yaml"
COMPOSE_PROJECT="srm-agent-demo"
MYSQL_CONTAINER="srm-agent-local-mysql"
MYSQL_VOLUME="srm-agent-local-mysql-data"
MYSQL_DATABASE="srm_agent_demo"
MYSQL_USER="srm_agent_demo_app"
BACKEND_PORT="${SRM_DEMO_BACKEND_PORT:-18080}"
FRONTEND_PORT="${SRM_DEMO_FRONTEND_PORT:-15173}"
MYSQL_PORT="${SRM_DEMO_MYSQL_PORT:-13306}"

log() { printf '[srm-demo] %s\n' "$*"; }
die() { printf '[srm-demo] ERROR: %s\n' "$*" >&2; exit 1; }
require_command() { command -v "$1" >/dev/null 2>&1 || die "missing required command: $1"; }

ensure_runtime_dir() {
  mkdir -p "$RUNTIME_DIR"
  chmod 700 "$RUNTIME_DIR"
}

random_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 24
  else
    LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 48
  fi
}

ensure_runtime_env() {
  ensure_runtime_dir
  if [[ -f "$RUNTIME_ENV" ]]; then
    chmod 600 "$RUNTIME_ENV"
    return
  fi
  if docker volume inspect "$MYSQL_VOLUME" >/dev/null 2>&1; then
    die "$MYSQL_VOLUME exists but .runtime/demo.env is missing; refusing to replace its credentials"
  fi
  local root_secret app_secret temp_env
  root_secret="$(random_secret)"
  app_secret="$(random_secret)"
  temp_env="$RUNTIME_ENV.tmp.$$"
  umask 077
  {
    printf 'SRM_DEMO_DB_ROOT_PASSWORD=%s\n' "$root_secret"
    printf 'SRM_DEMO_DB_APP_PASSWORD=%s\n' "$app_secret"
    printf 'SRM_DEMO_MYSQL_PORT=%s\n' "$MYSQL_PORT"
  } >"$temp_env"
  mv "$temp_env" "$RUNTIME_ENV"
  chmod 600 "$RUNTIME_ENV"
}

load_runtime_env() {
  [[ -f "$RUNTIME_ENV" ]] || die "missing $RUNTIME_ENV; run scripts/demo-up.sh first"
  set -a
  # shellcheck disable=SC1090
  source "$RUNTIME_ENV"
  set +a
}

compose() {
  docker compose --project-name "$COMPOSE_PROJECT" --env-file "$RUNTIME_ENV" -f "$COMPOSE_FILE" "$@"
}

assert_docker_ready() {
  require_command docker
  docker info >/dev/null 2>&1 || die "Docker Engine is not running"
  docker compose version >/dev/null 2>&1 || die "Docker Compose v2 is required"
}

assert_container_owned() {
  if docker container inspect "$MYSQL_CONTAINER" >/dev/null 2>&1; then
    local project_label
    project_label="$(docker container inspect --format '{{ index .Config.Labels "com.docker.compose.project" }}' "$MYSQL_CONTAINER")"
    [[ "$project_label" == "$COMPOSE_PROJECT" ]] || die "$MYSQL_CONTAINER exists but is not owned by Compose project $COMPOSE_PROJECT"
  fi
}

wait_for_mysql() {
  local deadline=$((SECONDS + 90)) status
  while (( SECONDS < deadline )); do
    status="$(docker container inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$MYSQL_CONTAINER" 2>/dev/null || true)"
    [[ "$status" == "healthy" ]] && return
    sleep 2
  done
  die "MySQL did not become healthy within 90 seconds"
}

mysql_root() {
  compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 "$@"' sh "$@"
}

mysql_app() {
  compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u"$MYSQL_USER" --default-character-set=utf8mb4 "$@"' sh "$@"
}

apply_srm_sql() {
  local sql
  for sql in "$PROJECT_DIR"/src/main/resources/db/srm/001_srm_schema.sql \
             "$PROJECT_DIR"/src/main/resources/db/srm/002_srm_demo_data.sql \
             "$PROJECT_DIR"/src/main/resources/db/srm/003_srm_step3_migration.sql; do
    mysql_root "$MYSQL_DATABASE" <"$sql"
  done
}

port_listener() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN -t 2>/dev/null | head -n 1 || true
}

assert_port_free() {
  local port="$1" service="$2" pid_file="$3" listener
  listener="$(port_listener "$port")"
  if [[ -n "$listener" ]]; then
    if [[ -f "$pid_file" ]] && [[ "$(cat "$pid_file")" == "$listener" ]]; then return; fi
    die "port $port is already used by PID $listener; refusing to replace it for $service"
  fi
}

process_owned() {
  local pid="$1" marker="$2" command_line
  kill -0 "$pid" 2>/dev/null || return 1
  command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  [[ "$command_line" == *"$marker"* ]]
}

stop_recorded_process() {
  local name="$1" pid_file="$2" marker="$3"
  [[ -f "$pid_file" ]] || return 0
  local pid
  pid="$(cat "$pid_file")"
  [[ "$pid" =~ ^[0-9]+$ ]] || die "invalid PID file: $pid_file"
  if process_owned "$pid" "$marker"; then
    log "stopping $name (PID $pid)"
    kill "$pid"
    local deadline=$((SECONDS + 20))
    while kill -0 "$pid" 2>/dev/null && (( SECONDS < deadline )); do sleep 1; done
    kill -0 "$pid" 2>/dev/null && die "$name did not stop cleanly"
  elif kill -0 "$pid" 2>/dev/null; then
    die "PID $pid is not owned by this project; refusing to stop it"
  fi
  rm -f "$pid_file"
}

find_maven() {
  if [[ -n "${MVN_BIN:-}" ]]; then
    [[ -x "$MVN_BIN" ]] || die "MVN_BIN is not executable: $MVN_BIN"
    printf '%s\n' "$MVN_BIN"
    return
  fi
  if command -v mvn >/dev/null 2>&1; then command -v mvn; return; fi
  local fallback="/private/tmp/srm-knowledge-engine-tools/apache-maven-3.9.9/bin/mvn"
  [[ -x "$fallback" ]] && { printf '%s\n' "$fallback"; return; }
  fallback="/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn"
  [[ -x "$fallback" ]] && { printf '%s\n' "$fallback"; return; }
  fallback="/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn"
  [[ -x "$fallback" ]] && { printf '%s\n' "$fallback"; return; }
  die "Maven 3.9+ not found; install Maven or set MVN_BIN to an existing executable"
}

wait_http() {
  local url="$1" label="$2" deadline=$((SECONDS + 90))
  while (( SECONDS < deadline )); do
    if curl -fsS --max-time 2 "$url" >/dev/null 2>&1; then return; fi
    sleep 2
  done
  die "$label did not become healthy: $url"
}

start_backend() {
  local pid_file="$RUNTIME_DIR/backend.pid" log_file="$RUNTIME_DIR/backend.log" jar mvn
  if [[ -f "$pid_file" ]] && process_owned "$(cat "$pid_file")" "$PROJECT_DIR"; then return; fi
  rm -f "$pid_file"
  assert_port_free "$BACKEND_PORT" backend "$pid_file"
  mvn="$(find_maven)"
  log "building backend (offline=${SRM_MAVEN_OFFLINE:-true})"
  if [[ "${SRM_MAVEN_OFFLINE:-true}" == "true" ]]; then
    "$mvn" -o -f "$PROJECT_DIR/pom.xml" -DskipTests clean package
  else
    "$mvn" -f "$PROJECT_DIR/pom.xml" -DskipTests clean package
  fi
  jar="$PROJECT_DIR/target/srm-agent-1.0.0-SNAPSHOT.jar"
  [[ -f "$jar" ]] || die "backend jar not produced: $jar"
  load_runtime_env
  (
    export SERVER_PORT="$BACKEND_PORT"
    export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:${MYSQL_PORT}/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
    export SPRING_DATASOURCE_USERNAME="$MYSQL_USER"
    export SPRING_DATASOURCE_PASSWORD="$SRM_DEMO_DB_APP_PASSWORD"
    export SRM_RUNTIME_MODE=DEMO
    export SRM_KNOWLEDGE_ENGINE_ENABLED=false
    exec nohup java -jar "$jar"
  ) </dev/null >>"$log_file" 2>&1 &
  printf '%s\n' "$!" >"$pid_file"
  wait_http "http://127.0.0.1:${BACKEND_PORT}/api/srm/agent/traces" backend
}

start_frontend() {
  local pid_file="$RUNTIME_DIR/frontend.pid" log_file="$RUNTIME_DIR/frontend.log" vite="$FRONTEND_DIR/node_modules/.bin/vite"
  if [[ -f "$pid_file" ]] && process_owned "$(cat "$pid_file")" "$FRONTEND_DIR"; then return; fi
  rm -f "$pid_file"
  assert_port_free "$FRONTEND_PORT" frontend "$pid_file"
  if [[ ! -x "$vite" ]]; then
    if [[ "${SRM_ALLOW_NPM_INSTALL:-false}" == "true" ]]; then
      log "frontend dependencies missing; running explicit npm ci"
      npm --prefix "$FRONTEND_DIR" ci
    else
      die "frontend/node_modules is missing; run npm ci yourself or set SRM_ALLOW_NPM_INSTALL=true"
    fi
  fi
  (
    cd "$FRONTEND_DIR"
    export VITE_SRM_API_BASE=''
    export VITE_SRM_API_PROXY_TARGET="http://127.0.0.1:${BACKEND_PORT}"
    exec nohup "$vite" --host 127.0.0.1 --port "$FRONTEND_PORT" --strictPort
  ) </dev/null >>"$log_file" 2>&1 &
  printf '%s\n' "$!" >"$pid_file"
  wait_http "http://127.0.0.1:${FRONTEND_PORT}/" frontend
}

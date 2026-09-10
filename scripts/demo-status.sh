#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/_common.sh
source "$SCRIPT_DIR/_common.sh"

status_pid() {
  local name="$1" file="$2" marker="$3"
  if [[ -f "$file" ]] && process_owned "$(cat "$file")" "$marker"; then
    printf '%-9s running (PID %s)\n' "$name" "$(cat "$file")"
  else
    printf '%-9s stopped\n' "$name"
  fi
}

status_pid backend "$RUNTIME_DIR/backend.pid" "$PROJECT_DIR"
status_pid frontend "$RUNTIME_DIR/frontend.pid" "$FRONTEND_DIR"
if docker info >/dev/null 2>&1 && docker container inspect "$MYSQL_CONTAINER" >/dev/null 2>&1; then
  printf '%-9s %s\n' mysql "$(docker container inspect --format '{{.State.Status}}/{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$MYSQL_CONTAINER")"
else
  printf '%-9s unavailable\n' mysql
fi


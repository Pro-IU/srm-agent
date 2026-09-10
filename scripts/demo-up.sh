#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/_common.sh
source "$SCRIPT_DIR/_common.sh"

require_command curl
require_command java
require_command lsof
require_command node
assert_docker_ready
ensure_runtime_env
load_runtime_env
assert_container_owned
compose config --quiet

log "starting project-owned MySQL"
compose up -d mysql
wait_for_mysql
apply_srm_sql
mysql_app "$MYSQL_DATABASE" -Nse "SELECT COUNT(*) FROM srm_supplier" | grep -qx '3' || die "synthetic supplier fixture validation failed"

start_backend
start_frontend

log "ready"
log "frontend: http://127.0.0.1:${FRONTEND_PORT}/"
log "backend:  http://127.0.0.1:${BACKEND_PORT}/"
log "mysql:    127.0.0.1:${MYSQL_PORT}/${MYSQL_DATABASE}"


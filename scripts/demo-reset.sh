#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/_common.sh
source "$SCRIPT_DIR/_common.sh"

assert_docker_ready
load_runtime_env
assert_container_owned
compose up -d mysql
wait_for_mysql
stop_recorded_process backend "$RUNTIME_DIR/backend.pid" "$PROJECT_DIR"

log "resetting only the fixed synthetic database: $MYSQL_DATABASE"
mysql_root -e "DROP DATABASE IF EXISTS srm_agent_demo; CREATE DATABASE srm_agent_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; GRANT SELECT,INSERT,UPDATE,DELETE ON srm_agent_demo.* TO 'srm_agent_demo_app'@'%'; FLUSH PRIVILEGES;"
apply_srm_sql
mysql_app "$MYSQL_DATABASE" -Nse "SELECT status FROM srm_purchase_order WHERE purchase_order_id='po_demo_approve'" | grep -qx 'PENDING_APPROVAL' || die "reset validation failed"
start_backend
log "synthetic database and in-process backend state reset"

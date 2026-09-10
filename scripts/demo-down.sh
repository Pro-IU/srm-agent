#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/_common.sh
source "$SCRIPT_DIR/_common.sh"

purge=false
[[ "${1:-}" == "--purge-volume" ]] && purge=true
[[ $# -le 1 ]] || die "usage: scripts/demo-down.sh [--purge-volume]"

ensure_runtime_dir
stop_recorded_process frontend "$RUNTIME_DIR/frontend.pid" "$FRONTEND_DIR"
stop_recorded_process backend "$RUNTIME_DIR/backend.pid" "$PROJECT_DIR"

if [[ -f "$RUNTIME_ENV" ]]; then
  assert_docker_ready
  load_runtime_env
  assert_container_owned
  if [[ "$purge" == "true" ]]; then
    log "removing project MySQL container, network and volume"
    compose down --volumes --remove-orphans
    rm -f "$RUNTIME_ENV"
  else
    log "stopping project MySQL; volume and generated credentials are retained"
    compose stop mysql
  fi
fi
log "stopped"


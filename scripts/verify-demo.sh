#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/_common.sh
source "$SCRIPT_DIR/_common.sh"

require_command curl
require_command node
load_runtime_env
base="http://127.0.0.1:${FRONTEND_PORT}/api/srm/agent"

json_assert() {
  local json="$1" expression="$2" label="$3"
  if ! printf '%s' "$json" | node -e "const fs=require('fs'); const x=JSON.parse(fs.readFileSync(0,'utf8')); if (!($expression)) process.exit(1);"; then
    die "$label"
  fi
}

post_chat() {
  curl -fsS --max-time 15 -H 'Content-Type: application/json' --data "$1" "$base/chat"
}

log "checking deterministic data route and quotation rules"
quote="$(post_chat '{"message":"比较 RFQ 的有效报价","intent":"QUOTE_ANALYSIS","parameters":{"rfqId":"rfq_demo_002"},"requestedBy":"verify-demo"}')"
json_assert "$quote" "x.mode==='DEMO' && x.route==='QUOTE_ANALYSIS' && x.status==='SUCCESS' && x.requiredConfirmations.length===0 && x.toolCalls.some(t=>t.toolName==='compare_srm_rfq_quotations')" "quotation route verification failed"
trace_id="$(printf '%s' "$quote" | node -e "const fs=require('fs');process.stdout.write(JSON.parse(fs.readFileSync(0,'utf8')).traceId)")"
trace="$(curl -fsS --max-time 10 "$base/traces/$trace_id")"
json_assert "$trace" "x.traceId && x.status==='SUCCESS' && !JSON.stringify(x).includes('confirmationToken')" "trace verification failed"

log "checking knowledge-disabled degradation"
knowledge="$(post_chat '{"message":"供应商准入制度需要哪些资料？","requestedBy":"verify-demo"}')"
json_assert "$knowledge" "x.route==='KNOWLEDGE_ONLY' && x.status==='DEGRADED' && x.knowledgeBasis && x.knowledgeBasis.status==='UNAVAILABLE'" "knowledge degradation verification failed"

log "checking preview-only and explicit-confirmation boundary"
before_status="$(mysql_app "$MYSQL_DATABASE" -Nse "SELECT status FROM srm_purchase_order WHERE purchase_order_id='po_demo_approve'")"
[[ "$before_status" == "PENDING_APPROVAL" ]] || die "demo approval order is not at baseline"
audit_before="$(curl -fsS --max-time 10 "$base/audit")"
preview="$(post_chat '{"message":"生成采购订单审批预览","intent":"PURCHASE_ORDER","parameters":{"purchaseOrderId":"po_demo_approve","previewApproval":"true"},"requestedBy":"verify-demo"}')"
json_assert "$preview" "x.route==='PURCHASE_ORDER' && x.requiredConfirmations.length===1 && x.requiredConfirmations[0].actionType==='APPROVE_PURCHASE_ORDER'" "preview verification failed"
after_preview_status="$(mysql_app "$MYSQL_DATABASE" -Nse "SELECT status FROM srm_purchase_order WHERE purchase_order_id='po_demo_approve'")"
[[ "$after_preview_status" == "PENDING_APPROVAL" ]] || die "preview unexpectedly changed the database"

chat_confirm="$(post_chat '{"message":"确认","parameters":{"purchaseOrderId":"po_demo_approve"},"requestedBy":"verify-demo"}')"
json_assert "$chat_confirm" "x.route!=='CONFIRMATION' && x.requiredConfirmations.length===0" "chat text unexpectedly entered confirmation route"
after_chat_status="$(mysql_app "$MYSQL_DATABASE" -Nse "SELECT status FROM srm_purchase_order WHERE purchase_order_id='po_demo_approve'")"
[[ "$after_chat_status" == "PENDING_APPROVAL" ]] || die "chat confirmation text changed the database"

bad_body='{"actionType":"APPROVE_PURCHASE_ORDER","targetId":"po_demo_approve","confirmationToken":"invalid-demo-token","confirmedBy":"verify-demo"}'
bad_code="$(curl -sS --max-time 10 -o "$RUNTIME_DIR/verify-bad-confirm.json" -w '%{http_code}' -H 'Content-Type: application/json' --data "$bad_body" "$base/confirm")"
[[ "$bad_code" == "400" ]] || die "invalid confirmation token was not rejected"
audit_after="$(curl -fsS --max-time 10 "$base/audit")"
[[ "$audit_before" == "$audit_after" ]] || die "verification unexpectedly created an audit record"
rm -f "$RUNTIME_DIR/verify-bad-confirm.json"

if [[ "${SRM_VERIFY_WRITE_CONFIRM:-false}" == "true" ]]; then
  log "explicit write verification enabled; confirming once and restoring baseline"
  confirm_json="$(printf '%s' "$preview" | node -e "const fs=require('fs');const x=JSON.parse(fs.readFileSync(0,'utf8'));const c=x.requiredConfirmations[0];process.stdout.write(JSON.stringify({...c,confirmedBy:'verify-demo'}))")"
  result="$(printf '%s' "$confirm_json" | curl -fsS --max-time 10 -H 'Content-Type: application/json' --data-binary @- "$base/confirm")"
  json_assert "$result" "x.executed===true" "explicit confirmation did not execute"
  "$SCRIPT_DIR/demo-reset.sh"
fi

log "PASS: data, quote, trace, knowledge degradation and confirmation safety contracts"


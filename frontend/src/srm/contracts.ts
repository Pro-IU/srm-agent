import type { SrmConfirmation, SrmQuotationComparison } from './types';

/** Pure UI guards: no token is persisted; confirmation can be enabled only from a backend-returned preview. */
export function canSubmitConfirmation(pending: SrmConfirmation | null, confirmedBy: string, acknowledged: boolean): boolean {
  return Boolean(pending?.actionType && pending.targetId && pending.confirmationToken && confirmedBy.trim() && acknowledged);
}

/** Step 5 deliberately renders structured tool JSON inside answer text. Extract only the known quote-comparison line. */
export function extractQuotationComparison(answer: string): SrmQuotationComparison | null {
  const marker = 'compare_srm_rfq_quotations：';
  const line = answer.split('\n').find((item) => item.includes(marker));
  if (!line) return null;
  try {
    const payload = JSON.parse(line.slice(line.indexOf(marker) + marker.length)) as { data?: SrmQuotationComparison; found?: boolean };
    return payload.found && payload.data?.rankedQuotes ? payload.data : null;
  } catch { return null; }
}

/** Audit has a stable JSON envelope but can be empty; preserve only array-like rows for display. */
export function formatAuditRows(value: unknown): unknown[] {
  if (Array.isArray(value)) return value;
  if (value && typeof value === 'object') {
    const data = (value as { data?: unknown }).data;
    return Array.isArray(data) ? data : [];
  }
  return [];
}

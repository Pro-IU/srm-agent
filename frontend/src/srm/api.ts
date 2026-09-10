import type { SrmActionResult, SrmAgentChatResponse, SrmChatRequest, SrmConfirmRequest, SrmRuntimeTrace } from './types';

const baseUrl = (import.meta.env.VITE_SRM_API_BASE ?? '').replace(/\/$/, '');
const timeoutMs = Number(import.meta.env.VITE_SRM_API_TIMEOUT_MS ?? 15000);
export type SrmApiError = Error & { status?: number; errorCode?: string };

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), Number.isFinite(timeoutMs) ? timeoutMs : 15000);
  try {
    const response = await fetch(`${baseUrl}${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...options.headers }, signal: controller.signal });
    const text = await response.text();
    const body: unknown = text ? JSON.parse(text) : null;
    if (!response.ok) {
      const data = body as { message?: string; errorCode?: string } | null;
      const error = new Error(data?.message || `请求失败 (${response.status})`) as SrmApiError;
      error.status = response.status; error.errorCode = data?.errorCode;
      throw error;
    }
    return body as T;
  } catch (error) {
    if (error instanceof SyntaxError) throw new Error('后端返回了不可识别的数据。', { cause: error });
    if ((error as DOMException)?.name === 'AbortError') throw new Error('请求超时，请检查本地后端运行状态。', { cause: error });
    throw error;
  } finally { window.clearTimeout(timer); }
}

export function sendSrmChat(payload: SrmChatRequest) { return request<SrmAgentChatResponse>('/api/srm/agent/chat', { method: 'POST', body: JSON.stringify(payload) }); }
export function confirmSrmAction(payload: SrmConfirmRequest) { return request<SrmActionResult>('/api/srm/agent/confirm', { method: 'POST', body: JSON.stringify(payload) }); }
export function fetchTrace(traceId: string) { return request<SrmRuntimeTrace>(`/api/srm/agent/traces/${encodeURIComponent(traceId)}`); }
export function fetchRecentTraces() { return request<SrmRuntimeTrace[]>('/api/srm/agent/traces'); }
export function fetchAudit() { return request<unknown>('/api/srm/agent/audit'); }

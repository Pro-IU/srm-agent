import { useEffect, useMemo, useState } from 'react';
import { confirmSrmAction, fetchAudit, fetchRecentTraces, fetchTrace, sendSrmChat, type SrmApiError } from './srm/api';
import { canSubmitConfirmation, extractQuotationComparison, formatAuditRows } from './srm/contracts';
import type { SrmAgentChatResponse, SrmConfirmation, SrmRuntimeTrace } from './srm/types';
import './App.css';

type Scenario = { label: string; message: string; intent?: string; parameters: Record<string, string>; note: string };

const SCENARIOS: Scenario[] = [
  { label: '供应商风险', message: '查询供应商档案、准入状态和风险', parameters: { supplierId: 'sup_demo_risk' }, note: '暂停供应商的合成风险场景' },
  { label: 'RFQ 进度', message: '查看 RFQ 寻源进度和参与报价', intent: 'SOURCING', parameters: { rfqId: 'rfq_demo_002' }, note: '连接器试制批寻源' },
  { label: '报价比较', message: '比较 RFQ 的有效报价', intent: 'QUOTE_ANALYSIS', parameters: { rfqId: 'rfq_demo_002' }, note: '固定透明评分规则' },
  { label: '价格库', message: '查询物料价格库及临期状态', intent: 'PRICE_LIBRARY', parameters: { materialCode: 'MAT-DEMO-CONN-02' }, note: '临期价格记录' },
  { label: '订单履约', message: '查看采购订单履约状态和明细', intent: 'PURCHASE_ORDER', parameters: { purchaseOrderId: 'po_demo_001' }, note: '部分收货正常流程' },
  { label: '授标预览', message: '对 RFQ 发起授标候选复核预览', intent: 'AWARD_REVIEW', parameters: { rfqId: 'rfq_demo_003', quoteId: 'quote_demo_003_r' }, note: '风险场景：只预览，不授标' },
  { label: '订单审批预览', message: '生成采购订单审批预览', intent: 'PURCHASE_ORDER', parameters: { purchaseOrderId: 'po_demo_approve', previewApproval: 'true' }, note: '独立人工确认演示' },
  { label: '准入制度依据', message: '供应商准入制度需要哪些资料？', parameters: {}, note: '知识库制度检索（需登录会话）' },
  { label: '数据 + 制度核验', message: '查询供应商风险并说明准入制度依据', parameters: { supplierId: 'sup_demo_risk' }, note: '事实与制度并列人工核验' },
];

function errorText(error: unknown): string {
  const apiError = error as SrmApiError;
  if (apiError?.errorCode === 'LIVE_MODE_NOT_CONFIGURED') return 'LIVE 模式未配置模型凭据，已安全拒绝；请使用 DEMO 模式或完成后端配置。';
  return apiError?.message || '请求未完成。请检查本地后端、合成数据初始化和网络连接。';
}

export default function App() {
  const [message, setMessage] = useState(SCENARIOS[0].message);
  const [selected, setSelected] = useState<Scenario>(SCENARIOS[0]);
  const [response, setResponse] = useState<SrmAgentChatResponse | null>(null);
  const [traces, setTraces] = useState<SrmRuntimeTrace[]>([]);
  const [traceId, setTraceId] = useState('');
  const [audit, setAudit] = useState<unknown[]>([]);
  const [pending, setPending] = useState<SrmConfirmation | null>(null);
  const [confirmedBy, setConfirmedBy] = useState('');
  const [acknowledged, setAcknowledged] = useState(false);
  const [loading, setLoading] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const comparison = useMemo(() => response ? extractQuotationComparison(response.answer) : null, [response]);

  useEffect(() => { void refreshObservability(); }, []);

  async function refreshObservability() {
    try {
      const [recent, auditRows] = await Promise.all([fetchRecentTraces(), fetchAudit()]);
      setTraces(recent); setAudit(formatAuditRows(auditRows));
    } catch { /* Chat stays useful when optional local observability endpoints are unavailable. */ }
  }
  function chooseScenario(scenario: Scenario) { setSelected(scenario); setMessage(scenario.message); setError(''); }
  async function submitChat() {
    if (!message.trim() || loading) return;
    setLoading(true); setError(''); setNotice('');
    setPending(null); setConfirmedBy(''); setAcknowledged(false);
    try {
      const result = await sendSrmChat({ message: message.trim(), intent: selected.intent, parameters: selected.parameters, requestedBy: 'demo-buyer' });
      setResponse(result); setTraceId(result.traceId); setPending(result.requiredConfirmations[0] ?? null);
      await refreshObservability();
    } catch (requestError) { setError(errorText(requestError)); }
    finally { setLoading(false); }
  }
  async function lookupTrace() {
    if (!traceId.trim()) return;
    setError('');
    try { const trace = await fetchTrace(traceId.trim()); setTraces((current) => [trace, ...current.filter((item) => item.traceId !== trace.traceId)]); }
    catch (requestError) { setError(errorText(requestError)); }
  }
  async function submitConfirmation() {
    if (!pending || !canSubmitConfirmation(pending, confirmedBy, acknowledged) || confirming) return;
    setConfirming(true); setError(''); setNotice('');
    try { const result = await confirmSrmAction({ ...pending, confirmedBy: confirmedBy.trim() }); setNotice(result.message || '受控确认请求已提交，请以审计记录和后端结果为准。'); }
    catch (requestError) { setError(errorText(requestError)); }
    finally {
      setPending(null); setConfirmedBy(''); setAcknowledged(false); setConfirming(false);
      await refreshObservability();
    }
  }

  return <main className="srm-shell">
    <aside className="scenario-rail">
      <div className="brand"><span className="brand-mark">▣</span><div><strong>SRM 智能采购</strong><small>协同与决策工作台</small></div></div>
      <div className="synthetic-badge">仅含合成演示数据</div><div className="rail-heading">业务场景</div>
      <nav>{SCENARIOS.map((scenario) => <button key={scenario.label} className={`scenario ${selected.label === scenario.label ? 'active' : ''}`} onClick={() => chooseScenario(scenario)}><span>{scenario.label}</span><small>{scenario.note}</small></button>)}</nav>
      <div className="rail-foot">个人作品集演示<br />非生产系统 · 不含原公司数据</div>
    </aside>
    <section className="workspace">
      <header className="topbar"><div><span className="eyebrow">STRUCTURED PROCUREMENT OPERATIONS</span><h1>SRM 智能采购工作台</h1></div><span className={`mode ${response?.mode === 'LIVE' ? 'live' : ''}`}>{response?.mode || 'DEMO'} 模式</span></header>
      <section className="dialog-panel">
        <div className="panel-title"><div><h2>采购协同对话</h2><p>Agent 只基于结构化工具结果回答；不会自动写入或确认。</p></div>{loading && <span className="loading">正在调用受控工具…</span>}</div>
        <div className="composer"><textarea value={message} onChange={(event) => setMessage(event.target.value)} maxLength={2000} aria-label="SRM Agent 问题" placeholder="例如：比较 RFQ 的有效报价" /><button onClick={() => void submitChat()} disabled={loading || !message.trim()}>{loading ? '处理中' : '发送问题'}</button></div>
        {error && <div className="error-box">{error}</div>}{notice && <div className="notice-box">{notice}</div>}
        {!response && !loading && <div className="empty-state">选择左侧合成业务场景，或输入一个带有有效演示 ID 的 SRM 问题。</div>}
        {response && <section className="answer-card"><div className="answer-meta"><span>路由：<b>{response.route}</b></span><span>Trace：<code>{response.traceId}</code></span></div><pre className="answer-text">{response.answer}</pre><div className="chips">{response.participants.map((agent) => <span key={agent}>{agent}</span>)}</div><div className="tool-list">{response.toolCalls.map((tool, index) => <div key={`${tool.toolName}-${index}`}><b>{tool.agent}</b><span>{tool.toolName || '无工具调用'}</span><em className={tool.status === 'SUCCESS' ? 'ok' : ''}>{tool.status}</em></div>)}</div></section>}
      </section>
      {comparison && <QuotationComparison comparison={comparison} />}
      {response?.knowledgeBasis && <KnowledgeBasis basis={response.knowledgeBasis} dataFacts={response.dataFacts ?? []} />}
      <section className="audit-panel"><div className="panel-title"><div><h2>受控操作审计</h2><p>仅显示当前进程内的合成演示审计记录。</p></div><button className="ghost-button" onClick={() => void refreshObservability()}>刷新</button></div>{audit.length === 0 ? <div className="empty-state compact">暂无审计记录。预览本身不会产生写操作审计。</div> : <div className="audit-list">{audit.map((item, index) => <pre key={index}>{JSON.stringify(item, null, 2)}</pre>)}</div>}</section>
    </section>
    <aside className="execution-rail">
      <section className="side-card"><h2>执行轨迹</h2><div className="trace-search"><input value={traceId} onChange={(event) => setTraceId(event.target.value)} placeholder="输入 traceId" /><button onClick={() => void lookupTrace()}>查看</button></div>{traces.length === 0 ? <div className="empty-state compact">尚无轨迹。发送一条问题后可查看路由、Agent 与工具状态。</div> : <div className="trace-list">{traces.map((trace) => <TraceCard key={trace.traceId} trace={trace} />)}</div>}</section>
      <section className="side-card confirmation"><h2>高风险人工确认</h2><p>聊天里的“确认”无效。只有后端预览返回的完整待确认对象才能在此操作。</p>{!pending ? <div className="empty-state compact">没有待确认预览。授标或订单审批场景只会先生成预览。</div> : <><dl><dt>操作</dt><dd>{pending.actionType}</dd><dt>目标</dt><dd>{pending.targetId}</dd><dt>过期时间</dt><dd>{pending.expiresAt || '以后端预览为准'}</dd></dl><ul className="risk-list">{pending.riskWarnings.map((risk) => <li key={risk}>{risk}</li>)}</ul><label>确认人<input value={confirmedBy} onChange={(event) => setConfirmedBy(event.target.value)} placeholder="例如 demo-approver" /></label><label className="check"><input type="checkbox" checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked)} />我已核对目标、风险和预览内容，并请求执行此单次操作。</label><button className="confirm-button" disabled={!canSubmitConfirmation(pending, confirmedBy, acknowledged) || confirming} onClick={() => void submitConfirmation()}>{confirming ? '正在受控确认' : '提交显式人工确认'}</button></>}</section>
    </aside>
  </main>;
}

function QuotationComparison({ comparison }: { comparison: NonNullable<ReturnType<typeof extractQuotationComparison>> }) {
  return <section className="comparison-panel"><div className="panel-title"><div><h2>有效报价横向比较</h2><p>固定透明规则，不是模型自主决策：价格 45% · 交期 25% · 账期 15% · 风险 15%</p></div><span className="rule-badge">透明固定评分</span></div>{comparison.rankedQuotes.length === 0 ? <div className="empty-state compact">当前 RFQ 没有可比较的有效报价；请查看工具结果中的排除原因。</div> : <div className="comparison-table"><table><thead><tr><th>排名</th><th>供应商</th><th>单价</th><th>交期</th><th>账期</th><th>风险</th><th>加权名次</th></tr></thead><tbody>{comparison.rankedQuotes.map((quote) => <tr key={quote.quoteId}><td>#{quote.rank}</td><td>{quote.supplierName}<small>{quote.quoteId}</small></td><td>{quote.unitPrice}</td><td>{quote.leadTimeDays} 天</td><td>{quote.paymentTermsDays} 天</td><td><span className={`risk ${quote.riskLevel?.toLowerCase()}`}>{quote.riskLevel}</span></td><td>{quote.weightedRank}</td></tr>)}</tbody></table></div>}</section>;
}
function TraceCard({ trace }: { trace: SrmRuntimeTrace }) {
  return <article className="trace-card"><div><b>{trace.route}</b><span className={`trace-status ${trace.status === 'SUCCESS' || trace.status === 'COMPLETED' ? 'ok' : ''}`}>{trace.status}</span></div><small>{trace.durationMs} ms · {trace.traceId}</small><div className="trace-agents">{trace.participants.length ? trace.participants.join(' → ') : '安全拒绝或无委派'}</div>{trace.toolCalls.map((call, index) => <div className="trace-tool" key={`${call.toolName}-${index}`}>{call.agent}: {call.toolName || '无工具'} <em>{call.status}</em></div>)}</article>;
}
function KnowledgeBasis({ basis, dataFacts }: { basis: NonNullable<SrmAgentChatResponse['knowledgeBasis']>; dataFacts: string[] }) {
  return <section className="knowledge-panel"><div className="panel-title"><div><h2>制度知识依据</h2><p>与结构化业务事实分开展示。文档内容可能过期，冲突时必须人工核验。</p></div><span className={`knowledge-status ${basis.status === 'AVAILABLE' ? 'available' : ''}`}>{basis.status}</span></div>
    {dataFacts.length > 0 && <div className="data-facts"><b>业务数据事实</b>{dataFacts.map((fact) => <span key={fact}>{fact}</span>)}</div>}
    {basis.status === 'UNAVAILABLE' ? <div className="empty-state compact">知识库不可用：{basis.notice}</div> : <><p className="knowledge-answer">{basis.answer || basis.notice}</p><div className="citation-list">{basis.citations.length === 0 ? <div className="empty-state compact">{basis.notice || '知识库未提供可核验引用。'}</div> : basis.citations.map((citation, index) => <article key={`${citation.documentId ?? 'citation'}-${index}`}><b>{citation.title || 'knowledge-engine 未返回文档标题'}</b><span>{citation.source ? `检索来源：${citation.source}` : '未提供检索来源'}</span>{citation.snippet && <p>{citation.snippet}</p>}{citation.url && <a href={citation.url} target="_blank" rel="noreferrer">查看 knowledge-engine 返回的来源</a>}</article>)}</div></>}</section>;
}

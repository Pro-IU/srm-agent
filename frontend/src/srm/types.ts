export interface SrmToolCallSummary { agent: string; toolName: string | null; status: string; }
export interface SrmConfirmation {
  actionType: string;
  targetId: string;
  confirmationToken: string;
  expiresAt?: string;
  riskWarnings: string[];
}
export interface SrmAgentChatResponse {
  answer: string;
  mode: 'DEMO' | 'LIVE' | string;
  route: string;
  participants: string[];
  toolCalls: SrmToolCallSummary[];
  requiredConfirmations: SrmConfirmation[];
  traceId: string;
  status: string;
  dataFacts?: string[];
  knowledgeBasis?: SrmKnowledgeBasis | null;
}
export interface SrmRuntimeTrace {
  traceId: string;
  startedAt: string;
  finishedAt: string;
  mode: string;
  route: string;
  participants: string[];
  toolCalls: SrmToolCallSummary[];
  status: string;
  durationMs: number;
}
export interface SrmQuotation { rank: number; quoteId: string; supplierId: string; supplierName: string; unitPrice: number | string; leadTimeDays: number; paymentTermsDays: number; riskLevel: string; weightedRank: number | string; }
export interface SrmQuotationComparison { rfqId: string; eligibleQuoteCount: number; excludedQuoteCount: number; rules: string[]; rankedQuotes: SrmQuotation[]; }
export interface SrmKnowledgeCitation { documentId?: string; title?: string; source?: string; url?: string; snippet?: string; }
export interface SrmKnowledgeBasis { status: string; answer: string; citations: SrmKnowledgeCitation[]; notice: string; }
export interface SrmChatRequest { message: string; conversationId?: string; intent?: string; parameters?: Record<string, string>; requestedBy?: string; }
export interface SrmConfirmRequest extends SrmConfirmation { confirmedBy: string; }
export interface SrmActionResult { executed?: boolean; actionType?: string; targetId?: string; auditId?: string; message?: string; }

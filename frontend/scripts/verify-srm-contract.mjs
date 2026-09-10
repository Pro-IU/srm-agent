import { readFileSync } from 'node:fs';

const sources = ['src/srm/api.ts', 'src/srm/contracts.ts', 'src/srm/types.ts', 'src/App.tsx']
  .map((path) => ({ path, text: readFileSync(new URL(`../${path}`, import.meta.url), 'utf8') }));
const required = [
  ['src/srm/api.ts', '/api/srm/agent/chat'], ['src/srm/api.ts', '/api/srm/agent/confirm'],
  ['src/srm/api.ts', '/api/srm/agent/traces'], ['src/srm/api.ts', '/api/srm/agent/audit'],
  ['src/srm/contracts.ts', 'canSubmitConfirmation'], ['src/srm/contracts.ts', 'confirmationToken'],
  ['src/App.tsx', '价格 45% · 交期 25% · 账期 15% · 风险 15%'], ['src/App.tsx', 'setPending(null)'],
  ['src/App.tsx', '制度知识依据'], ['src/App.tsx', 'knowledgeBasis'], ['src/srm/types.ts', 'SrmKnowledgeCitation'],
];
for (const [path, needle] of required) {
  const source = sources.find((item) => item.path === path);
  if (!source?.text.includes(needle)) throw new Error(`SRM contract check failed: ${path} missing ${needle}`);
}
if (sources.some((source) => /localStorage|sessionStorage/.test(source.text))) throw new Error('SRM contract check failed: browser storage is prohibited');
console.log('SRM frontend API/security contract: OK');

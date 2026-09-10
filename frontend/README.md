# SRM 智能采购工作台前端

这是 `srm-agent` 面向制造业采购协同的本地演示工作台。所有供应商、RFQ、报价、价格库与订单示例均来自仓库中的合成 SQL 数据，不代表任何原公司或生产系统。

## 页面与 API

- 左侧为有效合成 ID 的业务场景：供应商风险、RFQ、报价比较、价格库、订单履约、授标预览和订单审批预览。
- 中间为 `POST /api/srm/agent/chat` 对话及路由、参与 Agent、工具摘要。报价工具 JSON 会以表格呈现，并固定标注价格 45%、交期 25%、账期 15%、风险 15% 的透明规则；这不是模型决策。
- 右侧为 `GET /api/srm/agent/traces` / `GET /api/srm/agent/traces/{traceId}` 的轨迹，以及仅在后端返回待确认预览后才启用的确认面板。
- 审计区读取 `GET /api/srm/agent/audit`；确认提交走独立 `POST /api/srm/agent/confirm`。

复制 `.env.example` 为本地环境配置。本地 `npm run dev` 时应保持 `VITE_SRM_API_BASE` 为空：所有 `/api` 请求会由 Vite 的同源代理安全转发到 `VITE_SRM_API_PROXY_TARGET`（默认 `http://127.0.0.1:18080`），因此 chat、trace、audit 和 confirm 都不需要浏览器跨域访问。部署环境只有在自身提供同源网关时才设置 `VITE_SRM_API_BASE`。不要填写、保存或记录 API key。`confirmationToken` 只存在于当前 React 组件内存：没有后端预览、没有填写确认人、没有勾选确认时无法调用 confirm；成功或失败后都会清除。

```bash
npm run build
npm run lint
node scripts/verify-srm-contract.mjs
```

本步骤没有新增前端测试框架或下载依赖。`verify-srm-contract.mjs` 是无依赖静态契约核验，检查 API 路径、确认前置条件、报价规则文字与禁止浏览器持久化。若 `node_modules` 缺失，只能在已有本地 npm 缓存时使用 `npm ci --offline`；本仓库不复制依赖目录或联网下载。

## 知识依据展示

当 Step 5 chat 响应包含 `knowledgeBasis` 时，工作台显示 `KNOWLEDGE_ONLY` 或 `HYBRID` 路由、独立的“业务数据事实”与“制度知识依据”区、知识状态和 knowledge-engine 实际返回的文档标题/检索来源/片段/URL。`UNAVAILABLE`、无结果或无引用不会伪造来源，而会提示本地知识服务或登录会话不可用。该展示不接收或保存知识库 Authorization/token，原有确认 token 内存边界不变。

## 完整项目启动

前端依赖同仓库 `io.github.oudexin.srm.agent` 后端提供的 SRM API。推荐从项目根目录运行 `./scripts/demo-up.sh`，由脚本检查后端、数据库和前端并输出访问地址；使用 `./scripts/demo-down.sh` 安全停止项目资源。详细步骤见根目录 README。

# 架构与安全设计

## 组件与最小权限

| 组件 | 职责 | 可用工具 |
|---|---|---|
| ProcurementMasterAgent | 路由、有限委派、汇总 | 独立受控确认网关、审计查询 |
| SupplierAgent | 供应商档案、状态、风险 | `query_srm_supplier` |
| SourcingAgent | RFQ、寻源进度、价格库 | `query_srm_rfq`、`query_srm_price_library` |
| QuoteAnalysisAgent | 固定规则报价比较 | `compare_srm_rfq_quotations` |
| AwardReviewAgent | 授标风险复核 | `query_srm_rfq`、`preview_srm_award` |
| PurchaseOrderAgent | 订单履约与审批预览 | `query_srm_purchase_order`、`preview_srm_purchase_order_approval` |
| KnowledgeSupportAgent | 制度、流程、FAQ、错误码 | `query_srm_knowledge`（只读） |

业务子 Agent 都没有确认权限。Master 也只有在独立确认 API 收到四个显式字段后才能进入确认网关。

## 调用链

```text
用户问题 → Chat API → 路由分类
  DATA_ONLY      → Master → 业务 Agent → 结构化工具 → MySQL
  KNOWLEDGE_ONLY → KnowledgeSupportAgent → knowledge-engine
  HYBRID         → 先结构化工具，再知识检索 → 分区汇总
```

跨域任务最多委派 3 个角色，最大深度为 2。未知意图、参数越权、子 Agent 异常和知识服务超时都会返回结构化降级结果，不会扩大工具权限。

## 高风险写操作

```text
preview(action, target)
  → 校验当前业务状态
  → 生成绑定 action/target/requester/expiry 的一次性随机令牌
  → 返回变更摘要和风险，不写数据库

confirm(action, target, token, confirmedBy)
  → 校验完整字段、绑定关系、时效和未使用状态
  → 在事务内重新读取并校验领域状态
  → 执行一次写入并消费令牌
  → 生成审计
```

支持 RFQ 授标、价格记录启用/停用和采购订单审批。确认入口不接受自然语言“确认”作为授权。

## 数据模型

- `srm_supplier` 被报价、价格记录和采购订单引用。
- `srm_rfq_project` 管理物料、需求量、预算、截止期和定点结果。
- `srm_supplier_quote` 对 RFQ + 供应商 + 版本唯一。
- `srm_price_library_record` 记录有效期、MOQ、来源报价和启停状态。
- `srm_purchase_order` / `srm_purchase_order_line` 管理订单头、行和收货数量约束。

SQL 包含主键、业务唯一键、索引、外键和 MySQL 8 CHECK 约束。所有演示记录使用 `demo`/`SYN` 编码和 `.test` 邮箱。

## 模型、知识与本地交付边界

LIVE 模型只输出受 schema 和白名单限制的意图与参数；最终事实仍来自工具。知识片段是不可信证据：限制长度、净化控制标签、保留真实引用，但不能成为系统指令、选择工具或触发确认。

Compose 只管理项目专用 MySQL。后端和前端由脚本使用本机 JDK/Node 启动，避免提供未经验证的应用镜像。随机运行凭据保存在被忽略的 `.runtime/demo.env`，防止已有 volume 与新密码失配。


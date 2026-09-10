# SRM 采购协同与决策智能体

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)

`srm-agent` 是面向制造企业采购场景的个人作品集项目。它将供应商、寻源/RFQ、报价、价格库和采购订单等结构化数据，与可选的 SRM 制度知识检索组合成可解释、可审计的多 Agent 工作流。

配套项目：[knowledge-engine](https://github.com/Pro-IU/knowledge-engine) 提供带角色权限过滤、版本管理与引用回溯的 SRM 知识服务。

> 所有名称、编号、金额、联系人和业务记录均为合成数据。本项目不是原公司项目，也未声称在生产环境上线。

## 亮点

- 六个业务 Agent：采购总控、供应商、寻源、报价分析、授标复核、采购订单；另有只读知识支持 Agent。
- `DATA_ONLY`、`KNOWLEDGE_ONLY`、`HYBRID` 三类确定性路由；跨域委派有深度和次数上限。
- 报价评分规则透明：价格 45%、交期 25%、账期 15%、风险 15%，不是 LLM 自主决策。
- 授标、价格启停、订单审批采用“预览 → 人工显式确认 → 执行”两阶段机制。
- 一次性 256 位随机确认令牌绑定动作、目标和有效期；执行前重新校验状态，结果进入审计。
- 默认 `DEMO` 模式完全不需要模型 key；`LIVE` 模式只允许模型解析白名单意图和参数。
- 可选只读对接 `knowledge-engine`，引用来源与结构化业务事实分开展示。

## 架构

```text
React 工作台
  └─ /api/srm/agent
      ├─ DEMO 确定性解析 / LIVE 白名单解析
      ├─ ProcurementMasterAgent
      │   ├─ SupplierAgent / SourcingAgent / QuoteAnalysisAgent
      │   └─ AwardReviewAgent / PurchaseOrderAgent
      ├─ SrmStructuredDataTools → MyBatis-Plus → MySQL
      ├─ 独立 confirm 网关 → 事务校验 → 进程内审计
      └─ KnowledgeSupportAgent → knowledge-engine（可选、只读）
```

详细设计见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 目录

```text
src/main/java/io/github/oudexin/srm/agent/  后端、Agent、工具与安全机制
src/main/resources/db/srm/                 MySQL 8 表结构、合成数据、迁移
frontend/                                  React SRM 智能采购工作台
scripts/                                   启停、重置、状态和验收脚本
docs/                                      架构、演示、简历和测试报告
compose.yaml                               项目专用 MySQL（不自动拉镜像）
```

## 本地快速启动

前置条件：JDK 21、Maven 3.9+、Node.js 22/npm、Docker Compose v2，以及本地已有 `mysql:8.0.44` 镜像。脚本可识别 PATH、`MVN_BIN` 或 macOS IntelliJ IDEA 自带的 Maven；不会自动联网或拉取镜像。

```bash
./scripts/demo-up.sh
./scripts/demo-status.sh
./scripts/verify-demo.sh
```

默认地址：

- 工作台：<http://127.0.0.1:15173/>
- 后端：<http://127.0.0.1:18080/>
- MySQL：`127.0.0.1:13306/srm_agent_demo`

脚本默认以 Maven 离线模式构建。若本机缓存不完整，请先自行准备依赖；只有在你明确允许 Maven 联网时，才可执行：

```bash
SRM_MAVEN_OFFLINE=false ./scripts/demo-up.sh
```

前端 `node_modules` 缺失时脚本会停止，不会静默安装。明确允许 `npm ci` 时使用 `SRM_ALLOW_NPM_INSTALL=true`。

### 重置和停止

```bash
./scripts/demo-reset.sh       # 仅重建固定的 srm_agent_demo 合成库并重启后端
./scripts/demo-down.sh        # 停止本项目进程/容器，保留 volume 与生成凭据
./scripts/demo-down.sh --purge-volume  # 显式删除项目专用 volume 与运行凭据
```

运行时 PID、日志和随机凭据只保存在权限为 700/600 的 `.runtime/`，该目录已被 Git 忽略。停止脚本会校验 PID 命令行和 Compose 项目标签，不会按端口盲目杀进程。

## API

```http
POST /api/srm/agent/chat
POST /api/srm/agent/confirm
GET  /api/srm/agent/traces/{traceId}
GET  /api/srm/agent/traces
GET  /api/srm/agent/audit
```

聊天示例：

```json
{
  "message": "比较 RFQ 的有效报价",
  "intent": "QUOTE_ANALYSIS",
  "parameters": {"rfqId": "rfq_demo_002"},
  "requestedBy": "demo-buyer"
}
```

高风险确认不能通过聊天文本完成。必须先取得后端返回的 `requiredConfirmations`，再由人工通过独立确认接口提交完整的 `actionType`、`targetId`、`confirmationToken` 和 `confirmedBy`。

## 运行模式

- `SRM_RUNTIME_MODE=DEMO`：默认；确定性路由和真实数据库工具，不调用模型。
- `SRM_RUNTIME_MODE=LIVE`：当前仅支持 DashScope；必须显式提供 provider、model 和 API key，base URL 可选。模型无权选择任意 Bean/方法，也无权确认写操作。
- `SRM_KNOWLEDGE_ENGINE_ENABLED=false`：默认关闭。开启后调用独立服务的受保护 SSE 会话和引用接口；调用方负责合法登录上下文。

环境变量示例见 [.env.example](.env.example)，不得提交真实凭据。

## 演示与求职材料

- [8–12 分钟面试演示手册](docs/INTERVIEW_DEMO.md)
- [可用于简历的项目模板](docs/RESUME_PROJECT.md)
- [测试与验收报告](docs/TEST_REPORT.md)

## 非生产限制

- 待确认请求、Trace 和审计目前保存在单进程内，重启即丢失，不具备防篡改能力。
- 没有生产 IAM、审批权限、数据库锁策略、多节点一致性或完整可观测平台。
- `knowledge-engine` 的角色过滤是演示能力，不等同于企业级数据权限。
- LIVE 模式与真实知识库需要外部基础设施，本地默认验收不启用。
- 制度知识可能过期；若与业务数据冲突，系统只并列展示并要求人工核验。

# 测试与验收报告

报告日期：2026-09-04

## 验收范围

- Java 21 主源码与测试源码编译
- React lint、TypeScript/Vite 构建和前端契约检查
- MySQL 8 表结构、三份 SQL 和合成基线
- DEMO 模式真实 API、Trace、报价规则、知识降级和人工确认
- 一键脚本、Compose、凭据隔离和停止边界

## 已有端到端证据

Step 9 已使用真实 Spring Boot + MySQL + React 页面完成：

- 供应商、RFQ、报价、价格库和订单查询成功。
- 报价表显示固定 45/25/15/15 规则和结构化 Trace。
- knowledge-engine 关闭时返回 `KNOWLEDGE_ONLY / DEGRADED / UNAVAILABLE`。
- 审批预览不写数据库；确认人和勾选缺一时按钮禁用。
- 显式确认后数据库变为 `APPROVED` 并生成审计，随后恢复 `PENDING_APPROVAL`。
- 390px 视口无横向溢出，桌面三栏使用完整可用宽度。

## Step 10 实际结果

- IntelliJ IDEA 内置 Maven 3.9.9、Oracle JDK 21.0.10。
- `mvn clean test`：成功；92 个主源码、12 个测试源码，30 项测试全部通过，0 失败、0 错误、0 跳过。
- `mvn -o ... -DskipTests clean package`：成功；生成可执行 Spring Boot JAR，证明依赖补齐后可离线打包。
- `npm run lint`、`npm run build`、前端静态契约检查：成功。
- `docker compose ... config --quiet`：成功；使用本地 `mysql:8.0.44`，未拉取镜像。
- `demo-up.sh`：真实启动项目专用 MySQL、Spring Boot JAR 和 Vite 工作台。
- `verify-demo.sh`：报价路由、固定评分规则、Trace 脱敏、知识降级、预览不写库、聊天文本不能确认、错误令牌拒绝全部通过。
- `demo-reset.sh`：仅重建 `srm_agent_demo`，订单恢复 `PENDING_APPROVAL`，并通过重启清空进程内状态。
- `SRM_VERIFY_WRITE_CONFIRM=true verify-demo.sh`：显式确认真实执行一次，随后自动重置，最终回到合成基线。
- `demo-status.sh`：验收结束时 backend、frontend、mysql 均为 running/healthy。

执行过的核心命令：

```bash
bash -n scripts/*.sh
docker compose --env-file .runtime/demo.env -f compose.yaml config --quiet
./scripts/demo-down.sh
./scripts/demo-up.sh
./scripts/verify-demo.sh
./scripts/demo-reset.sh
./scripts/verify-demo.sh
npm --prefix frontend run lint
npm --prefix frontend run build
node frontend/scripts/verify-srm-contract.mjs
mvn -o clean test-compile
mvn -o test
```

## 已知限制

- 首次联网仅用于补齐本机 Maven Surefire/JAR/Spring Boot 插件依赖；补齐后的打包已再次以离线模式成功验证。
- Mockito 在 JDK 21 下提示未来版本需显式配置 Java agent；当前不影响 30 项测试通过。
- `HttpKnowledgeEngineClientTest` 有既有 unchecked 编译提示，不影响测试结果。
- LIVE DashScope 与真实 knowledge-engine 未在本地离线验收。
- Trace、待确认和审计是单进程内状态，重启丢失。
- 当前演示不等同于生产 IAM、持久审计、多节点一致性或数据库锁验证。

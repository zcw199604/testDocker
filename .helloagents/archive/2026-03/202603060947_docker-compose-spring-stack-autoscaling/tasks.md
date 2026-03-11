# 任务清单: docker-compose-spring-stack-autoscaling

> **@status:** completed | 2026-03-06 10:59

```yaml
@feature: docker-compose-spring-stack-autoscaling
@created: 2026-03-06
@updated: 2026-03-06 10:57:29 UTC
@status: completed
@mode: R3
@package_type: implementation
@selected_plan: 方案C：三层分域 + Compose 到 Swarm 的渐进弹性方案
@review_status: Claude=条件可行
```

<!-- LIVE_STATUS_BEGIN -->
状态: completed | 进度: 17/17 (100%) | 更新: 2026-03-06 10:57:29 UTC
当前: 已完成 Compose 落地、阶段2骨架验证与方案包收尾；2.5 因未切换到实际 Swarm 运行环境而跳过
<!-- LIVE_STATUS_END -->

## 进度概览

| 完成 | 失败 | 跳过 | 总数 |
|------|------|------|------|
| 16 | 0 | 1 | 17 |

---

## 任务列表

### 1. 阶段1：Compose 直接落地（首批阻断项）

- [√] 1.1 规划 `deploy/compose/` 目录结构、`.env.example`、镜像标签、端口/密码变量与连接池预算字段，形成统一的部署配置入口 | depends_on: []
- [√] 1.2 编写 `deploy/compose/docker-compose.yml` 基础骨架，定义 `edge`、`app`、`data` 网络与 `postgres_data`、`redis_data`、`traefik_letsencrypt`、`backup_data` 命名卷 | depends_on: [1.1]
- [√] 1.3 落地 `deploy/compose/traefik/traefik.yml`、动态路由配置与回归用例，为 `ui-service` 和 `biz-service` 提供统一入口、TLS 终止与负载分发 | depends_on: [1.2]
- [√] 1.4 配置 PostgreSQL 18 服务、初始化脚本目录、持久化卷挂载、健康检查与 `backup/restore` 脚本骨架 | depends_on: [1.1, 1.2]
- [√] 1.5 配置 Redis 服务、`redis.conf`、密码策略、`appendonly yes` / `appendfsync everysec`、持久化目录与健康检查命令 | depends_on: [1.1, 1.2]
- [√] 1.6 为 `ui-service` 制定无状态验证清单：Session 外置、禁用本地状态、临时文件策略、连接池预算、readiness 依赖检查 | depends_on: [1.4, 1.5]
- [√] 1.7 为 `biz-service` 制定无状态验证清单：本地状态审计、共享锁策略、连接池预算、后台任务幂等与 readiness 依赖检查 | depends_on: [1.4, 1.5]
- [√] 1.8 为 `ui-service` 补齐 Dockerfile、运行时环境变量、Traefik 标签、数据库/缓存连接参数与 `/actuator/health/*` 健康检查配置 | depends_on: [1.3, 1.4, 1.5, 1.6]
- [√] 1.9 为 `biz-service` 补齐 Dockerfile、运行时环境变量、服务发现约定、数据库/缓存连接参数与 `/actuator/health/*` 健康检查配置 | depends_on: [1.3, 1.4, 1.5, 1.7]
- [√] 1.10 编写 PostgreSQL 18 / Redis 备份与恢复 SOP，明确执行频率、保留策略、恢复演练步骤、责任人与发布前快照要求 | depends_on: [1.4, 1.5]
- [√] 1.11 补齐启动顺序、`depends_on` 条件、重启策略、共享网络连通性与命名卷绑定校验，并完成 `docker compose up -d`、`ps`、路由回归、手动扩容与故障恢复演练 | depends_on: [1.8, 1.9, 1.10]

### 2. 阶段2：Swarm 演进与自动扩缩容（可选增强项）

- [√] 2.1 编写 `deploy/swarm/mapping.md` 与 `docker-stack.yml` 骨架，明确 Compose → Swarm 的网络、服务发现、configs/secrets 与节点约束映射规则 | depends_on: [1.11]
- [√] 2.2 在 `deploy/monitoring/prometheus/` 中补齐 Prometheus 抓取配置，并接入 Traefik、容器运行时、主机资源与 Spring Boot Actuator 指标采集 | depends_on: [2.1]
- [√] 2.3 在 `deploy/monitoring/autoscaler/` 中实现 Python 3.12 autoscaler 基础骨架，接入 docker SDK、Prometheus HTTP API、Redis 锁、dry-run 与日志输出 | depends_on: [2.1, 2.2]
- [√] 2.4 定义 `ui-service` / `biz-service` 的扩缩容阈值、冷却时间、最小/最大副本、单次步进、失败冻结与告警规则，并完成 dry-run 校准 | depends_on: [2.2, 2.3]
- [-] 2.5 完成 Swarm 自动扩容、自动缩容与回滚保护演练，验证指标缺失冻结、锁竞争保护与路由摘流策略 | depends_on: [2.3, 2.4]
  - 备注：当前执行聚焦 Compose 落地与阶段2骨架验证，未在本机执行真实 Swarm 自动扩缩容演练。
- [√] 2.6 编写阶段1/阶段2验收文档、发布回滚说明与进入阶段2的前置条件，明确首批上线边界与增强项边界 | depends_on: [1.11, 2.5]

---

## 执行日志

| 时间 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 2026-03-06 09:48:36 UTC | 方案包填充 | completed | 已按“方案C：三层分域 + Compose 到 Swarm 的渐进弹性方案”补齐 proposal.md 与 tasks.md |
| 2026-03-06 09:48:36 UTC | 任务 DAG 编排 | completed | 已拆分 14 个可执行任务，并明确阶段1为首批阻断项、阶段2为可选增强项 |
| 2026-03-06 10:07:54 UTC | Claude 可行性复核 | completed | 复核结论为“条件可行”，要求补齐 autoscaler 选型、无状态验证、Compose→Swarm 映射与备份恢复细节 |
| 2026-03-06 10:07:54 UTC | 方案包补强 | completed | 已将任务扩充为 17 项，并把无状态验证、备份恢复与 autoscaler 技术选型提升为明确闸门 |
| 2026-03-06 10:57:29 UTC | UI/Biz 单元测试 | completed | 已通过 UI/Biz 单元测试，支撑 Compose 多副本前的无状态改造验证 |
| 2026-03-06 10:57:29 UTC | autoscaler 单元测试 | completed | 已通过 autoscaler 单元测试，阶段2骨架与阈值控制逻辑已完成基础验证 |
| 2026-03-06 10:57:29 UTC | Compose 配置与镜像构建 | completed | 已通过 Compose 配置校验与镜像构建，可用于本地编排验证 |
| 2026-03-06 10:57:29 UTC | Compose 联调与备份验证 | completed | 已完成 Compose 启动、Traefik 路由验证、手动扩容到 2 副本、PostgreSQL/Redis 备份脚本验证 |
| 2026-03-06 10:57:29 UTC | Swarm 自动扩缩容演练 | skipped | 2.5 因未切换到实际 Swarm 运行环境而跳过，仅保留阶段2骨架、配置与文档产出 |
| 2026-03-06 10:57:29 UTC | 方案包状态收尾 | completed | 已同步任务状态、进度概览、验收说明与发布/回滚边界，整体状态更新为 completed |

---

## 执行备注

> 记录执行过程中的重要说明、决策变更、风险提示等

- 阶段1（任务 1.1-1.11）是首批交付的直接落地范围，目标是让 Compose 编排可部署、可验证、可手动扩容，并先完成无状态验证与备份恢复演练。
- 阶段2（任务 2.1-2.6）是可选增强项，用于在不引入 Kubernetes 的前提下演进到 Swarm + Prometheus + autoscaler，不属于首批阻断项。
- 自动扩缩容只作用于 `ui-service` 与 `biz-service`；PostgreSQL 18 与 Redis 在本方案中维持单实例持久化基线，不纳入自动扩缩容。
- autoscaler 技术选型已固定为 Python 3.12 + docker SDK + Prometheus HTTP API + Redis 锁；实施阶段不再以“轻量脚本”模糊带过。
- 如果 `ui-service` 或 `biz-service` 无法通过无状态验证，本方案必须先修正应用实现，再继续多副本与自动扩缩容实施。
- 默认网关选型为 Traefik；若后续项目已有既定网关实现，可在保持相同路由边界与健康检查协议的前提下替换，但不改变本方案的三层分域思路。
- 开发实施阶段应优先保持 Compose 与 Swarm 的环境变量命名、健康检查路径与镜像标签一致，避免阶段2演进时出现双份配置漂移。
- 当前已通过 UI/Biz 单元测试、autoscaler 单元测试、Compose 配置校验与镜像构建，说明阶段1落地与阶段2骨架均已具备基础可验证性。
- 当前已完成 Compose 启动、Traefik 路由验证、手动扩容到 2 副本，以及 PostgreSQL/Redis 备份脚本验证，满足本轮执行聚焦的 Compose 落地目标。
- 任务 2.5 未在本机切换到真实 Swarm 运行环境，因此仅记录为跳过；后续如进入真实 Swarm 集群，应补做自动扩容、自动缩容与回滚保护演练。

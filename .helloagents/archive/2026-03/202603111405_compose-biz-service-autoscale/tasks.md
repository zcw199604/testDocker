# 任务清单: compose-biz-service-autoscale

```yaml
@feature: compose-biz-service-autoscale
@created: 2026-03-11
@status: completed
@mode: R3
```

<!-- LIVE_STATUS_BEGIN -->
状态: completed | 进度: 20/20 (100%) | 更新: 2026-03-11 14:59:11 UTC
当前: 已完成代码、联调、知识库与归档回填
<!-- LIVE_STATUS_END -->

## 进度概览

| 完成 | 失败 | 跳过 | 总数 |
|------|------|------|------|
| 20 | 0 | 0 | 20 |

---

## 任务列表

### 1. Compose 编排与服务模板调整
- [√] 1.1 修改根目录 `docker-compose.yml` 中的 `biz-service` 定义，移除固定 `container_name` 与宿主机 `8080:8080` 端口映射，只保留容器内 `8080` 供 Compose 网络访问 | depends_on: []
- [√] 1.2 在 `docker-compose.yml` 中新增 `autoscale-agent` 服务、共享配置挂载与 Docker Socket 访问约定，并明确当前环境继续使用 `docker-compose` 作为配置源入口 | depends_on: [1.1]
- [√] 1.3 为 `biz-service` 与 `autoscale-agent` 补充扩缩容所需环境变量、策略配置、服务标签与网络约束，确保副本可被稳定发现与管理 | depends_on: [1.1, 1.2]

### 2. nginx 动态 upstream 与摘流机制
- [√] 2.1 修改 `nginx/nginx.conf`，将 `/api/*` 从静态单实例代理重构为引用 `biz_service_upstream` 动态 upstream | depends_on: [1.3]
- [√] 2.2 约定 `nginx/generated/biz-service.upstream.inc` 生成文件与原子替换流程，只允许健康副本入池，并支持 draining 副本先摘流再退出 | depends_on: [2.1]
- [√] 2.3 为 nginx 补充 reload 校验、健康检查与回滚逻辑，确保 upstream 更新失败时保留旧配置 | depends_on: [2.1, 2.2]

### 3. biz-service 多副本运行保障
- [√] 3.1 调整 `biz-service` 运行配置，启用 graceful shutdown、停机等待窗口与生命周期状态输出 | depends_on: [1.1]
- [√] 3.2 为 `biz-service` 增加 autoscale 指标采集与 `/api/autoscale/metrics`，确保新增副本健康通过后才可入池 | depends_on: [3.1]

### 4. autoscale-agent 控制面实现
- [√] 4.1 新建 `autoscale-agent/` 目录骨架（Dockerfile、依赖清单、主程序、测试目录），实现基础启动与配置加载 | depends_on: [1.2, 1.3]
- [√] 4.2 实现副本发现与健康状态机：区分 seed / managed 副本，并根据健康与运行状态驱动 upstream 更新 | depends_on: [2.2, 3.2, 4.1]
- [√] 4.3 实现扩容流程：当达到扩容阈值时基于 Compose 模板通过 Docker SDK 创建 managed 副本，并在 Docker Health + `GET /api/health` 通过后写入 upstream | depends_on: [4.2]
- [√] 4.4 实现缩容流程：选择目标 managed 副本、先从 upstream 摘流并 reload nginx、等待 draining，再 stop/rm 容器 | depends_on: [2.3, 3.1, 4.2]
- [√] 4.5 实现策略保护：补齐 `min_replicas`、`max_replicas`、`cooldown`、CPU / 内存阈值、业务指标阈值与配置守护逻辑 | depends_on: [4.3, 4.4]

### 5. 测试与联调验证
- [√] 5.1 编写/补充单元测试，覆盖策略判断、upstream 生成、nginx 回滚、配置解析与目标副本数收敛 | depends_on: [4.3, 4.4, 4.5]
- [√] 5.2 执行 Compose 配置校验与镜像构建，确认 `docker-compose config`、`docker build` 与基础启动流程可用 | depends_on: [1.3, 2.3, 4.1]
- [√] 5.3 完成扩容联调：将 `biz-service` 扩到 seed + managed 组合，验证新副本健康通过后才入池，并经 nginx 实际承接流量 | depends_on: [5.1, 5.2]
- [√] 5.4 完成缩容联调：验证目标副本先摘流、等待 draining、再缩容回落，且 `/api/health`、PostgreSQL notes、Redis KV 路径保持可用 | depends_on: [5.1, 5.2, 5.3]

### 6. 文档与知识库同步
- [√] 6.1 更新 `.helloagents/context.md`、`modules/deploy-compose.md`、`modules/nginx-gateway.md`、`modules/biz-service.md` 与新增 `modules/autoscale-agent.md`，记录真实运行边界与实现口径 | depends_on: [5.3, 5.4]
- [√] 6.2 更新 `.helloagents/INDEX.md`、`modules/_index.md` 与 `CHANGELOG.md`，补充“Compose 配置源协调型自动扩缩容”方案、决策编号与验收结果 | depends_on: [6.1]
- [√] 6.3 回填 `proposal.md` / `tasks.md` 状态并归档方案包，确保知识库记录与实际代码一致 | depends_on: [6.2]

---

## 执行日志

| 时间 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 2026-03-11 14:05:00 UTC | 方案包编写 | completed | 已选定“Compose 配置源协调型自动扩缩容”，并拆分 20 个可执行任务 |
| 2026-03-11 14:31:00 UTC | autoscale-agent 去重与入口统一 | completed | 保留 `autoscale_agent/` 主实现，移除旧 `src/` 路径并统一 Dockerfile / 测试入口 |
| 2026-03-11 14:39:00 UTC | Python / Java / Compose 基础校验 | completed | autoscale-agent 单元测试、biz-service Maven 测试、`docker-compose config`、镜像构建均通过 |
| 2026-03-11 14:44:15 UTC | 第一次自动扩容闭环 | completed | 新 managed 副本健康通过后被加入 nginx upstream |
| 2026-03-11 14:44:39 UTC | 第一次自动缩容闭环 | completed | 先摘流再 stop/remove managed 副本 |
| 2026-03-11 14:46:07 UTC | 第二次自动扩容与流量分发验证 | completed | 连续请求 `/api/autoscale/metrics` 观测到 2 个不同 `instanceId` |
| 2026-03-11 14:46:31 UTC | 第二次自动缩容闭环 | completed | upstream 先恢复为仅 seed backend，再回收 managed 副本 |
| 2026-03-11 14:59:11 UTC | 知识库与归档回填 | completed | 同步 context、modules、CHANGELOG、INDEX 并完成方案包归档 |

---

## 执行备注
- 当前实现最终口径是“Compose 配置源 + Docker SDK 直控 managed 副本”，与初始计划中的 `docker-compose --scale` 执行口径不同；知识库已按代码事实修正。
- 本轮自动扩缩容只覆盖 `biz-service`；`ui-service`、`postgres`、`redis` 保持现有职责边界。
- 缩容前先摘流后缩容已经通过两轮日志与 upstream 文件变化验证，不是仅停留在设计描述中。

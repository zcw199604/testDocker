# 任务清单: compose-split-legacy-autoscale

> **@status:** completed | 2026-03-12 02:36

```yaml
@feature: compose-split-legacy-autoscale
@created: 2026-03-12
@updated: 2026-03-12 02:36:30 UTC
@status: completed
@mode: R2
@package_type: implementation
@selected_plan: 方案A：根目录保留旧版5服务，新增 autoscale 专用 compose
@review_status: 已实施并完成配置验证与知识库同步
```

<!-- LIVE_STATUS_BEGIN -->
状态: completed | 进度: 6/6 (100%) | 更新: 2026-03-12 02:36:30 UTC
当前: 已完成 Compose 拆分、配置验证、知识库同步与归档准备
<!-- LIVE_STATUS_END -->

## 进度概览

| 完成 | 失败 | 跳过 | 总数 |
|------|------|------|------|
| 6 | 0 | 0 | 6 |

---

## 任务列表

### 1. 编排入口拆分
- [√] 1.1 将根目录 `docker-compose.yml` 调整为旧版单机 5 服务编排，移除 autoscale-agent 与 autoscale 专属 biz-service 配置 | depends_on: []
- [√] 1.2 新增 `docker-compose.autoscale.yml`，保留当前 6 服务自动扩缩容编排，并修正 autoscale-agent 对 compose 文件的宿主挂载 | depends_on: [1.1]
- [√] 1.3 新增 `nginx/nginx.legacy.conf`，为旧版 5 服务恢复静态 `biz_service_upstream` 代理配置 | depends_on: [1.1]

### 2. 验证与知识同步
- [√] 2.1 分别验证 `docker-compose.yml` 与 `docker-compose.autoscale.yml` 的配置可解析，确认服务数与关键挂载正确 | depends_on: [1.2, 1.3]
- [√] 2.2 同步 `.helloagents` 中与 compose 入口、nginx 路由、autoscale 入口相关的知识库文档 | depends_on: [2.1]
- [√] 2.3 回填方案包状态、更新变更记录并归档 | depends_on: [2.2]

---

## 执行日志

| 时间 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 2026-03-12 02:28:58 UTC | 方案包创建 | completed | 已创建 `202603120228_compose-split-legacy-autoscale` 方案包 |
| 2026-03-12 02:28:58 UTC | 方案选择 | completed | 已确定采用“双入口拆分 + 旧版静态 nginx + 新版 autoscale compose” |
| 2026-03-12 02:29:00 UTC | pkg_keeper 降级 | partial | 子代理并行创建了 0227 方案包，主代理接手使用 0228 方案包继续执行 |
| 2026-03-12 02:31:00 UTC | 任务 1.1/1.2/1.3 完成 | completed | 已拆分 legacy 5 服务 compose、autoscale 6 服务 compose，并新增 nginx.legacy.conf |
| 2026-03-12 02:33:00 UTC | 任务 2.1 完成 | completed | 已通过 docker-compose config 与 YAML 解析验证两套 Compose 服务清单 |
| 2026-03-12 02:34:30 UTC | nginx 语法验证 | completed | 已通过 docker run + mock hosts 验证 legacy/autoscale 两套 nginx 配置语法 |
| 2026-03-12 02:35:30 UTC | 任务 2.2 完成 | completed | 已同步 INDEX、context、deploy-compose、nginx-gateway、autoscale-agent、biz-service 与模块索引 |
| 2026-03-12 02:36:30 UTC | 任务 2.3 完成 | completed | 已回填方案包状态、更新 CHANGELOG 并准备归档 |

---

## 执行备注

- 两套 compose 设计为替代关系，不建议同时启动。
- autoscale 版本的运行时副本控制口径保持不变：Compose 仅作为模板源，实际扩缩容由 Docker SDK 直控 managed 副本。

- 执行验证时，`docker compose -f` 在当前环境不可用，实际使用 `docker-compose -f` 完成配置校验。
- `nginx -t` 在独立容器中会解析 upstream 主机名，因此验证时通过 `--add-host ui-service:127.0.0.1 --add-host biz-service:127.0.0.1` 注入 mock hosts。
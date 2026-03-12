# 变更提案: compose-biz-service-autoscale

## 元信息
```yaml
类型: 新功能
方案类型: implementation
优先级: P1
状态: 已完成
创建: 2026-03-11
更新: 2026-03-11 14:59:11 UTC
推荐方案: Compose 配置源协调型自动扩缩容
实际落地: Compose 作为配置源 + Docker SDK 直控 managed 副本 + nginx 动态 upstream
复核结论: 已在当前 docker-compose 基线上完成自动扩缩容闭环验证
```

---

## 1. 目标与范围
- 保持当前环境继续使用 `docker-compose`。
- 仅对 `biz-service` 引入自动扩缩容。
- 采集业务指标与容器 CPU / 内存，并通过环境变量配置阈值。
- 新实例必须在健康通过后才加入 nginx 代理组。
- 缩容必须先摘流、等待 drain，再停止并删除目标副本。

## 2. 实际实施结果

### 2.1 Compose 与服务编排
- `docker-compose.yml` 已扩展为 6 服务编排：`postgres`、`redis`、`ui-service`、`biz-service`、`nginx`、`autoscale-agent`。
- `biz-service` 已移除固定 `container_name` 与宿主机 `8080:8080` 暴露，只保留容器内 `8080`。
- `biz-service` 已增加 autoscale labels 与 `AUTOSCALE_METRICS_WINDOW_DURATION=15s`。
- `autoscale-agent` 已挂载 Docker Socket、只读 Compose 文件与 `nginx/generated` 目录。

### 2.2 nginx 动态 upstream
- `nginx/nginx.conf` 已改为通过 `biz_service_upstream` 代理 `/api/*`。
- `nginx/generated/biz-service.upstream.inc` 由 `autoscale-agent` 原子生成。
- upstream 更新路径已落实为：写临时文件 -> `nginx -t` -> `nginx -s reload` -> 失败回滚。

### 2.3 biz-service 多副本运行保障
- 已新增 `GET /api/autoscale/metrics`。
- 已新增 `AutoscaleMetricsFilter` / `AutoscaleMetricsService` / `AutoscaleMetricsResponse`。
- 已启用 `server.shutdown: graceful` 与 shutdown 生命周期状态跟踪。
- 当前 `status` 可表达 `STARTING` / `READY` / `STOPPING`。

### 2.4 autoscale-agent 控制面
- 当前实现不是调用 `docker-compose --scale` 实时扩缩容，而是读取 Compose 中的 `biz-service` 模板后，使用 Docker SDK 直接创建 / 删除 managed 副本。
- 扩容流程：阈值命中 -> 创建 managed 副本 -> 等待 Docker Health + `/api/health` -> 更新 nginx upstream -> reload。
- 缩容流程：阈值命中 -> 从 upstream 移除目标 managed 副本 -> reload nginx -> 等待 drain / inflight 清空 -> stop / remove managed 副本。
- 当前策略已支持 `min_replicas`、`max_replicas`、`scale_up_window`、`scale_down_window`、`cooldown_seconds`、`drain_seconds`、CPU / 内存阈值、业务指标规则、`dry_run` 与 `failure_freeze` 配置解析。

## 3. 验收结果
- [x] `docker-compose.yml` 仍是唯一真实编排入口，且 `docker-compose config` 成功通过。
- [x] `biz-service` 不再使用固定 `container_name`，也不再对宿主机暴露 `8080:8080`。
- [x] `nginx` 的 `/api/*` 已改为引用动态 upstream，而不是静态单实例代理。
- [x] `autoscale-agent` 已在新副本 Docker Health + `GET /api/health` 都通过后再把副本加入 upstream。
- [x] `autoscale-agent` 已验证缩容前先摘流、reload nginx、等待 drain，再删除副本。
- [x] `biz-service` 已补齐 autoscale 指标、instanceId、graceful shutdown 与生命周期状态。
- [x] 自动扩缩容策略参数已可配置，并附带 Python / Java 测试与 Compose 联调验证。
- [x] 已至少完成两次扩容与两次缩容联调，并验证新副本经 nginx 实际承接流量。
- [x] 知识库、变更记录与归档信息已同步。

## 4. 联调证据摘要
- `docker-compose config --services` 输出 6 个服务。
- `python3 -m unittest discover -s autoscale-agent/tests -v` 当前通过 8 个测试。
- `mvn test`（经 Maven 容器执行）已通过。
- `docker-compose up -d --build` 后，`docker-compose ps` 显示基础服务 healthy，`autoscale-agent` 持续运行。
- 2026-03-11 14:44:06 UTC / 14:45:52 UTC：日志记录自动扩容触发。
- 2026-03-11 14:44:15 UTC / 14:46:07 UTC：日志记录新副本健康通过并加入 upstream。
- 通过 nginx 连续访问 `/api/autoscale/metrics`，观测到两个不同 `instanceId`，证明流量已分发到 managed 副本。
- 2026-03-11 14:44:39 UTC / 14:46:31 UTC：日志记录自动缩容，且顺序为先摘流再 stop/remove。

## 5. 关键决策
### compose-biz-service-autoscale#D001
采用“Compose 配置源协调型自动扩缩容”，保留当前 `docker-compose` 基线。

### compose-biz-service-autoscale#D002
使用 nginx 生成式 upstream 控制健康副本池，而不是继续静态代理 `biz-service:8080`。

### compose-biz-service-autoscale#D003
运行时副本控制采用“Compose 作为模板 + Docker SDK 直控 managed 副本”，而不是依赖 `docker-compose --scale` 作为实时控制平面。

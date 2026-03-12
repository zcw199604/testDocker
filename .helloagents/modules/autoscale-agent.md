# autoscale-agent

## 范围
- `autoscale-agent/Dockerfile`
- `autoscale-agent/requirements.txt`
- `autoscale-agent/autoscale_agent/`
- `autoscale-agent/tests/`

## 当前事实
- `autoscale-agent` 是一个基于 Python 3.12 的本地自动扩缩容协调器，当前通过 `python -m autoscale_agent` 启动。
- 当前实现采用“Compose 配置源协调型”：
  - `compose_loader.py` 从 `docker-compose.yml` 读取 `biz-service` 模板；
  - `runtime.py` 使用 Docker SDK 发现 seed / managed 副本，并直接创建 / 停止 / 删除 managed 副本；
  - `controller.py` 负责主循环、扩容、健康等待、缩容摘流与回收流程；
  - `metrics.py` 聚合 CPU / 内存 / 业务指标；
  - `policy.py` 根据窗口命中次数与 cooldown 做扩缩容判定；
  - `nginx_control.py` 负责生成 upstream、回滚与 nginx reload。
- 当前 `autoscale-agent` 不依赖 `docker-compose --scale` 执行实时副本控制；Compose 仅作为服务定义与默认环境配置源。
- 当前健康门禁为双重检查：Docker Health + `GET /api/health`。
- 当前业务指标规则通过 `AUTOSCALE_APP_METRIC_RULES` 配置，已验证的字段包括 `requestRatePerSecond` 与 `inflightRequests`。
- 当前配置代码已支持 `dry_run` 与 `failure_freeze` 守护开关解析。
- 当前缩容流程为：先从 upstream 移除目标副本 -> `nginx -t` + reload -> 等待 drain / inflight 清空 -> stop / remove managed 容器。

## 当前测试
- `test_config.py`：验证环境配置与布尔开关解析。
- `test_policy.py`：验证 scale up / scale down / cooldown 判定。
- `test_upstream.py`：验证 upstream 渲染与空后端占位逻辑。
- `test_nginx_control.py`：验证 upstream 更新失败时的回滚逻辑与 reload 流程。

## 已验证结果
- `python3 -m compileall autoscale-agent/autoscale_agent` 已通过。
- `python3 -m unittest discover -s autoscale-agent/tests -v` 已通过 8 个测试。
- `docker build -t testdocker-autoscale-agent:latest autoscale-agent` 已通过。
- 联调期间，`autoscale-agent` 已至少两次自动扩容并两次自动缩容：
  - 扩容原因日志记录了 `requestRatePerSecond` 超阈值；
  - 健康通过后 upstream 从 1 backend 变为 2 backends；
  - 缩容日志记录了先摘流再 stop/remove 的完整顺序。

## 当前边界
- 当前实现只管理 `biz-service`，不扩展到 `ui-service`、`postgres`、`redis`。
- 当前实现未接入 Prometheus、cAdvisor、Kubernetes 或 Swarm。
- 当前 managed 副本使用 Docker SDK 直接创建，因此不出现在 compose service DNS 列表中，需要由 nginx include 文件显式写入其容器 IP。

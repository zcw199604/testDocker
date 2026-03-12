# 变更提案: compose-split-legacy-autoscale

## 元信息
```yaml
类型: 重构
方案类型: implementation
优先级: P1
状态: 已完成
创建: 2026-03-12
更新: 2026-03-12 02:36:30 UTC
推荐方案: 方案A：根目录保留旧版5服务，新增 autoscale 专用 compose
```

---

## 1. 目标与范围
- 将当前单一 `docker-compose.yml` 拆分为两套独立可运行的编排入口。
- 旧版保留单机 5 服务：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`。
- 新版保留本地自动扩缩容能力：在 5 服务基线上新增 `autoscale-agent`，并保持 `biz-service` seed/managed 运行模式。
- 同步必要的 nginx 配置与知识库事实，避免“当前唯一 compose 入口”这类描述失真。

## 2. 当前事实与约束
- 当前代码树只有一个根目录 `docker-compose.yml`，内容已是 6 服务 autoscale 版本。
- `autoscale-agent` 通过 Docker SDK 直控 `biz-service` managed 副本，而不是依赖 Compose 原生 `--scale`。
- 当前 nginx 自动扩缩容版本依赖 `nginx/generated/biz-service.upstream.inc` 动态 include；旧版 5 服务若要干净回退，应恢复静态 upstream。
- `autoscale-agent` 运行时需要读取 compose 模板文件，但内部路径可继续固定为 `/workspace/docker-compose.yml`，通过绑定不同宿主文件适配。

## 3. 实施方案
### 方案A：根目录保留旧版5服务，新增 autoscale 专用 compose
- 将根目录 `docker-compose.yml` 调整为旧版 5 服务编排。
- 新增 `docker-compose.autoscale.yml`，保留当前 6 服务自动扩缩容能力。
- 新增 `nginx/nginx.legacy.conf`，供旧版 5 服务使用静态 `biz_service_upstream`。
- autoscale 版本继续使用当前 `nginx/nginx.conf` 与 `nginx/generated/`，并将 `docker-compose.autoscale.yml` 映射到 `autoscale-agent` 容器内的 `/workspace/docker-compose.yml`。

### 选择理由
- 入口语义清晰：旧版与新版文件名职责明确。
- 最大限度复用现有 autoscale 代码与 nginx 动态 upstream 机制，不需要修改 Python 控制面实现。
- 旧版 5 服务可作为更简单的本地基线，便于回归和对比。

## 4. 影响范围
```yaml
涉及模块:
  - deploy-compose: 拆分 compose 入口与运行说明
  - nginx-gateway: 新增旧版静态 nginx 配置，保留 autoscale 动态配置
  - autoscale-agent: 入口文件名从默认根 compose 切换为 autoscale 专用 compose 宿主文件
  - knowledge-base: 更新 INDEX/context/modules/CHANGELOG/archive 索引
预计变更文件: 8-10
```

## 5. 风险评估
| 风险 | 等级 | 应对 |
|------|------|------|
| 旧版 compose 仍误挂载 autoscale generated 配置 | 中 | 为旧版使用独立 `nginx.legacy.conf`，避免动态 include 依赖 |
| autoscale 版本找不到 compose 模板文件 | 中 | 在 `docker-compose.autoscale.yml` 中把宿主文件 `docker-compose.autoscale.yml` 映射到容器内固定路径 `/workspace/docker-compose.yml` |
| 知识库仍描述“唯一 compose 入口” | 中 | 同步 `INDEX.md`、`context.md`、`modules/deploy-compose.md`、`modules/nginx-gateway.md`、`modules/autoscale-agent.md` |
| 用户误同时启动两套 compose | 低 | 文档中明确两套编排是替代关系，不建议同时拉起 |

## 6. 验收标准
- [x] 根目录 `docker-compose.yml` 可解析为 5 服务编排，且不包含 `autoscale-agent`。
- [x] `docker-compose.autoscale.yml` 可解析为 6 服务编排，保留 autoscale 相关 labels、环境变量与挂载。
- [x] `nginx/nginx.legacy.conf` 为旧版提供静态 `biz_service_upstream`，不依赖 generated include。
- [x] autoscale 版本中的 `autoscale-agent` 能从 `docker-compose.autoscale.yml` 读取模板（通过容器内固定路径映射）。
- [x] 知识库已同步说明“两套 compose 入口”的事实与使用边界。

## 7. 关键决策
### compose-split-legacy-autoscale#D001
采用“双入口”拆分：根目录 `docker-compose.yml` 回归旧版 5 服务，新增 `docker-compose.autoscale.yml` 承载本地自动扩缩容。

### compose-split-legacy-autoscale#D002
旧版 5 服务使用独立 `nginx/nginx.legacy.conf` 恢复静态 `biz_service_upstream`，而不是继续复用 autoscale 动态 include 链路。

### compose-split-legacy-autoscale#D003
保持 `autoscale-agent` 容器内的 compose 模板路径为 `/workspace/docker-compose.yml`，通过宿主绑定文件切换到 `docker-compose.autoscale.yml`，避免修改 Python 控制面默认约定。

# 食材集中采购与监管平台 · 操作手册

本文档面向系统管理员、采购员、供应商与监管人员，覆盖部署、账户、日常使用、API、备份、监控与故障排查。

## 1. 系统概述

平台面向学校、医院及监管部门，覆盖从食材标准、供应商、采购计划、询价竞价、订单、配送验收、库存结算到批次追溯与监管驾驶舱的完整采购闭环。

架构：

- `frontend`：Vue 3 + TypeScript + Vite + Element Plus 管理台（nginx 托管）
- `backend`：Spring Boot 3.5 + Spring Security（HTTP Basic + RBAC）+ JPA + Flyway + OpenAPI
- `postgres`：业务数据；`redis`：缓存预留（当前未使用）

访问入口：前端 `http://localhost:8080`，OpenAPI 文档 `http://localhost:8080/api/swagger-ui/index.html`，健康检查 `http://localhost:8080/api/actuator/health`。

## 2. 快速部署

前置条件：已安装 Docker 与 Docker Compose。

```bash
cp .env.example .env
# 编辑 .env，至少替换 ADMIN_PASSWORD / BUYER_PASSWORD / SUPPLIER_PASSWORD / POSTGRES_PASSWORD / REDIS_PASSWORD
docker compose up -d --build
```

未配置账户密码时，`docker compose` 会直接报错并拒绝启动——这是有意设计，避免默认口令上线。

常用命令：

```bash
docker compose ps            # 查看服务状态
docker compose logs -f api   # 查看后端日志
docker compose down          # 停止（数据卷保留）
docker compose down -v       # 停止并删除数据卷（会清空数据，谨慎）
```

## 3. 账户与权限

账户由环境变量注入（见 `.env.example`），应用代码不再内置任何默认口令。三个默认角色：

| 角色 | 用户名（可改） | 权限范围 |
|---|---|---|
| 监管员/管理员 | `ADMIN_USERNAME` | 全部模块 + 监管驾驶舱 |
| 采购员 | `BUYER_USERNAME` | 除驾驶舱外的全部业务模块 |
| 供应商 | `SUPPLIER_USERNAME` | 仅查看询价列表、提交/查看自己的报价 |

供应商账户的登录名需要与供应商档案的「登录账户名（accountUsername）」字段一致，才能正常报价。创建供应商档案时请填写该字段（管理员/采购员操作）。

生产部署建议：在反向代理（Nginx/Caddy）层接入企业 SSO/OIDC 并启用 HTTPS，用令牌替代长期 Basic 凭据。

## 4. 日常操作流程

1. 采购员/监管员维护「食材标准库」与「供应商」档案。
2. 创建「采购计划」。
3. 发起「询价/竞价」，状态设为 `OPEN`，可设置截止时间并关联采购计划。
4. 供应商登录后，在询价列表对 `OPEN` 的询价提交报价（可重复提交，以最新报价为准）。
5. 采购员在询价的「报价/查看」中查看各家报价，据此创建「采购订单」。
6. 依次创建「配送」→「智能验收」→「库存批次」→「结算」。
7. 监管员在「监管驾驶舱」查看全局统计，通过库存「批次号」调用追溯接口核查来源。

## 5. API 参考

基础路径 `/api/v1`，认证方式 HTTP Basic。列表接口统一分页与搜索。

分页查询参数：

| 参数 | 说明 |
|---|---|
| `page` | 页码，从 0 开始，默认 0 |
| `size` | 每页条数，默认 20 |
| `q` | 关键字搜索（按各资源的主字段模糊匹配） |
| `sort` | 排序字段，默认按创建/入库时间倒序 |

列表响应结构：

```json
{ "items": [], "total": 0, "page": 0, "size": 20 }
```

主要接口：

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/me` | 当前登录用户与角色 | 已登录 |
| GET | `/standards` `/suppliers` `/plans` `/orders` `/deliveries` `/inspections` `/inventory` `/settlements` | 分页列表 | 采购/监管 |
| POST | 同上路径 | 新建记录（带校验） | 采购/监管 |
| GET | `/inquiries` | 询价分页列表 | 采购/监管/供应商 |
| POST | `/inquiries` | 新建询价 | 采购/监管 |
| GET | `/inquiries/{id}/bids` | 查看报价（供应商仅见自己的） | 采购/监管/供应商 |
| POST | `/inquiries/{id}/bids` | 提交报价（最新覆盖） | 采购/监管/供应商 |
| GET | `/traceability/{lotNo}` | 按批次追溯食材来源 | 采购/监管 |
| GET | `/dashboard` | 监管驾驶舱统计 | 监管 |

错误码：`400` 校验/完整性错误、`401` 未认证、`403` 无权限、`404` 不存在、`409` 业务冲突（如询价已关闭/已截止）、`500` 服务器错误。

## 6. 数据模型

核心表：`food_standard`、`supplier`、`procurement_plan`、`inquiry`、`bid`、`purchase_order`、`delivery`、`inspection`、`inventory_lot`、`settlement`。所有主键为 UUID，迁移文件位于 `backend/src/main/resources/db/migration/`，由 Flyway 在启动时自动执行。

## 7. 备份与恢复

备份（保留数据卷，只导出数据库）：

```bash
docker compose exec -T postgres pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > backup.sql
```

恢复：

```bash
docker compose exec -T postgres psql -U "$POSTGRES_USER" "$POSTGRES_DB" < backup.sql
```

建议使用定时任务配合异地存储保存备份，并在恢复后重启 `api` 服务。

## 8. 监控与健康检查

- 健康检查：`GET /api/actuator/health`（返回 `{"status":"UP"}`）。
- 容器状态：`docker compose ps` 会展示各服务的健康状态。
- 日志：`docker compose logs -f api`，默认 INFO 级别。

## 9. 安全清单

- [ ] 已在 `.env` 中设置所有强密码（账户、PostgreSQL、Redis）
- [ ] 已通过反向代理启用 HTTPS
- [ ] 生产环境已接入企业 SSO/OIDC
- [ ] 数据库与 Redis 端口仅绑定本机（`127.0.0.1`）或内网
- [ ] 已配置数据库定期备份
- [ ] 已限制 Swagger 文档对公网的访问（可按需收紧 `SecurityConfig` 中的放行规则）

## 10. 升级流程

```bash
git pull
docker compose up -d --build
docker compose ps   # 确认全部 healthy
```

Flyway 会自动执行新增的数据库迁移；如需回滚，请恢复数据库备份后再回退代码版本。

## 11. 故障排查

| 现象 | 处理 |
|---|---|
| 前端提示「无法连接服务」 | 确认后端已启动：`docker compose ps`、`docker compose logs -f api` |
| 登录提示「用户名或密码错误」 | 确认 `.env` 中的账户变量与启动容器一致，重启 `docker compose up -d --force-recreate api` |
| 供应商无法报价 | 确认询价状态为 `OPEN`、未过截止时间，且供应商档案的「登录账户名」与该供应商登录名一致 |
| 提交返回「数据违反唯一性或完整性约束」 | 检查必填字段与唯一编号（编码/编号等）是否重复 |
| 端口被占用 | 修改 `docker-compose.yml` 中 `web`/数据库的宿主机端口映射 |

## 12. 已知限制与后续规划

- 账户为内存配置，单实例运行；多实例或大规模用户需改为数据库/目录驱动的用户源。
- 暂无软删除与 PATCH/DELETE，记录只能新建。
- 关联对象通过 ID 选择，未做跨机构数据隔离（多租户）。
- 无接口限流，生产建议前置 API 网关。
- 前端全量引入 Element Plus，主包约 1MB，可后续做按需引入与代码分割。
- Redis 目前为预留，尚未承载缓存。

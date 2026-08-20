# 食材集中采购与监管平台（MVP）

面向学校、医院及监管部门的采购协同平台。MVP 覆盖食材标准、供应商、采购计划、询价竞价、订单、配送验收、库存结算、批次追溯和监管驾驶舱。

## 快速开始

```bash
cp .env.example .env
# 编辑 .env：至少替换 ADMIN_PASSWORD / BUYER_PASSWORD / SUPPLIER_PASSWORD（生产环境务必使用强密码）
docker compose up --build
```

访问 `http://localhost:8080`；OpenAPI 文档：`http://localhost:8080/api/swagger-ui/index.html`。

账户（HTTP Basic）：用户名与密码全部来自环境变量（`ADMIN_USERNAME`/`ADMIN_PASSWORD`、`BUYER_*`、`SUPPLIER_*`，见 `.env.example`），应用代码不再内置任何默认账户。未配置账户密码时 `docker compose` 会直接报错拒绝启动——这是有意设计，避免默认口令上线。生产部署建议在此基础上接入企业 SSO/OIDC。

完整部署、账户、日常操作、API 与备份指南见 [docs/OPERATIONS.md](docs/OPERATIONS.md)。

## 架构

- `frontend`：Vue 3 + TypeScript + Vite + Element Plus 管理台（真实表单/表格、分页搜索、角色化菜单）
- `backend`：Spring Boot 3.5、Spring Security RBAC、JPA、Flyway、OpenAPI、Actuator
- `postgres`：业务数据与审计数据；`redis`：竞价/驾驶舱缓存预留
- 健康检查：`/api/actuator/health`

## 主要 API

`/api/v1/standards`、`/suppliers`、`/plans`、`/inquiries`、`/orders`、`/deliveries`、`/inspections`、`/inventory`、`/settlements`、`/traceability`、`/dashboard`。

## 验证

```bash
cd backend && mvn test
cd ../frontend && npm ci && npm run build
```

所有 POST 接口均带基础校验（必填字段、长度、金额范围等），非法请求返回 400；同一供应商对同一询价重复报价会以最新报价覆盖。

CI 会分别构建后端与前端（前端使用已提交的 `package-lock.json` 安装依赖，保证构建可复现）。提交前请运行上述命令。

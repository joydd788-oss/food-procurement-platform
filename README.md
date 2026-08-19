# 食材集中采购与监管平台（MVP）

面向学校、医院及监管部门的采购协同平台。MVP 覆盖食材标准、供应商、采购计划、询价竞价、订单、配送验收、库存结算、批次追溯和监管驾驶舱。

## 快速开始

```bash
cp .env.example .env
docker compose up --build
```

访问 `http://localhost:8080`；OpenAPI 文档：`http://localhost:8080/api/swagger-ui/index.html`。

开发账户（HTTP Basic）：`admin / admin123`（监管员），`buyer / buyer123`（采购员），`supplier / supplier123`（供应商）。生产部署必须通过环境变量替换账户，并接入企业 SSO/OIDC。

## 架构

- `frontend`：Vue 3 + TypeScript + Vite + Element Plus 管理台
- `backend`：Spring Boot 3、Spring Security RBAC、JPA、Flyway、OpenAPI
- `postgres`：业务数据与审计数据；`redis`：竞价/驾驶舱缓存预留

## 主要 API

`/api/v1/standards`、`/suppliers`、`/plans`、`/inquiries`、`/orders`、`/deliveries`、`/inspections`、`/inventory`、`/settlements`、`/traceability`、`/dashboard`。

## 验证

```bash
cd backend && mvn test
cd ../frontend && npm install && npm run build
```

CI 会分别构建后端与前端。提交前请运行上述命令；本环境未预装 Java/Node/Docker，未能在本机执行。

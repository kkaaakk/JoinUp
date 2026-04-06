# 第11段：joinup-admin 管理后台模块

## 本段目标
实现 JoinUp 管理后台的基础骨架，覆盖活动审核复用说明、举报处理、异常活动下架、用户信用人工干预、风险用户监控、热门活动监控、操作日志查询，以及 RBAC 权限编码预留。

## 已完成内容
1. 复用已存在的活动审核接口：`POST /api/admin/activity/review/{id}`
   - 代码位置：`joinup-activity/src/main/java/com/joinup/activity/controller/AdminActivityController.java`
   - 由于这条接口在第5段已完成，所以本段没有重复定义，避免出现重复 Bean 与重复路由。
2. 新增管理后台举报处理接口：
   - `GET /api/admin/report/page`
   - `POST /api/admin/report/handle/{id}`
3. 新增管理后台统一信用人工调整接口：
   - `POST /api/admin/user/credit/adjust`
   - 该接口由 `joinup-admin` 统一对外，内部委托 `joinup-credit` 完成真正的信用规则与落库。
4. 新增监控接口：
   - `GET /api/admin/activity/hot/page`
   - `GET /api/admin/user/risk/page`
5. 新增操作日志接口：
   - `GET /api/admin/log/page`
6. 新增后台 Mapper / Service / Controller 骨架，并补齐中文方法级注释。
7. 新增基础 RBAC 权限编码常量：
   - `admin:activity:review`
   - `admin:report:handle`
   - `admin:user:credit:adjust`
   - `admin:monitor:hot:view`
   - `admin:monitor:risk:view`
   - `admin:log:view`
8. 新增后台通用权限校验器 `AdminPermissionChecker`。
   - 当前临时规则仍是用户名 `admin` 才允许通过。
   - 后续接 RBAC 时只需要替换这里的实现。
9. 新增后台操作日志落库与分页查询服务。
10. 举报处理已经具备这几个联动动作：
   - 更新举报状态
   - 可选下架被举报活动
   - 可选禁用被举报用户
   - 写入后台操作日志

## 关键代码位置
- `joinup-admin/src/main/java/com/joinup/admin/controller/AdminReportController.java`
- `joinup-admin/src/main/java/com/joinup/admin/controller/AdminUserController.java`
- `joinup-admin/src/main/java/com/joinup/admin/controller/AdminMonitorController.java`
- `joinup-admin/src/main/java/com/joinup/admin/controller/AdminLogController.java`
- `joinup-admin/src/main/java/com/joinup/admin/service/impl/AdminReportServiceImpl.java`
- `joinup-admin/src/main/java/com/joinup/admin/service/impl/AdminUserServiceImpl.java`
- `joinup-admin/src/main/java/com/joinup/admin/service/impl/AdminMonitorServiceImpl.java`
- `joinup-admin/src/main/java/com/joinup/admin/service/impl/OperationLogServiceImpl.java`
- `joinup-admin/src/main/java/com/joinup/admin/mapper/AdminActivityReportMapper.java`
- `joinup-admin/src/main/java/com/joinup/admin/mapper/AdminMonitorMapper.java`
- `joinup-admin/src/main/java/com/joinup/admin/mapper/OperationLogMapper.java`
- `joinup-admin/src/main/java/com/joinup/admin/constant/AdminPermissionConstants.java`
- `joinup-admin/src/main/java/com/joinup/admin/domain/AdminPermissionChecker.java`

## RBAC 建议
1. 角色层：至少拆成 `SUPER_ADMIN`、`AUDITOR`、`RISK_OPERATOR`、`OPS_VIEWER`。
2. 权限层：接口权限、菜单权限、按钮权限统一使用 `admin:*` 风格编码。
3. 数据权限：后续可按校区 / 社区 / 活动类型做范围隔离。
4. 审计要求：所有后台写操作都应落操作日志，并带上操作人、业务对象、结果和说明。
5. 接入方式：后续优先把 `AdminPermissionChecker` 接到 Spring Security 的 Authority 判定，而不是把判断逻辑散落在 Controller。

## 当前边界
1. 本段没有重复实现活动审核接口，而是直接复用第5段已有实现。
2. 本段也没有擅自改造前台业务流，只提供管理后台骨架与必要的治理联动。
3. 操作日志当前主要记录后台显式操作，尚未自动拦截所有后台请求；如果后续要扩展，可以再补统一切面。
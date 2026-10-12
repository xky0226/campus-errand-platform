# 校园帮 —— 校园互助跑腿平台

一个完整的校园跑腿服务平台：**Spring Boot 后端 + Vue 3 管理端 + uni-app 移动端 + Spring AI 智能助手 + 接口自动化测试**。

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.6-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Pytest](https://img.shields.io/badge/Pytest-Allure-0A9EDC?logo=pytest&logoColor=white)](https://docs.pytest.org/)

## 模块一览

| 目录 | 内容 | 技术栈 |
|---|---|---|
| `shixun/` | 后端服务 | Spring Boot 3.1.6 · MyBatis-Plus · MySQL · Redis · JWT · Druid |
| `campus-admin/` | 后台管理端 | Vue 3 · TypeScript · Element Plus · Vite |
| `shixun_uniapp/` | 移动端小程序 | uni-app |
| `springai_exam/` | AI 智能助手（校园百事通） | Spring Boot · Spring AI · DeepSeek |
| `tests/` | 接口自动化测试 | Python · Requests · Pytest · Allure |
| `campus.sql` | 数据库结构（15 张表） | MySQL |

## 后端架构

```
shixun/src/main/java/com/example/campus/
├── controller/     # 接口层：Auth / Orders / Users / Admin / AdminDashboard
├── service/        # 业务层（接口 + impl 分离）
├── mapper/         # 数据访问层（MyBatis-Plus）
├── entity/         # 数据库实体
├── dto/            # 请求 / 响应对象
├── common/         # JwtUtil、业务异常
├── config/         # JwtInterceptor、WebConfig
└── exception/      # 全局异常处理
```

**15 张表**，覆盖用户、订单、申诉、消息、通知、钱包流水、信用记录，以及一套 RBAC 权限体系（roles / permissions / menus / role_permissions / user_roles / menu_permissions）。

### 几个工程决策（为什么不选另一条路）

**① 密码加密只引 `spring-security-crypto`，不引 `spring-boot-starter-security`**

后者会把整条安全过滤链带进来 —— 项目里所有接口会**突然都要求 Basic 认证**，跟已有的 JWT 拦截器打架。只要一个 `BCryptPasswordEncoder`，引那个独立小包就够了。

**② Druid 从 1.0.9 升到 1.2.20**

课程文档指定的是 1.0.9（2016 年的版本）。它没有实现 JDBC 4.1 的 `getObject(int, Class)` 方法 —— MyBatis 读取 `LocalDateTime` 字段（`created_at` / `updated_at`）时会抛：

```
SQLFeatureNotSupportedException
```

升级到 1.2.20 解决。**这不是"换个新版本试试"，是先定位到驱动层缺哪个方法。**

**③ jjwt 锁定 0.11.5，不升 0.12**

`Jwts.parserBuilder()` 只在 0.11.x 存在，0.12 起 API 变了，升上去编译直接挂。

## 接口自动化测试（`tests/`）

订单模块的接口自动化测试，不是"调通接口"，是**带断言 + 数据校验 + 可反复执行**：

- **会话级登录夹具**：conftest 里登录一次，把 token 写进共享的 `AUTH_HEADERS`，9 条用例不用各自处理鉴权
- **基线数据保护**：库里 74 条未删除订单是硬编码基线 —— 测试不得改动这些数据
- **物理删除收尾**：用例自建的数据测完**物理删除**，而不是调接口的逻辑删除。原因：逻辑删除只是把 `deleted` 置 1，行还留在表里，**总行数就跟基线对不上了**
- **落库校验**：用例直连 MySQL 校验结果（逻辑删除后行仍在、`deleted` 置 1），不只看接口返回
- **报文附加到报告**：每次请求 / 响应都 attach 进 Allure，失败用例可直接在报告里看报文定位

```bash
cd tests
pytest                        # 需先启动后端（localhost:8088）与 MySQL
allure serve reports/allure-results
```

`conftest.py` 里写死的基线数字、管理员账号密码，都对应本机的开发环境，跑之前按需改。

## AI 智能助手（`springai_exam/`）

基于 Spring AI 的领域问答助手，重点在**调用链的组织**：

**Advisor 拦截链**（顺序由 `getOrder()` 决定，数值越小越先执行）：

| Advisor | 作用 |
|---|---|
| `SimpleLoggerAdvisor` | 请求 / 响应日志 |
| `SafeGuardAdvisor` | 敏感词拦截 —— 命中直接返回兜底语，**不请求模型** |
| `RewritePromptAdvisor` | 自定义：用 `PromptTemplate` 把用户问题改写成「先给结论、再分点解释」的结构 |

**三种调用模式**：同步 `call()`、流式 `Flux<String>`、`PromptTemplate` 双占位符。

### 踩过的坑

- 讲义示例的 `before()` **整体替换 Prompt 会丢掉 System 消息**，导致人设失效 → 改为只重写用户消息，用 `prompt.mutate().messages(...)` 保留 System
- `.system(Resource)` 单参重载内部用 `Charset.defaultCharset()`，中文 Windows 下是 GBK，会把 UTF-8 的 txt 读成乱码 → 显式指定 `StandardCharsets.UTF_8`
- 控制台日志重定向到文件时中文乱码 → `--logging.charset.console=UTF-8`

## 快速开始

```bash
# 1. 建库
mysql -uroot -p < campus.sql

# 2. 后端（需本机 MySQL + Redis，端口 8088）
cd shixun && mvn spring-boot:run

# 3. 管理端
cd campus-admin && npm install && npm run dev

# 4. 移动端（用 HBuilderX 打开 shixun_uniapp）
```

> **配置说明**：`shixun/src/main/resources/application.yml` 里的数据库 / Redis / JWT 密钥都支持环境变量覆盖（`${JWT_SECRET:...}`、`${WX_MINIAPP_APPID:...}`）。写死的默认值对应本机开发环境。

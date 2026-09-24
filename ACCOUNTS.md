# Pulverize 项目账户信息

> 本文档记录开发环境中各服务的访问地址与接入说明，**不含任何明文密码**（凭据一律存放于本机 `~/.pulverize/`），仅供开发使用。

---

## 应用服务

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端页面 | http://localhost:2020 | Vue + Vite 开发服务器（Apple 风格 UI） |
| 后端API | http://localhost:2021/api | SpringBoot 内嵌 Tomcat |
| MCP 端点 | http://localhost:2021/api/mcp | AI 客户端接入（Streamable HTTP + JSON-RPC 2.0） |
| 平台连接 | http://localhost:2020/connect | 运动 App 绑定 / 同步 / MCP 配置页 |

## MCP 接入（AI 教练）

```
# Claude Code
claude mcp add --transport http pulverize http://localhost:2021/api/mcp \
  --header "Authorization: Bearer <MCP令牌>"
```

- 令牌获取：`POST /api/mcp/token`（携带登录 JWT）→ `{token, expiresAt, expiresInDays:30}`；30 天长效 MCP 专用令牌，明文只在生成瞬间返回一次（前端自动复制到剪贴板，页面仅存掩码+有效期，不落明文）
- 认证：`Authorization: Bearer <MCP令牌>` 或 `X-RunAI-Token: <MCP令牌>`（`initialize` / `tools/list` 免认证，`tools/call` 必须携带）
- 协议：MCP 2025-06-18，Streamable HTTP 单次 POST，响应 `application/json`
- 工具（13 个；前端「AI 教练」工具区只展示中文名与说明，不暴露方法名）：

  | 中文名 | 方法名 | 说明 |
  |---|---|---|
  | 获取跑步者档案 | `get_profile` | 昵称、性别、身高体重、最大/静息心率 |
  | 获取跑步统计 | `get_weekly_stats` | 本周与全量的距离/次数/时长/配速/心率/热量 |
  | 查询跑步记录 | `list_activities` | 分页、时间倒序，含配速心率与来源平台 |
  | 获取跑步详情 | `get_activity_detail` | 完整指标 + 每公里分段 |
  | 执行跑步分析 | `analyze_activity` | 配速/心率/步频/疲劳/训练效果 + AI 摘要 |
  | 读取分析结果 | `get_analysis` | 已保存的评分、AI 摘要与各维度结论 |
  | 列出运动目标 | `list_goals` | 周跑量/最佳配速等目标及进度 |
  | 刷新目标进度 | `refresh_goals` | 按最新数据刷新并返回目标列表 |
  | 列出训练计划 | `list_plans` | 训练计划列表 |
  | 获取计划详情 | `get_plan` | 计划说明（AI 推理）与每日训练安排 |
  | 生成训练计划 | `generate_plan` | goalType: 5K_PB/10K_PB/HALF_MARATHON/MARATHON/LOSE_WEIGHT/KEEP_FIT |
  | 列出平台状态 | `list_platforms` | 各运动平台连接与同步状态 |
  | 触发平台同步 | `sync_platform` | 拉取指定平台（默认 COROS）跑步活动入库 |
- 可直接 curl 测试：

```bash
curl -X POST http://localhost:2021/api/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'
```

## AI 摘要与计划推理（任意 OpenAI 兼容服务）

- 配置入口（任一即可）：页面「设置 → AI 服务配置」（服务商预设一键填充）/ 报告页「配置 AI 服务」链接 / 环境变量 `AI_API_URL`、`AI_API_MODEL`、`AI_API_KEY`
- **服务商不写死**：DeepSeek、Kimi、智谱、通义、OpenAI 等任何 OpenAI 兼容接口均可使用
- 未配置密钥时自动降级为规则文案（分析页「AI 教练点评」、计划页「AI 推理过程」始终有内容，只是非大模型生成）
- 示例（DeepSeek）：接口 `https://api.deepseek.com/chat/completions`，模型 `deepseek-chat`

## 运动平台连接（可扩展架构）

- SPI：`module/platform/spi/PlatformConnector`（bind / sync / status / unbind），
  实现类注册为 Spring Bean 后由 `PlatformConnectorRegistry` 自动收集
- 已接入：`COROS`（高驰，`module/platform/connector/CorosConnector`）
- 占位（即将支持）：`STRAVA` / `GARMIN` / `APPLE_HEALTH` / `HUAWEI`
  （见 `AbstractUpcomingConnector`，改 `available()=true` 并实现方法即可上线）
- 通用接口：`GET /platform/list`、`POST|GET|DELETE /platform/{platform}/bind|sync|status|unbind`
- 旧接口 `/platform/coros/*` 仍保留兼容

## 测试用户账号

| 用户名 | 昵称 | 说明 |
|--------|------|------|
| 123 | admin | 本地测试账号 |
| testrunner | 跑步小将 | 本地测试账号 |

- 密码**不在仓库中记录**：已轮换为强密码，明文见本机 `~/.pulverize/accounts.local.md`
- 本机 MySQL root 密码同样只存于 `~/.pulverize/db.password`（或环境变量 `RUNAI_DB_PASSWORD`）

## 数据库

| 项目 | 值 |
|------|-----|
| 类型 | MySQL 9.7.1 |
| 主机 | localhost:3306 |
| 数据库名 | run_ai |
| 用户名 | root |
| 密码 | 不入库：`~/.pulverize/db.password`（或环境变量 `RUNAI_DB_PASSWORD`）；全新环境首次初始化默认 root |
| 字符集 | utf8mb4 (表级别) / UTF-8 (JDBC连接) |

### 连接方式

```bash
# 命令行连接（-p 后不带密码，回车后交互输入；真实密码见 ~/.pulverize/db.password）
mysql -uroot -p run_ai

# 查看所有表
SHOW TABLES;
```

### 数据库表清单

| 表名 | 说明 |
|------|------|
| t_user | 用户信息表 |
| t_user_platform_auth | 第三方运动平台授权表 |
| t_running_activity | 跑步活动主表 |
| t_running_lap | 跑步分段数据表 |
| t_running_track_point | GPS轨迹点表 |
| t_running_analysis | 跑步分析结果表 |
| t_training_plan | 训练计划表 |
| t_training_plan_detail | 训练计划每日详情表 |
| t_user_goal | 用户运动目标表 |

## 中间件 (开发环境状态)

| 中间件 | 端口 | 状态 | 说明 |
|--------|------|------|------|
| MySQL | 3306 | 运行中 | 已配置，数据库已初始化 |
| Redis | 6379 | 未安装 | 已做可选处理，不影响启动 |
| RabbitMQ | 5672 | 未安装 | 已做可选处理，不影响启动 |
| AliyunOSS | - | 未启用 | dev环境跳过 |

## 常用命令

```bash
# 启动后端 (打包后运行)
mvn package -DskipTests
java -jar target/pulverize-1.0-SNAPSHOT.jar --spring.profiles.active=dev

# 启动后端 (开发模式，热加载)
mvn spring-boot:run

# 启动前端
cd frontend
npm install
npm run dev

# 重新建库建表（-p 回车后交互输入密码）
mysql -uroot -p run_ai < sql/init.sql
```

## 项目结构概览

```
pulverize/
├── src/main/java/com/run/          # 后端 Java 代码
│   ├── RunAiApplication.java       # 启动类
│   ├── common/                     # 公共模块 (配置/异常/工具)
│   └── module/                     # 业务模块
│       ├── user/                   # 用户模块
│       ├── running/                # 跑步记录
│       ├── analysis/               # 跑步分析
│       ├── plan/                   # 训练计划
│       ├── goal/                   # 运动目标
│       ├── platform/               # 运动平台连接 (SPI + COROS 连接器)
│       └── mcp/                    # MCP Server (AI 教练接入)
├── src/main/resources/
│   ├── application.yml             # 主配置
│   └── application-dev.yml         # 开发环境配置
├── sql/init.sql                    # 建库建表SQL
├── frontend/                       # 前端 Vue 项目
│   └── src/
│       ├── assets/styles/          # Apple 风格设计系统 (theme.scss)
│       ├── api/                    # API 请求封装
│       ├── views/                  # 页面组件（含 Connect 连接页）
│       ├── store/                  # Pinia 状态管理
│       └── router/                 # 路由配置
├── pom.xml                         # Maven 依赖
└── ACCOUNTS.md                     # 本文档
```

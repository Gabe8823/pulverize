# Pulverize · 个人运动数据分析与 AI 教练

![](https://img.shields.io/github/v/release/Gabe8823/pulverize)

把高驰（COROS）等运动平台的跑步数据聚合到本地，生成**数据看板、跑步分析（VDOT/心率区间/分段）、训练计划、运动目标、周期报告**，并由 **AI 教练**给出复盘与建议；同时通过 **MCP 协议**把你的数据安全开放给 Claude、Cursor 等 AI 助手。

- 全中文、Apple（apple.com.cn）风格界面，支持暗黑模式，红黑白主题
- 桌面客户端（Windows exe）：内嵌前端、自动拉起后端、可自定义背景
- 数据只存**你自己的 MySQL**，AI 密钥只存服务端（界面仅回显掩码）

---

## 功能特性

| 模块 | 说明 |
|---|---|
| 数据看板 | 本周跑量/时长/配速/心率一屏掌握，趋势图与计划进度 |
| 跑步记录 | 活动列表（卡片/列表视图切换）+ 详情：每公里分段、心率区间分布 |
| 跑步分析 | 配速/心率/步频/疲劳/训练效果多维评分 + **VDOT**（按全部历史数据取最优） |
| 训练计划 | AI 生成周计划（5K/10K/半马/全马/减脂/保持），可**一键推送到手表** |
| 运动目标 | 周跑量、最佳配速等目标；**成绩式 PB 目标** + AI 进度建议 |
| 跑步报告 | 周期复盘；配置 AI 密钥后由规则摘要自动切换为 **AI 生成** |
| 连接 | COROS 绑定/同步（多平台 SPI 架构，Strava/Garmin/Apple/华为为可扩展占位）、**MCP 接入** |
| AI 服务接入 | 任意 OpenAI 兼容接口（DeepSeek 示例）：接口地址 / 模型 / 密钥，一键测试连接 |
| 桌面客户端 | 自定义标题栏 + 原生窗口按钮、4 档背景预设 + 自定义图片、快捷键、GPU 加速 |

---

## 技术选型

| 层 | 选型 | 理由 |
|---|---|---|
| 后端 | **Spring Boot 3.2 + JDK 21 + MyBatis-Plus + MySQL 8** | 成熟稳定的 CRUD 生态；启动时自动 DDL 补表，开发零门槛 |
| AI 接入 | 自研 `AiClient`（OpenAI 兼容）+ `t_ai_config` 单表配置 | 配置存库、热生效；`apiKey` 留空=不修改，回传仅掩码，不落前端明文 |
| MCP | **自实现 JSON-RPC 2.0 over Streamable HTTP**（`POST /api/mcp`） | 零重依赖、完全可控；15 个工具只暴露中文名，令牌 30 天长效、明文仅生成瞬间返回一次 |
| 前端 | **Vue 3 + TypeScript + Vite + Pinia + ECharts（按需）** | 组件按需懒加载（echarts 独立 chunk，gzip≈180kB）；**移除 Element Plus**，自研 `.a-*` 设计系统还原 Apple 风格 |
| 主题 | 纯 CSS 变量（`--a-*`）+ `data-theme` 暗黑切换 + 自定义背景强制调色板 | 深色背景强制浅字/浅色背景强制深字，可读性与主题开关解耦 |
| 桌面客户端 | **Electron 44 + electron-builder（NSIS）** | **UI 就是这套 Web 界面**，Electron 直接复用且可加原生能力；本地静态服务 + `/api` 同源代理 + 后端进程编排（空闲自动 `java -jar`，已运行则复用） |
| 为什么不 JavaFX / C++ 重写 | — | 客户端视觉层就是 Vue 页面，用 JavaFX/Qt 重写等于丢弃现有全部 UI 重画一遍（工期长、美观与可读性倒退）；卡顿类问题用 GPU 开关与壳层优化解决即可。未来若追求更小体积，可平滑迁移 **Tauri**（WebView2 壳，网页零改动） |

### 系统架构

```
┌───────────────┐    ┌──────────────────────────────────────────┐
│  浏览器 :2020 │───▶│ Vite dev server（/api 代理 → :2021）      │
└───────────────┘    └──────────────────────────────────────────┘
┌───────────────┐    ┌──────────────────────────────────────────┐
│ Pulverize.exe     │───▶│ 本地静态服务 :5219（内嵌 frontend/dist）   │
│（Electron）   │    │   /api/* → 同源代理 127.0.0.1:2021        │
└───────────────┘    │ 后端未运行时自动 java -jar（等待页轮询）    │
                     └───────────────────┬──────────────────────┘
                                         ▼
                     ┌──────────────────────────────────────────┐
                     │ Spring Boot :2021（context-path /api）    │
                     │  ├─ 业务模块（活动/分析/计划/目标/报告）    │
                     │  ├─ 平台 SPI（COROS 主实现 + 占位扩展）    │
                     │  ├─ AiClient（OpenAI 兼容，配置存库）      │
                     │  └─ MCP 端点 POST /api/mcp（JSON-RPC 2.0）│
                     └───────────┬──────────────┬───────────────┘
                                 ▼              ▼
                          MySQL run_ai    DeepSeek 等大模型
```

---

## 快速开始（使用手册）

### 环境要求

- **JDK 21+**、**Maven 3.9+**、**Node.js 18+**、**MySQL 8.x**

### 1. 初始化数据库

```bash
mysql -uroot -p < sql/init.sql     # 创建 run_ai 库（表结构也会在启动时自动补齐/升级）
```

数据源默认 `localhost:3306/run_ai`（root/[REDACTED]，见 `pulverize-service/src/main/resources/application-dev.yml` 按需修改）。Redis/RabbitMQ/OSS 为可选中间件，开发环境已自动排除，不安装也能启动。

### 2. 启动后端（端口 2021）

```bash
mvn -q -DskipTests package                                    # package parent + common/api/service
java -jar pulverize-service/target/pulverize-1.0-SNAPSHOT.jar # run (:2021)
```

### 3. 启动前端（端口 2020）

```bash
cd frontend
npm install
npm run dev        # 打开 http://localhost:2020
```

### 4. 注册 / 登录

首次使用在登录页完成注册即可（JWT 2 小时 + 刷新令牌 7 天）。

### 5. 配置 AI 密钥（启用 AI 报告 / 计划推理 / 目标建议）

入口（三处任选，均指向同一表单）：

1. **个人中心 → AI 服务接入**（完整表单）
2. **跑步报告** →「配置 DeepSeek Key」链接（无密钥时显示）
3. **连接** →「配置 AI 服务密钥」链接

填写示例（任意 OpenAI 兼容接口均可）：

| 字段 | 示例值 |
|---|---|
| 接口地址 | `https://api.deepseek.com/chat/completions` |
| 模型名称 | `deepseek-chat` |
| API 密钥 | `sk-xxxx`（从服务商控制台获取） |

「测试连接」会用当前填写值发起一次真实调用；「保存配置」后立即生效。密钥仅存服务端，界面只显示掩码（`sk-t****abcd`），**留空保存表示不修改**。

### 6. 接入 MCP（让 Claude / Cursor 直接读你的训练数据）

1. 打开 **连接 → AI 教练接入（MCP）**，点击生成访问令牌（明文仅出现一次，自动复制）
2. 添加 MCP 客户端：

```bash
claude mcp add --transport http pulverize http://localhost:2021/api/mcp \
  --header "Authorization: Bearer <MCP令牌>"
```

- 端点：`POST http://localhost:2021/api/mcp`（Streamable HTTP，JSON-RPC 2.0，MCP 2025-06-18）
- 认证：`Authorization: Bearer <令牌>` 或 `X-RunAI-Token: <令牌>`；`initialize`/`tools/list` 免认证
- 15 个工具全部以**中文名**展示在页面工具区（获取跑步者档案、跑步统计、跑步详情、VDOT、生成训练计划、推送相关查询……）

### 7. 桌面客户端（Windows）

1. 前往 **[Releases](../../releases)** 下载 `Pulverize Setup x.y.z.exe`，双击安装（生成桌面快捷方式）
2. 运行要求：本机安装 **Java 21+**（客户端在 2021 端口无后端时会自动 `java -jar` 拉起内置后端；若开发时后端已在 2021 端口运行则直接复用）
3. 客户端特性：
   - 自定义标题栏（拖拽移动窗口 + 原生红黑白窗口按钮）
   - **背景切换**：默认 / 红黑 / 墨石灰 / 云白 / 自定义图片（本地压缩存储，深色背景自动强制浅色文字保证可读）
   - 快捷键：`F12` 开发者工具、`Ctrl+R` 刷新、`Ctrl +/−/0` 缩放
   - 强制 GPU 硬件加速（`ignore-gpu-blocklist` 等），避免软件渲染卡顿

### 端口约定

| 服务 | 端口 |
|---|---|
| 前端（Vite dev） | **2020** |
| 后端 API / MCP（context-path `/api`） | **2021** |
| 桌面客户端本地静态服务 | 5219 |
| MySQL | 3306 |

---

## 目录结构

```
pulverize/
├─ pulverize-service/src/main/java/com/run/        # Spring Boot 后端
│  ├─ common/ai/                 # AiClient / AiConfigProvider（依赖倒置）
│  ├─ common/math/               # VDOT 计算等纯算法
│  └─ module/                    # activity/analysis/plan/goal/report/ai/mcp/platform...
├─ pulverize-service/src/main/resources/           # application.yml（:2021）+ application-dev.yml
├─ sql/init.sql                  # 数据库初始化
├─ frontend/                     # Vue 3 + TS + Vite
│  └─ src/
│     ├─ components/a*、ui/       # 自研 Apple 风格设计系统（无 Element Plus）
│     ├─ components/client/      # 客户端专属标题栏（preload 门控）
│     ├─ utils/clientTheme.ts    # 客户端背景系统
│     └─ views/                  # 看板/记录/分析/计划/目标/报告/连接/个人中心
├─ client/                       # Electron 桌面客户端
│  ├─ main.js                    # 本地服务 + /api 代理 + 后端编排 + GPU/菜单/快捷键
│  ├─ preload.js                 # 仅暴露 window.runai.isClient
│  └─ backend/pulverize-*.jar       # 打包内置后端（随 extraResources 带入）
└─ ACCOUNTS.md                   # 本地开发地址与 MCP 速查（仅开发用）
```

## 开发与构建命令

```bash
# 后端
java -jar pulverize-service/target/pulverize-1.0-SNAPSHOT.jar   # run (:2021)
mvn -q -DskipTests package                  # package (parent + common/api/service)

# 前端
cd frontend
npm run dev                                  # 开发（:2020）
npx vue-tsc -b && npx vite build            # 类型检查 + 产物 frontend/dist

# 桌面客户端
cd client
cp ../pulverize-service/target/pulverize-1.0-SNAPSHOT.jar backend/   # 更新内置后端（后端有改动时）
npm run dist                                 # 产出 release/Pulverize Setup x.y.z.exe
```

国内网络打包 Electron 时可走镜像：`ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/`、`ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/`。

---

## 后续规划

- 平台 SPI 继续接入 Strava / Garmin / Apple 健身 / 华为运动健康
- 客户端可选迁移 Tauri（更小体积，网页零改动）
- 报告对比（周期间）、训练负荷曲线

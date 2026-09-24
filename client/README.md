# Pulverize 桌面客户端（Electron）

红黑白主题的 Windows 桌面客户端：打包后的前端静态资源 + 本地 `/api` 同源代理 + 后端自动拉起。

## 架构

```
┌─────────────── Pulverize.exe（Electron 窗口）───────────────┐
│  本地静态服务 127.0.0.1:5219                            │
│    ├─ /            → 内嵌 frontend/dist（SPA）          │
│    └─ /api/*       → 代理 127.0.0.1:2021（同源，无 CORS）│
│  后端编排：2021 未监听时自动 `java -jar` 拉起            │
│    └─ 启动等待页 loading.html（轮询 /__health）          │
└─────────────────────────────────────────────────────────┘
```

- 前端 `axios baseURL = '/api'`（相对路径）→ 客户端零改动。
- 2021 已被占用（例如开发时后端 jar 已在运行）→ 直接复用，不会重复启动。
- 后端由客户端拉起时，工作目录为 `client/backend/`（jar 所在目录）。

## 目录

```
client/
├─ main.js           # Electron 主进程：本地服务 + 代理 + 后端编排
├─ preload.js        # 预加载桥：仅暴露 window.runai.isClient 标识
├─ loading.html      # 后端启动等待页（红黑白主题，自包含无外部依赖）
├─ build_icon.ps1    # 生成 build/icon.png（PowerShell System.Drawing）
├─ build/icon.png    # 应用图标（红底白 R，256×256）
├─ backend/          # 后端 jar 放这里（打包时随 extraResources 带入）
│  └─ pulverize-1.0-SNAPSHOT.jar
└─ release/          # 打包产物（git 不入库）
```

## 开发运行

```powershell
# 1) 构建前端（client 引用 ../frontend/dist）
cd frontend
npm run build        # 或 npx vue-tsc -b; npx vite build

# 2) 启动客户端窗口（后端可不手动起，会自动拉起 backend/ 下的 jar）
cd ..\client
npm start            # electron .
```

## 打包 exe

```powershell
cd client
npm run icon         # 可选：重新生成图标
npm run dist         # electron-builder --win --x64
# 产物：release\Pulverize Setup 1.0.0.exe（NSIS 一键安装，装完生成桌面快捷方式）
```

国内网络建议走镜像（本机已配置 npmmirror registry 时二进制也走镜像）：

```powershell
$env:ELECTRON_MIRROR='https://npmmirror.com/mirrors/electron/'
$env:ELECTRON_BUILDER_BINARIES_MIRROR='https://npmmirror.com/mirrors/electron-builder-binaries/'
npm run dist
```

## 更新后端 jar（改动了 Java 代码后）

```powershell
mvn -q -DskipTests package
Copy-Item pulverize-service\target\pulverize-1.0-SNAPSHOT.jar client\backend\ -Force
# 再 npm run dist 重新打包；开发期 npm start 直接生效
```

## 运行环境要求（目标机器）

- Windows 10/11 x64
- Java 21+（`java -version` 可用；没有 Java 时客户端会在等待页给出明确提示）
- MySQL 8（库 `run_ai`，配置在 jar 内 application.yml，默认 root/[REDACTED]@localhost）
- 首次安装包未签名，SmartScreen 提示「更多信息 → 仍要运行」属正常现象

## 端口

| 用途 | 端口 |
| --- | --- |
| 客户端本地静态服务（仅 127.0.0.1） | 5219（占用时自动 +1，最多试 5 个） |
| 后端 API（context-path `/api`） | 2021 |

## 调试

- 主进程日志：`npm start` 的控制台前缀 `[runai]` / `[backend]`
- 页面侧：窗口内 F12 不可用时，可用 `ELECTRON_ENABLE_LOGGING=1` 启动查看

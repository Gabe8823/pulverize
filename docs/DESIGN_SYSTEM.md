# Pulverize 设计系统

> 依据：`frontend/src/assets/styles/theme.scss`（唯一真源，本文件为其说明文档）。
> 风格参照 apple.com.cn：克制、留白、内容优先；红黑白品牌色；全站中文。
> 已移除 Element Plus——全部组件为自研 `.a-*` 类 + CSS 变量，无第三方 UI 依赖。

---

## 1. 设计原则

1. **内容优先**：数据与图表是主角，装饰不抢戏；一屏一个视觉重心。
2. **克制的动态**：动效只用于「状态解释」（进入、切换、反馈），单次 ≤ 300ms，禁止无限循环装饰动画。
3. **红黑白**：品牌强调色恒为红；大面积只用黑/白/灰，红色仅出现在可交互焦点与关键数值。
4. **双主题对等**：亮/暗不是两套设计，而是同一套 token 的两组取值；任何组件只允许消费变量，不得写死颜色。
5. **可读性兜底**：自定义桌面背景（红黑/墨石/图片）会强制切换文字色系（见 §8），保证任意背景下正文对比度达标。

---

## 2. 色彩 Token

定义位置：`theme.scss` `:root`（亮）与 `html[data-theme='dark']`（暗）。

| Token | 亮色 | 暗色 | 用途 |
|---|---|---|---|
| `--a-blue` | `#e30613` | `#ff3b30` | **品牌强调（红）**：主按钮、链接、focus、选中态（变量名含 blue 为历史保留） |
| `--a-blue-hover` | `#c70511` | `#ff5147` | 强调色 hover |
| `--a-blue-active` | `#ab0410` | `#d92f26` | 强调色按压 |
| `--a-blue-ring` | `rgba(227,6,19,.18)` | `rgba(255,59,48,.32)` | focus 光环 / 选中底色 |
| `--a-text` | `#1d1d1f` | `#f5f5f7` | 正文 |
| `--a-text-secondary` | `#6e6e73` | `#aeaeb2` | 次级文字 |
| `--a-text-tertiary` | `#86868b` | `#98989d` | 辅助/占位 |
| `--a-bg` | `#f5f5f7` | `#000000` | 页面底 |
| `--a-surface` | `#ffffff` | `#1d1d1f` | 卡片/弹窗底 |
| `--a-surface-2` | `#ffffff` | `#2c2c2e` | 次级面（按钮默认底） |
| `--a-surface-active` | `#ffffff` | `#48484a` | 分段控件选中底 |
| `--a-fill` / `-hover` / `-strong` | `#f2f2f4` / `#ececee` / `#e4e4e8` | 白色 9%/14%/18% | 填充灰三级 |
| `--a-input-bg` | `#ffffff` | `#1d1d1f` | 输入框底 |
| `--a-border-input` | `rgba(0,0,0,.14)` | `rgba(255,255,255,.2)` | 输入框描边 |
| `--a-divider` / `-strong` | 黑 8% / 16% | 白 12% / 26% | 分隔线 |
| `--a-green` / `--a-orange` / `--a-red` | `#34c759` / `#ff9500` / `#ff3b30` | 同左 | 语义色：成功/警示/危险 |

规则：
- 新组件必须引用上表变量；确需新色先加 token 再使用。
- 语义色底（chip 等）一律 16% 透明度同色系，文字用加深/提亮变体。

---

## 3. 字体与排版

- 字体栈 `--a-font`：`Inter → -apple-system → SF Pro → PingFang SC → Microsoft YaHei → …`
- 全局：`font-variant-numeric: tabular-nums`（数字等宽，表格/统计对齐）、`text-rendering: optimizeLegibility`、抗锯齿开启。
- 层级（均为 clamp 流式，桌面→移动自适应）：

| 类 | 尺寸 | 字重/行高/字距 |
|---|---|---|
| `.display` | `clamp(40px,5.6vw,64px)` | 700 / 1.06 / -0.022em |
| `.headline` | `clamp(28px,3.6vw,40px)` | 700 / 1.12 / -0.016em |
| `.section-title` | `clamp(24px,2.6vw,32px)` | 700 / 1.15 / -0.018em |
| `.subhead` | `clamp(17px,1.6vw,21px)` | 400 / 1.45，次级色 |
| `.stat-value` | `clamp(34px,3.6vw,48px)` | 700 / tabular |
| 正文 | 15px | 400 / 1.47 |
| 辅助 `.caption` | 13px | 三级色 |

- 英文/数字字距收紧（-0.01em ～ -0.022em），中文正文不加字距。

---

## 4. 形状、阴影、间距

| Token/类 | 值 | 用途 |
|---|---|---|
| `--a-radius-card` | 20px | 卡片 |
| 按钮 | `980px`（胶囊） | 所有 `.a-btn` |
| 输入框 | 14px | `.a-input` / `.a-select__trigger` |
| 弹窗 | 24px，遮罩 blur(8px) | `.a-modal` |
| 菜单 | 16px + `0 16px 48px` 阴影 | 下拉/浮层 |
| `--a-shadow-card` | `0 1px 2px + 0 8px 24px` | 静息卡片 |
| `--a-shadow-card-hover` | 加深 + `0 16px 40px` | hover 卡片 |

间距刻度：8 / 12 / 16 / 20 / 24 / 32 / 48 / 64 / 76（页面头上下留白 76px）。容器 `max-width: 1280px`，横向 padding 24px。

---

## 5. 组件规范（`.a-*` 自研）

| 组件 | 要点 |
|---|---|
| `.a-btn` | 高 44（lg 52 / sm 34）胶囊；变体 primary / danger / ghost / ghost-danger / block；按压 `scale(.98)`；`is-loading` 前置旋转指示且 `pointer-events:none`；primary hover 上浮 1px + 红色光环 |
| `.a-input` | 高 52、16px 字；focus：红描边 + `0 0 0 4px --a-blue-ring`；禁用 55% 透明 |
| `.a-select` | 自研：trigger 同输入框规范，菜单 6px 内衬圆角 11 选项，选中红字+对勾，支持「自定义…」内联输入 |
| `.a-switch` | 52×32 胶囊，开=绿，滑块 220ms |
| `.a-choice` | 选中 = 红描边 + 4px 光环（单选卡片组） |
| `.a-seg` | 分段控件：灰槽 + 白/深浮起项 |
| `.a-card` | 毛玻璃 `rgba(255,255,255,.64) + blur(20px)`，暗色 62% 深灰；`hoverable` 上浮 4px |
| `.a-table` | 表头 13px 次级色，行 hover 灰底 + 首列变红（可点暗示） |
| `.a-modal` | 遮罩 blur、弹体 `scale(.96)+8px → 归位` 240ms |
| `.a-toast` | 顶部居中胶囊、毛玻璃、-12px 下落进入 |
| `.chip` | 胶囊标签，语义色 16% 底 |
| `.a-empty` / `.a-skeleton` | 空态圆标 + 文案；骨架屏为 `.a-skeleton` shimmer 条（1.4s 往返） |
| `.a-code-block` | MCP 配置示例：黑底等宽 |

---

## 6. 动效规范

| 场景 | 时长/曲线 |
|---|---|
| 微交互（hover/按压/色彩） | 0.14–0.22s `ease` |
| 弹窗/浮层 | 0.24–0.26s `cubic-bezier(.2,.8,.2,1)` |
| 页面切换 `.page-fade` | 0.22s ease，新页 `+8px 上浮`，离场页 `pointer-events:none` |
| 区块入场 `.a-rise` | 0.5s `cubic-bezier(.22,1,.36,1)`，`.section` 依次 60ms 错峰 |
| 数据条/计数变化 | 0.6s `cubic-bezier(.2,.7,.3,1)` |
| 骨架 shimmer | 1.4s ease 往返 |
| 按钮 loading 自旋 | 0.7s linear 无限（功能性允许无限） |

- **唯一允许的无限循环**：loading 指示类。
- 错误反馈：表单校验失败触发 `.a-shake`（0.36s，水平 6px 两段位移），与 toast 二选一或并用。
- 登录/注册页允许 **aurora 背景**（两枚低透明径向渐变缓移，18–24s 循环）作为唯一氛围动效，`prefers-reduced-motion` 下静止。
- 无障碍：`@media (prefers-reduced-motion: reduce)` 必须关停入场/切换/aurora 动效（保留必要 opacity 即时态）。

---

## 7. 热力图色阶（肌肉负荷）

- 插值色阶 **蓝 → 黄 → 红**（冷=低负荷，热=高负荷），全站唯一热力口径：
  - `0–45 分`：蓝 `#4a9eff` → 黄 `#ffd60a`（线性 RGB 插值）
  - `45–100 分`：黄 `#ffd60a` → 红 `#ff3b30`
  - `0 分 / 无数据`：中性灰 `rgba(150,150,158,.45)`
- 必配图例（高/中/轻/未激活四档），hover 显示「名称 · 分数（等级）」tooltip，点击肌群打开明细面板（走 `GET /analysis/muscle-map/{key}/detail`）。
- 分数与等级文案由后端 `levelText` 下发，前端不得自行改写等级阈值。

---

## 8. 暗黑模式与自定义背景

- 暗黑由 `html[data-theme='dark']` 切换，`index.html` 预挂载脚本应用防闪烁；`color-scheme: dark` 同步原生控件。
- 桌面客户端可选页面背景 `html[data-bg]`（red-black / graphite / cloud / image），**强制调色板**：
  - 深色背景 → 无视用户主题，强制暗色文字体系；
  - 云白背景 → 强制亮色文字体系；
  - 与手动主题开关解耦，永远保证文字可读。
- 深色底上卡片自动加 1px 白 8% 描边、滚动条提亮。

---

## 9. 可访问性与文案

- 所有可交互元素具备 `:focus-visible` 红色 2px outline（offset 2px）。
- 尊重 `prefers-reduced-motion`（§6）。
- 正文对比度：亮色 ≥ `#1d1d1f on #ffffff`，暗色 ≥ `#f5f5f7 on #1d1d1f`。
- 文案：全中文、第二人称「你」、短句、动词开头按钮（登录/保存/生成）；不出现内部实现词（服务名、库名、密钥字段等）。
- 演示环境标识：登录/注册页脚固定「本地演示环境 · 数据仅保存在你的机器上」，不出现任何真实账号或密码。

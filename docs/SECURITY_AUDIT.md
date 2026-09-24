# Pulverize 数据安全审计与处置报告

> 版本：1.0 ｜ 审计日期：2026-09-24 ｜ 范围：工作区、`git log --all --full-history`（全量历史）、已推送远程分支/Tag/Release 资产、GitHub Actions / Secret Scanning
> 状态标记：✅ 全部完成（轮换 → 历史清除 → force-push → Release 资产重建，见附录 A）

## 一、泄露清单

| ID | 严重度 | 位置 | 内容 | 扩散范围 | 处置 |
|---|---|---|---|---|---|
| F1 | 🔴 高 | `application.yml` → `jwt.secret` | 硬编码 JWT 签名密钥 | 全部历史提交 + 已推送 + 打进 v1.0.0 / v1.1.0 两个公开安装包 | ✅ 轮换为 `RUNAI_JWT_SECRET` 环境变量 / `~/.pulverize/jwt.secret` 本地文件；yml 仅留占位默认值；✅ 历史已以占位值覆盖清除；✅ 两个安装包已用净化后源码重建替换（见附录 A） |
| F2 | 🟠 中 | `ACCOUNTS.md`（公开入库） | 测试账号明文密码 ×2、MySQL `root/[REDACTED]` | 全历史 + 已推送 | ✅ 文档去密码改指本地 keystore；测试账号密码已 BCrypt 重置；✅ 历史文本已清除（见附录 A） |
| F3 | 🟠 中低 | `application-dev.yml` | 本机 MySQL `root/[REDACTED]`（RabbitMQ guest、OSS/AI 为占位符，不计） | 全历史 + 已推送 + 打进安装包 | ✅ 改 `${RUNAI_DB_PASSWORD:root}` + 本地文件；本机 root 密码已轮换；✅ 历史行已清除（见附录 A） |
| F4 | 🟠 中 | GitHub Release 附件 | 两个安装包内嵌 F1+F3 | 公网可下载 | ✅ 已从净化后 Tag 重建并替换资产（size 校验一致，保留 Tag 语义，见附录 A） |
| F5 | 🟡 低（仅本地，从未入库） | 仓库根 | `runai_jwt.txt` 明文残留、`stdout/stderr.log` | `.gitignore` 覆盖 | ✅ JWT 残留已删除；日志保留（本地调试用，永不入库） |

## 二、干净项（未发现）

- ✅ 无 COROS 账号/密码/token/refresh_token 硬编码（凭据仅走请求体 → `t_user_platform_auth` 库表）
- ✅ 无 Strava/Garmin/Nike OAuth client secret、无 `sk-` 真实密钥、无 `LTAI` OSS Key、无 `ghp_`、无 JWT 实值、无私钥文件
- ✅ 无 `.fit/.gpx/.tcx` 个人数据、无 `.env`、无 `pem/p12/jks/keystore`；`sql/init.sql` 纯 DDL 零 INSERT
- ✅ git 全历史仅 5 个 revision，从未提交过 jar/node_modules/日志/数据文件（密钥形状扫描仅命中 DDL 列名）
- ✅ 远程仅 `main` + 2 Tag；Actions 0 runs / 0 secrets（无 CI 日志面）；GitHub Secret Scanning 0 告警；Code Scanning 未启用（404）

## 三、轮换处置结果（本机）

| 项 | 结果 | 验证 |
|---|---|---|
| JWT 签名密钥 | 新 96 字符随机值，写入 `~/.pulverize/jwt.secret` + 用户级 `RUNAI_JWT_SECRET`（两处同值） | ✅ 登录签发/校验正常（token 191 字符），MCP 令牌签发正常 |
| MySQL root 密码 | 新 32 位随机值，写入 `~/.pulverize/db.password` + 用户级 `RUNAI_DB_PASSWORD`；`root@localhost` 已 `ALTER USER` | ✅ 旧密码拒绝（登录 code=500）；后端经本地文件认证启动成功（vdot/muscle-map 200） |
| 测试账号 `123` | BCrypt 重置为 16 位随机密码 | ✅ 新密码登录 200 / 旧密码拒绝 |
| 测试账号 `testrunner` | BCrypt 重置（该行处于逻辑删/不存在状态，登录不可用属预期） | ✅ UPDATE 已执行 |
| MCP 令牌 | 旧令牌随密钥轮换全部失效 | ✅ 重新签发验证通过 |
| 凭据落点 | `~/.pulverize/{jwt.secret, db.password, accounts.local.md}`（仓库之外，永不入库） | ✅ 存在性与长度校验通过 |
| 本机 CLI 连接方式 | `mysql -uroot -p` 交互输入（以本地文件为准） | 见 ACCOUNTS.md「连接方式」 |

**注意**：本机 MySQL root 改密后，**其他配置了旧 root 密码的项目（如 IDEA 中的 hmall 数据源）需同步更新为 `~/.pulverize/db.password` 中的新密码**。

## 四、密钥装载机制（改造后）

优先级：**环境变量 > 本机密钥文件 > yml 占位默认值**

- 实现：`common/config/LocalSecretsEnvironmentPostProcessor`（注册于 `META-INF/spring.factories`，order = `HIGHEST_PRECEDENCE + 20`）
- `jwt.secret` ← `RUNAI_JWT_SECRET`，否则 `~/.pulverize/jwt.secret`（缺失自动生成随机值落盘）
- `spring.datasource.password` ← `RUNAI_DB_PASSWORD`，否则 `~/.pulverize/db.password`（缺失沿用 yml 默认，保证全新环境可初始化）
- OSS / AI 密钥：`${ALIYUN_OSS_*:占位}`、`${AI_API_KEY:sk-placeholder}`（真实值走页面配置或环境变量）

## 五、防护体系

1. `.gitignore` 增补：`.env*`、`*.local.yml`、`.pulverize/`、`*.secret`、`*.pem/*.p12/*.jks/*.keystore`、`*.fit/*.gpx/*.tcx`、`*.dump/*.sql.gz`、token 临时文件
2. **gitleaks pre-commit**：二进制装于 `%LOCALAPPDATA%\Programs\gitleaks`；`scripts/git-hooks/pre-commit` 对 staged 变更做密钥扫描；启用方式 `git config core.hooksPath scripts/git-hooks`（已对本仓库启用）
3. GitHub 侧：Secret Scanning 已启用（当前 0 告警）；Code Scanning 未启用（可选后续）
4. 纪律：敏感值不进代码/提交/日志；AI 密钥仅存 `t_ai_config` 库表（接口掩码返回）

## 六、历史清除与发布（✅ 已完成）

- [x] `git-filter-repo` 三段定向替换：`application.yml` 密钥行、`application-dev.yml` 密码行、`ACCOUNTS.md`/登录注册页密码文本（外加方括号 marker 规范化，见附录 A）
- [x] `git push --force` 覆盖远程 `main` + 重指 `v1.0.0` / `v1.1.0`（SSH :22 + Deploy Key）
- [x] Release 资产：`target_commitish=main`（分支名引用无需 PATCH）；从净化后 Tag 重建两个安装包并替换（size 校验一致）
- [x] 旧 commit SHA 在 GitHub 站内变为不可达对象（平台 GC 回收；如需立即清除可向 GitHub Support 提交清除请求）

## 七、审计原始结论快照

- 历史 revision 数：5（`d95f1b1` 及之前）；全历史密钥形状扫描命中：1（`init.sql` DDL 列名，非密钥）
- 远程分支：`main`；Tag：`v1.0.0`、`v1.1.0`；Release 资产：`RunAI.Setup.1.0.0.exe`(147.8MB)、`Pulverize.Setup.1.1.0.exe`(148.6MB)
- Actions：0 runs / 0 secrets；Secret Scanning 告警：0

## 附录 A：历史清除与 Release 重建执行记录（P0 收尾）

> 执行日期：2026-09-24 ｜ 工具：git-filter-repo（a40bce548d2c）｜ 三遍规则、双 Tag 构建验收、全量复核通过

### A.1 清史规则（全局规则 + 精确字面值）

| 遍次 | 规则（示意） | 目的 |
|---|---|---|
| Pass A | 8 条：1 个历史 JWT 签名值、5 个已轮换密码 token、`root/root`、`regex:password:\s*root` | 凭据字面值清除 |
| Pass B | 1 条：`secret: <REDACTED>` → `secret: ${RUNAI_JWT_SECRET:…}` | Tag 时代 `application.yml` 对齐占位形态（jjwt 0.12.5 要求 HMAC ≥32 字节，10 字节标记值会导致安装包登录抛 `WeakKeyException`） |
| Pass C | 1 条：`<REDACTED>` → `[REDACTED]` | 方括号规范化：尖括号形式会被 Vue 模板当作元素解析（Login/Register 演示账号行编译失败），在 Markdown 中也会渲染为隐形标签 |

> 说明：Pass C 期间一次 marker 规范化提交与其父提交内容收敛，被 filter-repo 折叠（最终树内容等价：SECURITY `root/[REDACTED]`×2 落位、`<REDACTED>` 归零），以逐行 diff + 磁盘复核为准。

### A.2 验证口径（全绿）

- pickaxe `git log --all -S…`：历史 JWT、5 个密码 token、`root/root`、`password: root`、`<REDACTED>` = 全 0
- `application.yml` 形态审计：全历史 4 个版本 `secret:` 行 100% 为 `${RUNAI_JWT_SECRET:…}` 占位（violations=0）
- 全 ref `git grep -F '<REDACTED>'`（heads/tags/remotes）= 0；**HEAD 树逐字节不变**（tree_same=True）
- gitleaks `--redact` 全历史 = 0；pre-commit 常态拦截（每次提交均绿）
- 双 Tag 前端构建验收（`vue-tsc -b && vite build`）：v1.1.0 / v1.0.0 均通过后才允许推送
- 本地旧代对象：`reflog expire` + `gc --prune=now` + `ORIG_HEAD` 清理 → 旧关键提交全部不可达、`fsck` 干净

### A.3 推送与同步（最终 SHA）

- 通道：GitHub :443（SNI 间歇阻断）→ **SSH :22 + Deploy Key**（id `164279852`，读写已验证）；REST 走 `api.github.com`
- 清史 force-push：`main` → **`c4945d2`**（forced；此后新提交会继续前进，以远端 `refs/heads/main` 实时值为准）；tag 为最终稳定值、不再变动：`v1.0.0` → **`bdbfc16`**（deref `bd01c0a`）、`v1.1.0` → **`80eaa20`**
- 原仓库 fetch/reset/tags 同步一致（local == remote），工作区 clean；旧代 commit 在远端变为不可达对象，由平台 GC 回收（无法经 API 主动加速；历史克隆残留需持有者自行清理）

### A.4 Release 资产重建（F4 闭环）

| 资产 | 字节数 | 构建链路 | 替换 |
|---|---|---|---|
| `Pulverize.Setup.1.1.0.exe` | 155,828,418 | Tag `v1.1.0`（`80eaa20`）worktree：`mvn package`（jar 内嵌 yml 占位审计 ✓）→ `vue-tsc`+`vite build` → `electron-builder --win --x64` | 删旧 id 585098973 → 上传 → size match=True |
| `RunAI.Setup.1.0.0.exe` | 154,976,202 | Tag `v1.0.0`（`bd01c0a`）同链路 | 删旧 id 585063397 → 上传 → size match=True |

- `v1.0.0` 资产名保留 `RunAI.Setup.1.0.0.exe`：改名前的历史发布（Tag 内 `productName=RunAI`），与第七节清单一致；当前产品品牌全量 Pulverize。
- 两个 Release 的 `target_commitish=main`（分支名引用，随远端自动解析），无需 PATCH。
- 客户端 `main.js` 以目录扫描定位后端 jar（`*.jar` 取首个），安装包内 jar 文件名不影响启动。

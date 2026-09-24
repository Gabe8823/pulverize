# Pulverize 数据安全审计与处置报告

> 版本：1.0 ｜ 审计日期：2026-09-24 ｜ 范围：工作区、`git log --all --full-history`（全量历史）、已推送远程分支/Tag/Release 资产、GitHub Actions / Secret Scanning
> 状态标记：✅ 已完成 ｜ ⏳ 发布阶段执行（历史清除 → force-push → 重建 Release）

## 一、泄露清单

| ID | 严重度 | 位置 | 内容 | 扩散范围 | 处置 |
|---|---|---|---|---|---|
| F1 | 🔴 高 | `application.yml` → `jwt.secret` | 硬编码 JWT 签名密钥 | 全部历史提交 + 已推送 + 打进 v1.0.0 / v1.1.0 两个公开安装包 | ✅ 轮换为 `RUNAI_JWT_SECRET` 环境变量 / `~/.pulverize/jwt.secret` 本地文件；yml 仅留占位默认值 ⏳ 历史中以占位值覆盖清除 ⏳ 两个安装包用净化后源码重建替换 |
| F2 | 🟠 中 | `ACCOUNTS.md`（公开入库） | 测试账号明文密码 ×2、MySQL `root/[REDACTED]` | 全历史 + 已推送 | ✅ 文档去密码改指本地 keystore；测试账号密码已 BCrypt 重置；⏳ 历史文本清除 |
| F3 | 🟠 中低 | `application-dev.yml` | 本机 MySQL `root/[REDACTED]`（RabbitMQ guest、OSS/AI 为占位符，不计） | 全历史 + 已推送 + 打进安装包 | ✅ 改 `${RUNAI_DB_PASSWORD:root}` + 本地文件；本机 root 密码已轮换；⏳ 历史行清除 |
| F4 | 🟠 中 | GitHub Release 附件 | 两个安装包内嵌 F1+F3 | 公网可下载 | ⏳ 用净化后的历史 Tag 重建安装包并替换资产（保留 Tag 语义） |
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

**注意**：本机 MySQL root 改密后，**其他使用 `root/[REDACTED]` 的项目（如 IDEA 中的 hmall 数据源）需同步更新为 `~/.pulverize/db.password` 中的新密码**。

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

## 六、历史清除与发布（⏳ 发布阶段执行）

- [ ] `git-filter-repo` 三段定向替换：`application.yml` 密钥行、`application-dev.yml` 密码行、`ACCOUNTS.md`/登录注册页密码文本
- [ ] `git push --force` 覆盖远程 `main` + 重指 `v1.0.0` / `v1.1.0`
- [ ] Release `target_commitish` 更新 + 从净化后 Tag 重建两个安装包、替换资产
- [ ] 旧 commit SHA 在 GitHub 站内变为不可达对象（平台 GC 回收；如需立即清除可向 GitHub Support 提交清除请求）

## 七、审计原始结论快照

- 历史 revision 数：5（`d95f1b1` 及之前）；全历史密钥形状扫描命中：1（`init.sql` DDL 列名，非密钥）
- 远程分支：`main`；Tag：`v1.0.0`、`v1.1.0`；Release 资产：`RunAI.Setup.1.0.0.exe`(147.8MB)、`Pulverize.Setup.1.1.0.exe`(148.6MB)
- Actions：0 runs / 0 secrets；Secret Scanning 告警：0

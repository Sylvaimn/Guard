# TemporeData Guard · 完整回归与 CI 执行指引

> 问题：本会话沙箱（TRAE）会定时 SIGTERM 回收后台 Java 进程（15 轮峰值 37/44），需在**无回收的普通终端**完成最终 44 项验收。
> 本文给两条可执行路径。

---

## 路径 A：本机普通终端一键自检（推荐，5 分钟完成）

> 在 **Terminal / iTerm**（非 TRAE 会话、非本 AI 沙箱）执行：

```bash
cd "/Users/kodlien/Documents/金盾智云警用大数据中台系统"

# 一键: 预检 → (构建) → 启动主应用 → 完整回归 44 → 结果
bash scripts/verify-all.sh
# 已构建过可加速: bash scripts/verify-all.sh --no-build
# 服务已在跑只想回归: bash scripts/verify-all.sh --only-reg
```

预期输出：`回归结果: 44 / 44` ✅

**为何普通终端可行**：verify-all.sh 中的 java 由你的终端会话托管，无 TRAE 回收；脚本内置等待健康与全流程，单命令完成。

手动等价步骤（若需分步）：
```bash
# 1. 启动主应用(需要 MySQL: docker start zy-mysql)
nohup env SPRING_PROFILES_ACTIVE=prod DB_HOST=localhost DB_PORT=3306 DB_NAME=temporedata \
  DB_USERNAME=root DB_PASSWORD=zy@2026 NACOS_ENABLED=false EVENT_BUS_TYPE=kafka \
  KAFKA_SERVERS=localhost:9094 SERVER_PORT=8080 \
  java -Xmx512m -Xms256m -jar temporedata-guard-main/target/tdg.jar \
  --app.lineage.enabled=true --app.audit.listener.enabled=true \
  --lakehouse.warehouse="$(pwd)/data/lake-iceberg" > /tmp/verify-main.log 2>&1 &
# 2. 等日志出现 "Started ZyApplication"(约1-3分钟)
# 3. 跑回归
bash scripts/police-combat-test.sh        # 预期 44 项
```

---

## 路径 B：GitHub Actions（CI 已内置）

`.github/workflows/ci.yml` 已包含三 job（backend 构建 / frontend 构建 / E2E regression 完整 44 项 + MySQL service）。

本地 git 仓库**尚无 remote**，接入步骤：

```bash
cd "/Users/kodlien/Documents/金盾智云警用大数据中台系统"

# 1. GitHub 新建私有仓库(如 temporedata-guard), 然后:
git remote add origin git@github.com:<你的账号>/temporedata-guard.git
git push -u origin main
# 2. Actions 页自动触发: backend → frontend → regression(44项, 无回收限制)
```

> 注意：当前 git 根是 `/Users/kodlien/Documents`（父目录），push 会包含父目录其他内容。
> 若需仓库独立，请在 `temporedata-guard` 工程目录内 `git init` 新仓库后推送。

本地等效（无 GitHub 时）：
```bash
# 安装 act(GitHub Actions 本地执行器, 需 Docker):
brew install act
act -j regression
```

---

## 验收判定

| 结果 | 含义 |
|---|---|
| `44 / 44` | TemporeData Guard 迁移后完整回归通过 → 验收完成 |
| `<44` 且 [FAIL] 为超时/无响应 | 进程被回收 → 换路径 A 普通终端执行 |
| `<44` 且 [FAIL] 为断言失败 | 记录失败项反馈（迁移未引入逻辑改动，正常应为 0 断言失败） |

迁移正确性现有证据（此前已归档 docs/迁移后服务启动验证记录.md）：双构建全绿 · 6 服务启动 · 回归段 1-3 多轮全过 · 峰值 37/44（中断均 SIGTERM 无断言失败）。

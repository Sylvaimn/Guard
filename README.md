# TemporeData Guard

> 警务数据智能中台 · 时间数据守卫（数据 + 时间维度研判 + 安全防护）
> 前身：金盾智云警用大数据中台（qingzhouyun / zy-*）——详见「新旧对照」

TemporeData Guard 是面向警务数据场景的一体化智能中台：**数据汇聚 → 关系研判 → 专案协同 → AI 辅助 → 安全管控**。提供管理员（治理）与警务用户（业务）双工作台，支持数据源插件化、湖仓（Iceberg 时间旅行）、CDC 实时采集、跨层血缘、受控数据查询与字段脱敏、Agent 权限继承等能力。

## 技术架构

```
UI(temporedata-guard-ui, Vue3) ──/api──▶ 主应用 tdg-server(:8080) ── 拆分服务 tdg-service-{platform|security|data|police|agent|dev}
            │                              │
            └── MySQL(主从) · Kafka · Nacos · ClickHouse · Redis · Iceberg 湖仓 · Prometheus/Grafana
```

- Java 11 · Spring Boot 2.7 · Maven 多模块（父 POM groupId=com.temporedata）
- Java 主包：`com.temporedata.guard.*`；模块命名：`temporedata-guard-{component}`（见 docs/包到域映射表）
- 运行三模式：日常研发（前端热更新 :5174 + 后端 :8080）/ 单包演示（tdg.jar）/ 生产（nginx + 双节点），详见 docs/技术方案设计文档

## 快速开始

```bash
# 1. 构建
cd temporedata-guard-ui && npm run build        # 前端 → 主应用 static
mvn -T 1C clean install -DskipTests             # 后端全模块

# 2. 启动主应用(依赖 MySQL :3306, 湖仓目录 data/lake-iceberg)
./scripts/temporedata-guard.sh start            # :8080 等待健康
./scripts/temporedata-guard.sh status           # 端口/进程/健康一览
# 日常研发(前端免打包): cd temporedata-guard-ui && npm run dev  → http://localhost:5174

# 3. 验收
bash scripts/police-combat-test.sh              # 44 项端到端回归(段分片: REG_START=n 续跑)
```

演示账号：`admin/admin123`（管理员工作台）· `police/police123`（用户工作台）

## 文档索引（docs/）

- 架构与模块：包到域映射表 · 后端与基础设施服务详细设计 · 前端界面设计与功能设计（当前基线）
- 运行与部署：技术方案设计（v2.2 三模式）· 系统运行方案 · 警用内网环境生产部署方案
- 重构设计：项目代码目录重构设计（TemporeData Guard）
- 业务设计：数据研发/数据安全/数据服务/系统管理模块警务化重构设计

## 新旧对照（qingzhouyun → TemporeData Guard）

| 旧 | 新 |
|---|---|
| com.isxcode.zy.* | com.temporedata.guard.* |
| zy-backend/zy-main | temporedata-guard-main（jar: tdg.jar） |
| zy-backend/zy-service-{d} | temporedata-guard-service-{d}（jar: tdg-service-{d}.jar） |
| zy-frontend | temporedata-guard-ui（views 按 admin/user/common/agent 归类） |
| scripts/qingzhouyun.sh | scripts/temporedata-guard.sh（旧名软链兼容一期） |
| 数据库名/表前缀 zy_* | 库名已更名 temporedata；表前缀 zy_* 一期保留 |
| HTTP /api/* | 不变（前后端契约无感） |

## License

Apache License 2.0（LICENSE / NOTICE 见仓库根）

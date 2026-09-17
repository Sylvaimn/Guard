# TemporeData Guard · 项目代码目录重构设计文档

> 版本：v2.0（详细版）
> 更名：金盾智云警用大数据中台（qingzhouyun / zy-*）→ **TemporeData Guard**
> 工程标准：借鉴 **Apache 项目工程组织规范**（LICENSE/NOTICE/README/CI、`{project}-{module}` 模块命名、src 标准布局、父 POM 聚合、发布物规范）——**不采用 org.apache.* 包名**
> 代码坐标：`groupId=com.temporedata` · Java 主包 **`com.temporedata.guard.*`**
> 原则：HTTP API 不变、数据库名/表前缀 zy_* 一期不变、业务数据零迁移、git 分支可回退

---

## 一、设计总则

| 维度 | 设计 |
|---|---|
| 项目名 | TemporeData Guard（时间数据守卫） |
| 仓库名 | `temporedata-guard` |
| groupId | `com.temporedata` |
| Java 主包 | `com.temporedata.guard.*`（**不含 apache**） |
| 模块命名 | `temporedata-guard-{component}` |
| 前端模块 | `temporedata-guard-ui`（Vue3 SPA） |
| 后端技术 | Java 11 · Spring Boot 2.7 · Maven 多模块（15 模块） |
| 工程治理 | LICENSE(Apache-2.0)/NOTICE/README/.github CI/RAT 可选 |
| 风险控制 | 两阶段执行（先后端后前端），每步构建+回归验证，全程 git 分支 |

---

## 二、顶层工程布局（详细）

```text
temporedata-guard/                            ← 仓库根
├── LICENSE                                  Apache License 2.0
├── NOTICE                                   第三方版权声明
├── README.md                                项目简介/架构图/快速开始/文档索引/贡献指南
├── pom.xml                                  父 POM（见三）
├── .github/
│   ├── workflows/ci.yml                     main 推送: mvn verify + npm build + 回归脚本
│   └── CODEOWNERS / ISSUE_TEMPLATE/         可选
├── docs/
│   ├── architecture/                        架构/运行/部署/内网方案文档
│   ├── frontend/                            前端功能/设计文档
│   ├── backend/                             后端落地/模块文档
│   └── sql/                                 建表与初始化脚本集中目录
├── conf/
│   ├── application-prod.yml.example         生产配置模板(去敏)
│   ├── .env.example                         统一运行参数模板
│   └── logback-spring.xml.example
├── sql/
│   ├── 001_schema.sql                       （由后端 db/ 抽取编排）
│   └── 002_demo_data.sql                    演示数据
├── scripts/
│   ├── temporedata-guard.sh                 （由 qingzhouyun.sh 更名，start/stop/status/logs/dev）
│   ├── police-combat-test.sh → guard-test.sh  回归(44 项, 脚本内提示兼容)
│   ├── mysql-dr.sh / data-govern.sh         （容灾/治理, 保留原名或前缀统一）
│   └── init-db.sh                           初始化库/导入 sql
├── temporedata-guard-common/
├── temporedata-guard-api/
├── temporedata-guard-security/
├── temporedata-guard-modules/
├── temporedata-guard-server/
├── temporedata-guard-gateway/
├── temporedata-guard-service-platform/
├── temporedata-guard-service-security/
├── temporedata-guard-service-data/
├── temporedata-guard-service-police/
├── temporedata-guard-service-agent/
├── temporedata-guard-service-dev/
└── temporedata-guard-ui/
```

---

## 三、父 POM 与 Maven 模块（详细）

### 3.1 父 POM 关键内容
```xml
<project>
  <groupId>com.temporedata</groupId>
  <artifactId>temporedata-guard</artifactId>
  <version>2.2.0</version>            <!-- 或用 ${revision} 占位 -->
  <packaging>pom</packaging>
  <name>TemporeData Guard</name>
  <description>警务数据智能中台（数据+时间维度研判+安全防护）</description>
  <licenses><license>Apache-2.0</license></licenses>
  <modules>
    <module>temporedata-guard-common</module>
    <module>temporedata-guard-api</module>
    <module>temporedata-guard-security</module>
    <module>temporedata-guard-modules</module>
    <module>temporedata-guard-server</module>
    <module>temporedata-guard-gateway</module>
    <module>temporedata-guard-service-platform</module>
    ...（6 service）
  </modules>
  <properties>
    <maven.compiler.source>11</maven.compiler.source>
    <spring-boot.version>2.7.18</spring-boot.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
</project>
```

### 3.2 模块清单与职责
| 原模块（工程根/） | 新模块 | artifactId | 说明 |
|---|---|---|---|
| zy-common + zy-api-base | temporedata-guard-common | guard-common | 异常/存储抽象/事件总线/湖仓/资源/文件/工具 + base(BaseResponse/异常基类) |
| zy-api | temporedata-guard-api | guard-api | 各域 Req/Res/枚举 |
| zy-security | temporedata-guard-security | guard-security | JWT/LoginUser/SecurityConfig |
| zy-modules | temporedata-guard-modules | guard-modules | 业务域实现 |
| zy-main | temporedata-guard-server | guard-server | 主应用(前端内嵌/Quartz/监听), finalName=tdg-server |
| zy-gateway | temporedata-guard-gateway | guard-gateway | 网关 |
| zy-service-platform | temporedata-guard-service-platform | guard-service-platform | 拆分服务 |
| …（6 个 service） | … | … | finalName=tdg-service-{domain} |

### 3.3 依赖方向（单向）
```text
guard-api ──▶ guard-common ◀── guard-security
     ▲            ▲                 ▲
     └──── guard-modules ◀──────────┘
               ▲
       ┌───────┼────────┐
   guard-server  guard-gateway   guard-service-{domain}×6
```

### 3.4 模块内部 src 布局（示例：guard-modules）
```text
temporedata-guard-modules/
├── pom.xml
└── src/main/java/com/temporedata/guard/modules/...
└── src/main/resources/            （模块内 mapper/规则等）
└── src/test/java/...              🆕 单测骨架
```

---

## 四、Java 包命名设计（详细树）

### 4.1 包迁移映射（不含 apache）
| 原包 | 新包 |
|---|---|
| com.temporedata.guard.common | com.temporedata.guard.common |
| com.temporedata.guard.api | com.temporedata.guard.api |
| com.temporedata.guard.api.base | com.temporedata.guard.common.base |
| com.temporedata.guard.security | com.temporedata.guard.security |
| com.temporedata.guard.modules | com.temporedata.guard.modules |
| com.temporedata.guard.main / serviceplatform 等 | com.temporedata.guard.server / .service.{domain} |

### 4.2 modules 完整子包（7 业务域 + usercenter + common）
```text
com.temporedata.guard.modules
├── platform            tenant / user / org / role / auth
├── security            security(audit/behavior/density) / datalevel / sensitive /
│                       blacklist / apilog / changeaudit / permapproval / message
├── police              intelligence / entitymaster / entitygraph / watch / track / photo / network
├── agent               agent(chat/gateway/analysis) / aianalysis
├── data                datasource(plugin/…) / service(dataapi) / report / form
├── dev                 workflow / meta / lineage / quality / ingestion / sync / engine /
│                       indicator / tag / datacenter / dashboard / query / real / func /
│                       globalvar / dependency / catalog
├── usercenter          controller / service（工作台/受控查询/专案/成果/申请/权限接口）
└── common              eventbus / lakehouse / resource / file / storage / async
```
> 每个子包再按 controller / service / repository / entity 细分（沿用现状分层）。

---

## 五、主应用与拆分服务（server/service-*）设计

### 5.1 主应用 temporedata-guard-server
```text
src/main/java/com/temporedata/guard/server/
├── ServerApplication.java            （原 ZyApplication，@EnableKafka 等）
└── config/                           （调度/迁移等启动配置）
src/main/resources/
├── application.yml / application-prod.yml
├── logback-spring.xml
└── static/                           （前端构建产物输出目标, 由 UI 构建脚本写入）
```
- 启动类名：`TemporedataGuardServerApplication`（可保留 class 名变更仅一处）
- jar finalName：`tdg-server` → tdg-server.jar

### 5.2 拆分服务模板（service-{platform|security|data|police|agent|dev}）
```text
src/main/java/com/temporedata/guard/service/{domain}/Guard{Domain}ServiceApplication.java
```
- scanBasePackages 全部由 `com.temporedata.guard` → `com.temporedata.guard`（精确扫描列表同步替换）
- jar finalName：tdg-service-{domain}

---

## 六、前端目录重设计（详细）

### 6.1 顶层
```text
temporedata-guard-ui/
├── package.json / vite.config.js / index.html
├── README.md                          UI 开发说明(模式一热更新 :5174)
├── public/                            （favicon/logo——"金盾智云"标识迁移为品牌目录）
└── src/
```

### 6.2 src 详细目录
```text
src/
├── main.js / App.vue
├── api/                      # 按域
│   ├── auth.js               # 原 modules/auth.js
│   ├── user.js               # 用户端 P0-P2(usercenter)
│   ├── admin/                # 原 datasource/workflow/lakehouse/quality/lineage/meta/engine/…
│   │   ├── datasource.js  workflow.js  lakehouse.js  engine.js  quality.js  …
│   ├── intelligence.js       # entitymaster/graph/watch
│   └── agent.js
├── components/
│   ├── common/               # SearchTable/PageHeader/StatusTag/EmptyState/Breadcrumb/AssistantDrawer
│   ├── workflow/             # WorkflowDagEditor/DagNode/NodeConfigPanel
│   ├── lineage/              # LineageGraph(+lineage 子件)
│   ├── security/             # PermissionButton/DataMask/SecurityTag（预留)
│   └── index.js              # 统一导出(可选)
├── composables/              # useCRUD/useTable/useDialog/useTableExt
├── config/                   # menu.config.js(双工作台菜单)
├── layouts/                  # AdminLayout.vue(原 Layout)/UserLayout.vue
├── router/                   # index.js(三段路由 admin/user/兼容)
├── stores/                   # auth.js/permission.js
├── styles/
│   ├── tokens.css            # 🆕 品牌/设计 token 集中(主色/圆角/阴影)
│   ├── global.css            # 全局
│   └── element-override.css  # 🆕 EP 覆盖统一
├── utils/                    # dsLabels/request/mask/permission 指令注册
└── views/                    # 按域子目录(替代 84 平铺)
    ├── admin/                # dashboard/resource/development/ops/governance/asset/service/system/security
    ├── user/                 # workbench/intel/project/data/service/result/mine
    ├── agent/                # Agent 全屏(原 Agent.vue)
    ├── common/               # Login/Profile/Message/Error/WorkbenchFallback
    └── legacy/               # 🆕 兼容占位(旧平铺文件名转发注释, 一个发布周期后删除)
```

### 6.3 views 归类映射（节选）
| 原文件 | 新位置 |
|---|---|
| Datasource.vue / Driver.vue / Container.vue / Cluster.vue / ResourceCenter.vue | views/admin/resource/ |
| Workflow.vue / Lakehouse.vue / Realtime.vue / Engine.vue / Ingestion.vue / Approval.vue | views/admin/development/ |
| Quality.vue / Lineage.vue / Meta.vue / Indicator.vue / Tag.vue / DataAsset.vue | views/admin/governance/ + views/admin/asset/ |
| AuditLog.vue / DataLevel.vue / Sensitive.vue / PermApproval.vue / Blacklist.vue | views/admin/security/ |
| DataApi.vue / ApiLog.vue / Report.vue / Form.vue | views/admin/service/ |
| Tenant/Role/Org/Settings/Notify/Passwordless/License | views/admin/system/ |
| UserWorkbench.vue | views/user/workbench/ |
| UserProject*.vue ×8 | views/user/project/ |
| DataQuery.vue / UserQueryHistory.vue / UserDataDetail.vue | views/user/data/ |
| UserEntityProfile.vue / UserClueCenter.vue / Intelligence.vue | views/user/intel/ |
| UserReport*/UserGenerateTasks.vue | views/user/result/ |
| UserApply/UserFavorite/UserMyOps/Message/Profile | views/user/mine/ |
| Agent.vue | views/agent/Agent.vue |

---

## 七、配置文件与运行标识（联动更名）

| 项 | 现值 | 新值 |
|---|---|---|
| 主应用 jar | tdg.jar | tdg-server.jar |
| 服务 jar | tdg-service-{d}.jar | tdg-service-{d}.jar |
| 启动脚本 | scripts/temporedata-guard.sh | scripts/temporedata-guard.sh（旧名软链兼容一期） |
| Nacos 服务名 | qingzhouyun / zy-service-* | guard-server / guard-service-{domain} |
| Kafka topic | zy-task/zy-alert/zy-lineage/zy-cdc/zy-quality | tdg-*（消费组与位点同步迁移） |
| 应用配置扫描包 | com.temporedata.guard.* | com.temporedata.guard.* |
| 前端标题 | 金盾智云大数据平台 | 配置化(admin settings 已支持 siteName 覆盖) |
| 数据库名/表前缀 | qingzhouyun / zy_* | 库名已随更名改 **temporedata**（外部执行）；表前缀 **zy_\* 一期保留**；二期评估 tdg_\* |
| 湖仓/日志目录 | data/lake-iceberg / logs | 不变（相对工程根） |

---

## 八、执行步骤（详细清单，供排期）

### 阶段 0：准备
1. git 新建分支 `refactor/temporedata-guard`
2. 全量备份（或确认可回退 commit）
3. 补 LICENSE/NOTICE/README.md/.github 模板

### 阶段 1：后端目录与模块更名（每步构建）
1. `工程根/` 下模块目录 `git mv` → `temporedata-guard-*`
2. 父 POM 移到仓库根（若保留 工程根 层则仅改 artifactId——**推荐保留原目录层级或提升，按团队部署习惯二选一**，本文推荐提升到仓库根并删除中间层）
3. 每个模块 pom：groupId/artifactId/依赖引用/finalName 更新
4. 全局文本替换（版本可控，勿全局 sed 风险）：
   - `com.temporedata.guard` → `com.temporedata.guard`
   - `com.temporedata.guard.main.ZyApplication` → `com.temporedata.guard.server.TemporedataGuardServerApplication`
   - scanBasePackages / @EntityScan / @EnableJpaRepositories 列表
   - logback、application*.yml 内 logger 名可选
5. `mvn -T 1C clean install -DskipTests` → 主应用启动（java -jar tdg-server.jar）→ 回归 44 项

### 阶段 2：前端目录归类
1. `temporedata-guard-ui` → `temporedata-guard-ui`（package.json name 同步）
2. views/components/api 按第 6 章归类（git mv 保历史）
3. 批量修正 import（相对路径 + @ 别名不受影响）
4. `npm run build` 全绿 → 主应用内嵌产物冒烟（或 dev :5174 联调）

### 阶段 3：收尾
1. scripts/conf/sql 更名与模板化；.env.example 生成
2. docs 文档链接与新旧名对照说明更新
3. 启动脚本验证（start/status）与回归报告归档

---

## 九、Apache 工程治理补充（不含 apache 包名，仅工程规范）

- LICENSE：Apache License 2.0 文本；NOTICE：Element Plus/Vue/Spring Boot/MyBatis 等第三方声明
- README：项目定位/架构 ASCII 图/三模式运行/文档索引/账号说明(演示)
- CI：`mvn verify`（后端）+ `npm run build`（前端）+ `scripts/guard-test.sh`（回归）
- 代码头注释：统一 `Copyright (c) 2026 TemporeData Guard`（可选全量执行）
- 版本规范：主版本.功能.修复（当前 2.2.0 对齐业务版本）

---

## 十、验收标准（设计稿通过条件）

- [ ] 顶层含 LICENSE/NOTICE/README/父 pom/CI/docs/sql/conf/scripts
- [ ] 全部模块名 `temporedata-guard-{component}`、jar 名 tdg-*
- [ ] Java 包 `com.temporedata.guard.*`（无 apache），import 修正无残留（grep com.isxcode=0）
- [ ] 前端 `temporedata-guard-ui`，views 按 admin/user/agent/common 域子目录，import 全绿
- [ ] `mvn clean install` 与 `npm run build` 全绿；tdg-server.jar :8080 启动 + 回归 44 项
- [ ] HTTP API 路径不变；数据库名/表前缀 zy_* 一期不变；业务数据零迁移
- [ ] 旧脚本/旧链接兼容说明（README 新旧对照表）

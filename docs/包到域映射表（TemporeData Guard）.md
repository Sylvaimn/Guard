# TemporeData Guard · 包→域映射表（temporedata-guard-modules：70 包 → 7 域 + usercenter）

> 基线：包迁移后（com.temporedata.guard.modules.*，2026-09；原 zy-modules）
> 主包：org 域包 com.temporedata.guard.modules；每个叶子包内含 controller/service/repository/entity 分层
> 承载：主应用全包扫描；6 拆分服务按域精确 scanBasePackages（下表"服务覆盖"列）

| 域 | 子包数 | 承载服务 scan |
|---|---|---|
| platform | 11 | platform 服务(8) + security 服务 |
| security | 12 | security 服务(20) + dev 服务(security.changeaudit) |
| police | 7 | police 服务(10) + agent 服务(police 部分) |
| agent | 2 | agent 服务 |
| data | 4 | data 服务(7) + agent/dev 服务(datasource) |
| dev | 17 | dev 服务(22) |
| common | 15 | 各服务 common.eventbus/storage/async；lakehouse 仅 dev+主应用 |
| usercenter | 2 | 主应用全包（工作台/受控查询/专案/成果/申请 P0-P2） |

## platform（11 包）

- com.temporedata.guard.modules.platform.auth
- com.temporedata.guard.modules.platform.license
- com.temporedata.guard.modules.platform.notify
- com.temporedata.guard.modules.platform.org
- com.temporedata.guard.modules.platform.passwordless
- com.temporedata.guard.modules.platform.preference
- com.temporedata.guard.modules.platform.role
- com.temporedata.guard.modules.platform.secret
- com.temporedata.guard.modules.platform.settings
- com.temporedata.guard.modules.platform.tenant
- com.temporedata.guard.modules.platform.user

## security（12 包）

- com.temporedata.guard.modules.security.alarm
- com.temporedata.guard.modules.security.apilog
- com.temporedata.guard.modules.security.approval
- com.temporedata.guard.modules.security.behaviorlog
- com.temporedata.guard.modules.security.blacklist
- com.temporedata.guard.modules.security.changeaudit
- com.temporedata.guard.modules.security.loginlog
- com.temporedata.guard.modules.security.message
- com.temporedata.guard.modules.security.mydata
- com.temporedata.guard.modules.security.permapproval
- com.temporedata.guard.modules.security.security
- com.temporedata.guard.modules.security.sensitive

## police（7 包）

- com.temporedata.guard.modules.police.entitygraph
- com.temporedata.guard.modules.police.entitymaster
- com.temporedata.guard.modules.police.intelligence
- com.temporedata.guard.modules.police.network
- com.temporedata.guard.modules.police.photo
- com.temporedata.guard.modules.police.track
- com.temporedata.guard.modules.police.watch

## agent（2 包）

- com.temporedata.guard.modules.agent.agent
- com.temporedata.guard.modules.agent.aianalysis

## data（4 包）

- com.temporedata.guard.modules.data.datasource
- com.temporedata.guard.modules.data.form
- com.temporedata.guard.modules.data.report
- com.temporedata.guard.modules.data.service

## dev（17 包）

- com.temporedata.guard.modules.dev.catalog
- com.temporedata.guard.modules.dev.dashboard
- com.temporedata.guard.modules.dev.datacenter
- com.temporedata.guard.modules.dev.dependency
- com.temporedata.guard.modules.dev.engine
- com.temporedata.guard.modules.dev.func
- com.temporedata.guard.modules.dev.globalvar
- com.temporedata.guard.modules.dev.indicator
- com.temporedata.guard.modules.dev.ingestion
- com.temporedata.guard.modules.dev.lineage
- com.temporedata.guard.modules.dev.meta
- com.temporedata.guard.modules.dev.quality
- com.temporedata.guard.modules.dev.query
- com.temporedata.guard.modules.dev.real
- com.temporedata.guard.modules.dev.sync
- com.temporedata.guard.modules.dev.tag
- com.temporedata.guard.modules.dev.workflow

## common（15 包）

- com.temporedata.guard.modules.common.ai
- com.temporedata.guard.modules.common.async
- com.temporedata.guard.modules.common.bootstrap
- com.temporedata.guard.modules.common.cluster
- com.temporedata.guard.modules.common.container
- com.temporedata.guard.modules.common.driver
- com.temporedata.guard.modules.common.eventbus
- com.temporedata.guard.modules.common.file
- com.temporedata.guard.modules.common.ha
- com.temporedata.guard.modules.common.lakehouse
- com.temporedata.guard.modules.common.monitor
- com.temporedata.guard.modules.common.resource
- com.temporedata.guard.modules.common.storage
- com.temporedata.guard.modules.common.view
- com.temporedata.guard.modules.common.work

## usercenter（2 包）

- com.temporedata.guard.modules.usercenter.controller
- com.temporedata.guard.modules.usercenter.service

---
## 服务 scanBasePackages 完备性核查（已确认 ✅）
| 服务 | 覆盖 | 说明 |
|---|---|---|
| platform(:8091) | platform 全 + common.eventbus/storage/async | 平台域职责 |
| security(:8090) | security 全 + platform 核心 + common | 安全域全量 |
| data(:8092) | data 全 + common | 服务/报表/表单/数据源 |
| police(:8093) | police 全 + common | 警务域全量 |
| agent(:8094) | agent + police(情报/图谱/布控/轨迹/网络) + dev.query + data.datasource + common | 工具数据依赖 |
| dev(:8095) | dev 全 + security.changeaudit + common.lakehouse/eventbus/storage/async | 研发域+湖仓 |
| 主应用(:8080) | com.temporedata.guard.modules（全包） | 全量能力+usercenter |

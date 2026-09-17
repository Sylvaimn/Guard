# Agent 模型服务注册与路由 · 重构设计文档（取代"模型部署与版本管理"）

> 版本：v1.0（设计稿）
> 背景：Agent 设置「系统全局 → 模型部署与版本管理」经真实场景评估——"部署/版本"语义失真（平台内手填清单无后端控制），安全治理/配额/约束真实有用。
> 重构方向：**平台不做模型部署与伪版本管理**，改为真实的「模型服务注册（推理端点）+ 场景路由 + 安全分级 + 探活治理」。

---

## 一、现状与问题（评估结论）

| 现状 | 问题 |
|---|---|
| 模型清单表（name/id/**version**/type/状态）纯前端手填 | 无后端实体/控制，"版本""下架"不真实生效 |
| "模型部署"语义 | 拉起/运维推理服务是**独立推理平台/运维职责**，非中台功能 |
| 类型标签（警务微调/通用/多模态…） | 无意义的分类，不驱动任何逻辑 |
| 资源配额/温度长度约束/开关锁定 | ✅ 真实有用（保留） |
| 大模型配置（推理参数接入） | ✅ 真实（保留并纳入模型服务清单） |
| 后端 AiModelGateway 场景路由（small/large/sql）+ 密级守卫 | ✅ 真实骨架（复用/扩展） |

---

## 二、重构目标

平台以"**注册消费方**"视角治理模型服务（不负责部署模型本身）：
1. **模型服务注册**：每个服务 = 真实推理端点（名称/类型/分级/baseUrl/model/key 加密/状态/健康）
2. **连通与健康**：逐服务「测试连通」+ 健康状态（可用/不可用）
3. **场景路由**：通用/快速/长文本/NL2SQL/研判报告 等场景 → 绑定默认服务（沿用 AiModelGateway small/large/sql 扩展）
4. **角色/用户级默认模型**：多级策略（系统→角色→用户→会话）
5. **本地/外发分级**：L1-L4 密级约束——涉密仅本地私有化模型（内网 vLLM/Ollama），外发需审批通道（复用 AiDensityGuard）
6. **配额与安全约束**：保留现状（每日调用/token、温度/长度范围、RAG/工具锁定）

---

## 三、数据模型

### 3.1 模型服务注册表 `zy_ai_model_service`（新增）
| 列 | 类型 | 说明 |
|---|---|---|
| id | varchar(32) | 主键 |
| name | varchar(64) | 服务显示名（如"内网 Qwen2.5-72B"） |
| category | varchar(32) | 用途类目：LOCAL_COMPLIANT（本地合规）/EXTERNAL_APPROVED（外发审批）/LIGHT（轻量快） |
| base_url | varchar(256) | 推理端点（OpenAI 兼容） |
| api_key_enc | varchar(256) | key 加密存储（CryptoUtil） |
| model | varchar(128) | 模型名 |
| temperature | double | 默认温度 |
| max_tokens | int | 默认最大输出 |
| enabled | tinyint(1) | 启用/停用（真实控制路由） |
| health_status | varchar(16) | UNKNOWN/ONLINE/OFFLINE（探活结果） |
| health_time | datetime | 最近探活时间 |
| tenant_id | varchar(32) | 租户 |
| create/update_time | datetime | - |

### 3.2 模型策略 `zy_ai_model_policy`（改造现有 ModelPolicyEntity）
```json
{
  "defaultModel": "service-id-A",              // 系统默认
  "sceneModels": { "chat": "A", "fast": "B", "long": "C", "nl2sql": "A", "report": "A" },
  "roleDefaults": { "POLICE_USER": "B" },      // 角色级（已有雏形）
  "quota": { "dailyCalls": 500, "dailyTokens": 1000000 },
  "limits": { "temperature": {"min":0,"max":1}, "maxTokens": {"max":4096} },
  "locks": { "rag": true, "toolCalls": true }
}
```

---

## 四、后端设计

### 4.1 端点（扩展 AgentController 或独立 ModelServiceController）
| 端点 | 说明 |
|---|---|
| GET /api/agent/models | 模型服务清单（含 health） |
| POST /api/agent/models | 注册服务（name/category/baseUrl/apiKey/model…，key 加密） |
| PUT /api/agent/models/{id} | 更新（含启用/停用） |
| DELETE /api/agent/models/{id} | 删除（被策略引用时禁止） |
| POST /api/agent/models/{id}/test | 逐服务连通测试（复用 AiModelGateway.callLlm 空消息探测）→ 更新 health_status |
| POST /api/agent/models/health-check | 批量探活（调度/手动，更新 ONLINE/OFFLINE） |
| GET/POST /api/agent/model-policy | 场景/角色路由 + 配额约束（改造现有） |
| GET /api/agent/models/available | 供用户端对话选择（仅 enabled + 按分级过滤） |

### 4.2 路由执行链（AgentService.chat / AnalysisAgent 总结）
```text
会话/角色/用户默认模型(多级策略) → 场景映射(sceneModels) → 目标服务
   ↓ 密级守卫(AiDensityGuard)：数据级 > 服务分级 时拦截（涉密仅 LOCAL_COMPLIANT）
   ↓ AiModelGateway.chat(service.baseUrl, key, model)
   ↓ 失败 → 降级：同分级其他服务 → 明确报错（不静默伪应答）
```
- AgentConfig（原单配置）迁移语义：作为"默认推理服务"自动注册到 zy_ai_model_service（兼容迁移，见六）

### 4.3 分级守卫
- service.category=LOCAL_COMPLIANT：可处理 L1-L4（本地私有化）
- EXTERNAL_APPROVED：仅 ≤ INTERNAL（复用现有 canSendExternal 逻辑）
- 前端可用模型按用户密级过滤

---

## 五、前端设计（Agent.vue 系统全局）

### 5.1 UI 结构（重构后）
```text
系统全局(管理员)
├── 模型服务管理（原"模型部署与版本管理"）
│   ├── 服务清单表格:
│   │   名称(类别徽标: 本地合规/外发审批/轻量) / 推理端点(model@baseUrl 摘要)
│   │   健康状态(ONLINE绿/OFFLINE红/UNKNOWN灰 + 最近探活) / 启用开关
│   │   操作: 测试连通 / 编辑 / 删除
│   ├── [+ 注册模型服务]（原"+添加模型"）→ 表单: 名称/类别/BaseURL/APIKey/模型/温度/最大长度
│   ├── [全部探活] 按钮 → 批量更新健康状态
│   └── 说明文案: "平台注册消费模型服务；模型部署与训练由推理平台/运维负责，版本由服务上报"
├── 默认模型与场景路由（新）
│   ├── 系统默认模型(下拉=服务清单)
│   ├── 场景映射: 通用对话/快速回复/长文本/NL2SQL/研判报告 → 各自绑定服务
│   └── 角色默认: POLICE_USER/ADMIN… → 绑定服务
├── 资源配额与安全约束（保留现状）
└── 大模型配置 → 收敛为"服务注册"同源（见 5.3）
```

### 5.2 用户端可见（普通用户）
- 对话设置仅显示"可用模型"下拉（GET available，按密级过滤 enabled 服务）
- 不再出现版本/部署语义；接入失败提示明确（服务离线/无权限分级）

### 5.3 兼容（原"大模型配置"对话框）
- 配置页保存 → 同时写 zy_ai_model_service 默认服务（provider 预设映射 baseUrl/model）——一套数据两个入口收敛到服务清单

---

## 六、迁移与兼容
| 项 | 方案 |
|---|---|
| 现有 zy_agent_config | 启动迁移器：有配置 → 自动注册为默认服务（category=EXTERNAL_APPROVED/本地按 baseUrl 判断） |
| 现有 model-policy JSON | 字段映射到新 policy 结构（sceneModels 空则 default 兜底） |
| 旧前端字段（version/type） | 移除；type→category 映射 |
| 后端 AiModelGateway | 参数由 config 改为 service 解析（内部收敛） |

---

## 七、验收标准
- [ ] 管理员注册 2+ 模型服务（本地合规 + 外发审批），test 连通更新 ONLINE/OFFLINE
- [ ] 场景路由：NL2SQL 对话命中 nl2sql 绑定服务；角色默认对 POLICE_USER 生效
- [ ] 密级：L1/L2 数据仅走本地合规服务；外发服务对涉密请求拦截
- [ ] 停用服务后对话明确报错（不静默伪应答）；健康检查调度可更新状态
- [ ] 用户端可用模型按密级过滤；原"大模型配置"保存与模型服务注册同源
- [ ] 配额/温度范围/开关锁定照常生效；回归 44/44

---

## 八、实施范围（不做什么）
- ❌ 不做模型部署编排/拉起（推理平台职责）
- ❌ 不做模型训练/微调
- ❌ 不做伪版本回滚（版本由推理服务上报展示只读，如需历史可记录调用版本审计）
- ❌ 前端不保留 version 手填与"下架"伪操作

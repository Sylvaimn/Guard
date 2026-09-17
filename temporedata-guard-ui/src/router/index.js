import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/common/Login.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    redirect: '/admin/dashboard',
    meta: { workspace: 'admin' },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '管理员工作台' } },
      // 资源管理
      { path: 'resource/cluster', name: 'AdminCluster', component: () => import('@/views/admin/Cluster.vue'), meta: { title: '计算集群' } },
      { path: 'resource/datasource', name: 'AdminDatasource', component: () => import('@/views/admin/Datasource.vue'), meta: { title: '数据源' } },
      { path: 'resource/driver', name: 'AdminDriver', component: () => import('@/views/admin/Driver.vue'), meta: { title: '驱动管理' } },
      { path: 'resource/container', name: 'AdminContainer', component: () => import('@/views/admin/Container.vue'), meta: { title: '计算容器' } },
      { path: 'resource/resource', name: 'AdminResource', component: () => import('@/views/admin/ResourceCenter.vue'), meta: { title: '资源中心' } },
      // 数据研发
      { path: 'development/workflow', name: 'AdminWorkflow', component: () => import('@/views/admin/Workflow.vue'), meta: { title: '工作流' } },
      { path: 'development/lakehouse', name: 'AdminLakehouse', component: () => import('@/views/admin/Lakehouse.vue'), meta: { title: '湖仓管理' } },
      { path: 'development/realtime', name: 'AdminRealtime', component: () => import('@/views/admin/Realtime.vue'), meta: { title: '实时计算' } },
      { path: 'development/globalvar', name: 'AdminGlobalVar', component: () => import('@/views/admin/GlobalVar.vue'), meta: { title: '全局变量' } },
      { path: 'development/funcrepo', name: 'AdminFuncRepo', component: () => import('@/views/admin/FuncRepo.vue'), meta: { title: '函数仓库' } },
      { path: 'development/dependency', name: 'AdminDependency', component: () => import('@/views/admin/Dependency.vue'), meta: { title: '依赖合集' } },
      { path: 'development/approval', name: 'AdminApproval', component: () => import('@/views/admin/Approval.vue'), meta: { title: '发布审批' } },
      { path: 'development/engine', name: 'AdminEngine', component: () => import('@/views/admin/Engine.vue'), meta: { title: '引擎作业' } },
      { path: 'development/ingestion', name: 'AdminIngestion', component: () => import('@/views/admin/Ingestion.vue'), meta: { title: '数据接入' } },
      // 运维监控
      { path: 'ops/monitor', name: 'AdminMonitor', component: () => import('@/views/admin/Monitor.vue'), meta: { title: '资源监控' } },
      { path: 'ops/scheduler', name: 'AdminScheduler', component: () => import('@/views/admin/Scheduler.vue'), meta: { title: '调度历史' } },
      { path: 'ops/alarm', name: 'AdminAlarm', component: () => import('@/views/admin/Alarm.vue'), meta: { title: '基线告警' } },
      // 数据治理
      { path: 'governance/meta', name: 'AdminMeta', component: () => import('@/views/admin/Meta.vue'), meta: { title: '结构采集' } },
      { path: 'governance/qualityrule', name: 'AdminQuality', component: () => import('@/views/admin/Quality.vue'), meta: { title: '质量规则' } },
      { path: 'governance/lineage', name: 'AdminLineage', component: () => import('@/views/admin/Lineage.vue'), meta: { title: '血缘分析' } },
      { path: 'governance/datacenter', name: 'AdminDataCenter', component: () => import('@/views/admin/DataCenter.vue'), meta: { title: '数据中心' } },
      // 数据资产
      { path: 'asset/indicator', name: 'AdminIndicator', component: () => import('@/views/admin/Indicator.vue'), meta: { title: '数据指标' } },
      { path: 'asset/tag', name: 'AdminTag', component: () => import('@/views/admin/Tag.vue'), meta: { title: '数据标签' } },
      { path: 'asset/dataasset', name: 'AdminDataAsset', component: () => import('@/views/admin/DataAsset.vue'), meta: { title: '数据目录' } },
      { path: 'asset/datamask', name: 'AdminDataMask', component: () => import('@/views/admin/DataMask.vue'), meta: { title: '数据大屏' } },
      // 数据服务
      { path: 'service/dataapi', name: 'AdminDataApi', component: () => import('@/views/admin/DataApi.vue'), meta: { title: '接口服务' } },
      { path: 'service/apilog', name: 'AdminApiLog', component: () => import('@/views/admin/ApiLog.vue'), meta: { title: '接口日志' } },
      { path: 'service/blacklist', name: 'AdminBlacklist', component: () => import('@/views/admin/Blacklist.vue'), meta: { title: '黑白名单' } },
      { path: 'service/report', name: 'AdminReport', component: () => import('@/views/admin/Report.vue'), meta: { title: '数据报表' } },
      { path: 'service/form', name: 'AdminForm', component: () => import('@/views/admin/Form.vue'), meta: { title: '表单管理' } },
      // 数据安全
      { path: 'security/auditlog', name: 'AdminAuditLog', component: () => import('@/views/admin/AuditLog.vue'), meta: { title: '审计总览' } },
      { path: 'security/changeaudit', name: 'AdminChangeAudit', component: () => import('@/views/admin/ChangeAudit.vue'), meta: { title: '变更审计' } },
      { path: 'security/datalevel', name: 'AdminDataLevel', component: () => import('@/views/admin/DataLevel.vue'), meta: { title: '分类分级' } },
      { path: 'security/sensitive', name: 'AdminSensitive', component: () => import('@/views/admin/Sensitive.vue'), meta: { title: '敏感数据' } },
      { path: 'security/mydata', name: 'AdminMyData', component: () => import('@/views/admin/MyData.vue'), meta: { title: '我的数据' } },
      { path: 'security/permapproval', name: 'AdminPermApproval', component: () => import('@/views/admin/PermApproval.vue'), meta: { title: '权限审批' } },
      // 系统管理
      { path: 'system/tenant', name: 'AdminTenant', component: () => import('@/views/admin/Tenant.vue'), meta: { title: '单位管理' } },
      { path: 'system/role', name: 'AdminRole', component: () => import('@/views/admin/Role.vue'), meta: { title: '角色管理' } },
      { path: 'system/org', name: 'AdminOrg', component: () => import('@/views/admin/Org.vue'), meta: { title: '组织架构' } },
      { path: 'system/passwordless', name: 'AdminPasswordless', component: () => import('@/views/admin/Passwordless.vue'), meta: { title: '免密登录' } },
      { path: 'system/notify', name: 'AdminNotify', component: () => import('@/views/admin/Notify.vue'), meta: { title: '通知配置' } },
      { path: 'system/settings', name: 'AdminSettings', component: () => import('@/views/admin/Settings.vue'), meta: { title: '系统设置' } },
      { path: 'system/license', name: 'AdminLicense', component: () => import('@/views/admin/License.vue'), meta: { title: '平台授权' } },
      { path: 'agent', name: 'AdminAgent', component: () => import('@/views/agent/Agent.vue'), meta: { title: '金盾智助' } },
      { path: 'message', name: 'AdminMessage', component: () => import('@/views/admin/Message.vue'), meta: { title: '消息中心' } },
      { path: 'profile', name: 'AdminProfile', component: () => import('@/views/admin/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },
  {
    path: '/workbench',
    component: () => import('@/views/user/UserLayout.vue'),
    redirect: '/workbench',
    meta: { workspace: 'user' },
    children: [
      // 工作台
      { path: '', name: 'UserWorkbench', component: () => import('@/views/user/UserWorkbench.vue'), meta: { title: '工作台', permission: 'workbench:view' } },
      { path: 'search', name: 'UserSearch', component: () => import('@/views/user/UserGlobalSearch.vue'), meta: { title: '全局搜索', permission: 'workbench:view' } },

      // 警务实战
      { path: 'intel/search', name: 'UserIntelSearch', component: () => import('@/views/admin/Intelligence.vue'), meta: { title: '情报检索', permission: 'intelligence:view', securityLevel: 'L4' } },
      { path: 'intel/entity', name: 'UserIntelEntity', component: () => import('@/views/user/UserEntityProfile.vue'), meta: { title: '对象研判', permission: 'intelligence:detail', securityLevel: 'L3' } },
      { path: 'intel/graph', name: 'UserIntelGraph', component: () => import('@/views/admin/EntityGraph.vue'), meta: { title: '关系图谱', permission: 'intelligence:graph', securityLevel: 'L3' } },
      { path: 'intel/clue', name: 'UserIntelClue', component: () => import('@/views/user/UserClueCenter.vue'), meta: { title: '线索中心', permission: 'intelligence:view' } },
      { path: 'intel/alert', name: 'UserIntelAlert', component: () => import('@/views/admin/WatchCenter.vue'), meta: { title: '预警中心', permission: 'intelligence:view' } },

      // 专案中心
      { path: 'project', name: 'UserProject', component: () => import('@/views/user/UserProjectList.vue'), meta: { title: '我的专案', permission: 'project:view' } },
      { path: 'project/detail', name: 'UserProjectDetail', component: () => import('@/views/user/UserProjectDetail.vue'), meta: { title: '专案详情', permission: 'project:view' } },
      { path: 'project/tasks', name: 'UserProjectTasks', component: () => import('@/views/user/UserProjectTasks.vue'), meta: { title: '专案任务', permission: 'project:view' } },
      { path: 'project/clues', name: 'UserProjectClues', component: () => import('@/views/user/UserProjectClues.vue'), meta: { title: '专案线索', permission: 'project:view' } },
      { path: 'project/objects', name: 'UserProjectObjects', component: () => import('@/views/user/UserProjectObjects.vue'), meta: { title: '专案对象', permission: 'project:view' } },
      { path: 'project/graph', name: 'UserProjectGraph', component: () => import('@/views/admin/EntityGraph.vue'), meta: { title: '专案图谱', permission: 'project:view' } },
      { path: 'project/reports', name: 'UserProjectReports', component: () => import('@/views/user/UserProjectReports.vue'), meta: { title: '专案报告', permission: 'project:view' } },
      { path: 'project/activity', name: 'UserProjectActivity', component: () => import('@/views/user/UserProjectActivity.vue'), meta: { title: '专案动态', permission: 'project:view' } },

      // 数据中心
      { path: 'data/catalog', name: 'UserDataCatalog', component: () => import('@/views/admin/DataAsset.vue'), meta: { title: '数据目录', permission: 'dataasset:view' } },
      { path: 'data/detail', name: 'UserDataDetail', component: () => import('@/views/user/UserDataDetail.vue'), meta: { title: '数据详情', permission: 'dataasset:view' } },
      { path: 'data/query', name: 'UserDataQuery', component: () => import('@/views/user/DataQuery.vue'), meta: { title: '数据查询', permission: 'dataasset:query' } },
      { path: 'data/history', name: 'UserDataHistory', component: () => import('@/views/user/UserQueryHistory.vue'), meta: { title: '查询历史', permission: 'dataasset:view' } },
      { path: 'data/metric', name: 'UserDataMetric', component: () => import('@/views/admin/Indicator.vue'), meta: { title: '指标中心', permission: 'dataasset:view' } },
      { path: 'data/mydata', name: 'UserMyData', component: () => import('@/views/admin/MyData.vue'), meta: { title: '我的数据', permission: 'dataasset:view' } },

      // 数据服务
      { path: 'service/api', name: 'UserServiceApi', component: () => import('@/views/admin/DataApi.vue'), meta: { title: 'API 服务', permission: 'dataapi:view' } },
      { path: 'service/api-detail', name: 'UserServiceApiDetail', component: () => import('@/views/user/UserApiDetail.vue'), meta: { title: 'API 详情', permission: 'dataapi:view' } },
      { path: 'service/report', name: 'UserServiceReport', component: () => import('@/views/admin/Report.vue'), meta: { title: '报表服务', permission: 'report:view' } },
      { path: 'service/form', name: 'UserServiceForm', component: () => import('@/views/admin/Form.vue'), meta: { title: '表单服务', permission: 'form:view' } },

      // 成果中心
      { path: 'result', name: 'UserResult', component: () => import('@/views/user/UserReportList.vue'), meta: { title: '我的报告', permission: 'report:view' } },
      { path: 'result/create', name: 'UserResultCreate', component: () => import('@/views/user/UserReportCreate.vue'), meta: { title: '报告创建', permission: 'report:create' } },
      { path: 'result/detail', name: 'UserResultDetail', component: () => import('@/views/user/UserReportDetail.vue'), meta: { title: '报告详情', permission: 'report:view' } },
      { path: 'result/tasks', name: 'UserResultTasks', component: () => import('@/views/user/UserGenerateTasks.vue'), meta: { title: '生成任务', permission: 'report:view' } },

      // AI 助手
      { path: 'agent', name: 'UserAgent', component: () => import('@/views/agent/Agent.vue'), meta: { title: '金盾智云 Agent', permission: 'agent:view' } },

      // 我的
      { path: 'mine/apply', name: 'UserMineApply', component: () => import('@/views/user/UserApply.vue'), meta: { title: '我的申请', permission: 'dataasset:apply' } },
      { path: 'mine/favorite', name: 'UserMineFavorite', component: () => import('@/views/user/UserFavorite.vue'), meta: { title: '我的收藏' } },
      { path: 'mine/ops', name: 'UserMineOps', component: () => import('@/views/user/UserMyOps.vue'), meta: { title: '我的操作' } },
      { path: 'mine/message', name: 'UserMineMessage', component: () => import('@/views/admin/Message.vue'), meta: { title: '消息中心' } },
      { path: 'mine/profile', name: 'UserMineProfile', component: () => import('@/views/admin/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },
  {
    path: '/',
    component: () => import('@/views/admin/Layout.vue'),
    redirect: '/dashboard',
    children: [
      // 资源管理
      { path: 'cluster', name: 'Cluster', component: () => import('@/views/admin/Cluster.vue'), meta: { title: '计算集群' } },
      { path: 'datasource', name: 'Datasource', component: () => import('@/views/admin/Datasource.vue'), meta: { title: '数据源' } },
      { path: 'driver', name: 'Driver', component: () => import('@/views/admin/Driver.vue'), meta: { title: '驱动管理' } },
      { path: 'container', name: 'Container', component: () => import('@/views/admin/Container.vue'), meta: { title: '计算容器' } },
      { path: 'resource', name: 'ResourceCenter', component: () => import('@/views/admin/ResourceCenter.vue'), meta: { title: '资源中心' } },
      // 数据研发
      { path: 'workflow', name: 'Workflow', component: () => import('@/views/admin/Workflow.vue'), meta: { title: '数据开发' } },
      { path: 'realtime', name: 'Realtime', component: () => import('@/views/admin/Realtime.vue'), meta: { title: '实时计算' } },
      { path: 'lakehouse', name: 'Lakehouse', component: () => import('@/views/admin/Lakehouse.vue'), meta: { title: '湖仓管理' } },
      { path: 'globalvar', name: 'GlobalVar', component: () => import('@/views/admin/GlobalVar.vue'), meta: { title: '全局变量' } },
      { path: 'funcrepo', name: 'FuncRepo', component: () => import('@/views/admin/FuncRepo.vue'), meta: { title: '函数仓库' } },
      { path: 'dependency', name: 'Dependency', component: () => import('@/views/admin/Dependency.vue'), meta: { title: '依赖合集' } },
      { path: 'approval', name: 'Approval', component: () => import('@/views/admin/Approval.vue'), meta: { title: '发布审批' } },
      // 数据运维
      { path: 'monitor', name: 'Monitor', component: () => import('@/views/admin/Monitor.vue'), meta: { title: '资源监控' } },
      { path: 'scheduler', name: 'Scheduler', component: () => import('@/views/admin/Scheduler.vue'), meta: { title: '调度历史' } },
      { path: 'alarm', name: 'Alarm', component: () => import('@/views/admin/Alarm.vue'), meta: { title: '基线告警' } },
      // 数据监控
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '监控总览' } },
      { path: 'meta', name: 'Meta', component: () => import('@/views/admin/Meta.vue'), meta: { title: '结构采集' } },
      { path: 'datacenter', name: 'DataCenter', component: () => import('@/views/admin/DataCenter.vue'), meta: { title: '数据中心' } },
      // 数据治理
      { path: 'quality', redirect: '/qualityrule' },
      { path: 'qualityrule', name: 'QualityRule', component: () => import('@/views/admin/Quality.vue'), meta: { title: '质量规则' } },
      // 数据安全
      { path: 'entitymaster', name: 'EntityMaster', component: () => import('@/views/admin/EntityMaster.vue'), meta: { title: '实体归一化' } },
      { path: 'entitygraph', name: 'EntityGraph', component: () => import('@/views/admin/EntityGraph.vue'), meta: { title: '实体图谱' } },
      { path: 'watchcenter', name: 'WatchCenter', component: () => import('@/views/admin/WatchCenter.vue'), meta: { title: '布控预警' } },
      { path: 'intelligence', name: 'Intelligence', component: () => import('@/views/admin/Intelligence.vue'), meta: { title: '情报检索' } },
      { path: 'auditlog', name: 'AuditLog', component: () => import('@/views/admin/AuditLog.vue'), meta: { title: '审计总览' } },
      { path: 'changeaudit', name: 'ChangeAudit', component: () => import('@/views/admin/ChangeAudit.vue'), meta: { title: '变更审计' } },
      { path: 'datalevel', name: 'DataLevel', component: () => import('@/views/admin/DataLevel.vue'), meta: { title: '分类分级' } },
      { path: 'sensitive', name: 'Sensitive', component: () => import('@/views/admin/Sensitive.vue'), meta: { title: '敏感数据' } },
      { path: 'mydata', name: 'MyData', component: () => import('@/views/admin/MyData.vue'), meta: { title: '我的数据' } },
      { path: 'permapproval', name: 'PermApproval', component: () => import('@/views/admin/PermApproval.vue'), meta: { title: '权限审批' } },
      // 数据资产
      { path: 'indicator', name: 'Indicator', component: () => import('@/views/admin/Indicator.vue'), meta: { title: '数据指标' } },
      { path: 'tag', name: 'Tag', component: () => import('@/views/admin/Tag.vue'), meta: { title: '数据标签' } },
      { path: 'dataasset', name: 'DataAsset', component: () => import('@/views/admin/DataAsset.vue'), meta: { title: '数据目录' } },
      { path: 'lineage', name: 'Lineage', component: () => import('@/views/admin/Lineage.vue'), meta: { title: '血缘分析' } },
      // 数据服务
      { path: 'datamask', name: 'DataMask', component: () => import('@/views/admin/DataMask.vue'), meta: { title: '数据大屏' } },
      { path: 'dataapi', name: 'DataApi', component: () => import('@/views/admin/DataApi.vue'), meta: { title: '接口服务' } },
      { path: 'apilog', name: 'ApiLog', component: () => import('@/views/admin/ApiLog.vue'), meta: { title: '接口日志' } },
      { path: 'blacklist', name: 'Blacklist', component: () => import('@/views/admin/Blacklist.vue'), meta: { title: '黑白名单' } },
      { path: 'report', name: 'Report', component: () => import('@/views/admin/Report.vue'), meta: { title: '数据报表' } },
      { path: 'form', name: 'Form', component: () => import('@/views/admin/Form.vue'), meta: { title: '表单管理' } },
      // 系统管理
      { path: 'tenant', name: 'Tenant', component: () => import('@/views/admin/Tenant.vue'), meta: { title: '单位管理' } },
      { path: 'role', name: 'Role', component: () => import('@/views/admin/Role.vue'), meta: { title: '角色管理' } },
      { path: 'org', name: 'Org', component: () => import('@/views/admin/Org.vue'), meta: { title: '组织架构' } },
      { path: 'notify', name: 'Notify', component: () => import('@/views/admin/Notify.vue'), meta: { title: '通知配置' } },
      { path: 'agent', name: 'Agent', component: () => import('@/views/agent/Agent.vue'), meta: { title: 'Agent助手' } },
      { path: 'settings', name: 'Settings', component: () => import('@/views/admin/Settings.vue'), meta: { title: '系统设置' } },
      // 平台管理
      { path: 'passwordless', name: 'Passwordless', component: () => import('@/views/admin/Passwordless.vue'), meta: { title: '免密登录' } },
      { path: 'license', name: 'License', component: () => import('@/views/admin/License.vue'), meta: { title: '平台授权' } },
      // 个人中心
      { path: 'profile', name: 'Profile', component: () => import('@/views/admin/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'message', name: 'Message', component: () => import('@/views/admin/Message.vue'), meta: { title: '消息中心' } },
      // 遗留页面
      { path: 'synctask', name: 'SyncTask', component: () => import('@/views/admin/SyncTask.vue'), meta: { title: '数据同步' } },
      { path: 'ingestion', name: 'Ingestion', component: () => import('@/views/admin/Ingestion.vue'), meta: { title: '数据采集' } },
      { path: 'engine', name: 'Engine', component: () => import('@/views/admin/Engine.vue'), meta: { title: '引擎作业' } },
      { path: 'secret', name: 'Secret', component: () => import('@/views/admin/Secret.vue'), meta: { title: '密钥管理' } },
      { path: 'ha', name: 'Ha', component: () => import('@/views/admin/Ha.vue'), meta: { title: '高可用' } },
      { path: 'work', name: 'Work', component: () => import('@/views/admin/Work.vue'), meta: { title: '作业管理' } },
      { path: 'viewpage', name: 'ViewPage', component: () => import('@/views/admin/ViewPage.vue'), meta: { title: '视图管理' } },
      { path: 'sqleditor', name: 'SqlEditor', component: () => import('@/views/admin/SqlEditor.vue'), meta: { title: 'SQL 编辑器' } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// Auth guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('zqy_token')
  if (to.meta.noAuth) {
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (!token) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router

/**
 * Sidebar menu configuration matching 15 functional groups.
 */
export const menuConfig = [
  {
    group: '资源管理',
    icon: 'FolderOpened',
    children: [
      { path: '/cluster', title: '计算集群', icon: 'Monitor' },
      { path: '/datasource', title: '数据源', icon: 'Coin' },
      { path: '/driver', title: '驱动管理', icon: 'Setting' },
      { path: '/container', title: '计算容器', icon: 'Box' },
      { path: '/resource', title: '资源中心', icon: 'FolderOpened' }
    ]
  },
  {
    group: '数据研发',
    icon: 'EditPen',
    children: [
      { path: '/workflow', title: '数据开发', icon: 'Share' },
      { path: '/realtime', title: '实时计算', icon: 'Odometer' },
      { path: '/globalvar', title: '全局变量', icon: 'PriceTag' },
      { path: '/funcrepo', title: '函数仓库', icon: 'MagicStick' },
      { path: '/dependency', title: '依赖合集', icon: 'Connection' },
      { path: '/approval', title: '发布审批', icon: 'DocumentChecked' }
    ]
  },
  {
    group: '数据运维',
    icon: 'TrendCharts',
    children: [
      { path: '/monitor', title: '资源监控', icon: 'TrendCharts' },
      { path: '/scheduler', title: '调度历史', icon: 'Clock' },
      { path: '/alarm', title: '基线告警', icon: 'Bell' }
    ]
  },
  {
    group: '数据监控',
    icon: 'DataAnalysis',
    children: [
      { path: '/dashboard', title: '监控总览', icon: 'DataBoard' },
      { path: '/meta', title: '结构采集', icon: 'DataAnalysis' },
      { path: '/datacenter', title: '数据中心', icon: 'Coin' }
    ]
  },
  {
    group: '警务实战',
    icon: 'Grid',
    children: [
      { path: '/entitymaster', title: '实体归一化', icon: 'UserFilled' },
      { path: '/entitygraph', title: '实体图谱', icon: 'Share' },
      { path: '/intelligence', title: '情报检索', icon: 'Search' },
      { path: '/watchcenter', title: '布控预警', icon: 'Bell' }
    ]
  },
  {
    group: '数据安全',
    icon: 'Lock',
    children: [
      { path: '/auditlog', title: '审计总览', icon: 'Tickets' },
      { path: '/changeaudit', title: '变更审计', icon: 'EditPen' },
      { path: '/datalevel', title: '分类分级', icon: 'Lock' },
      { path: '/sensitive', title: '敏感数据', icon: 'Warning' },
      { path: '/mydata', title: '我的数据', icon: 'Folder' },
      { path: '/permapproval', title: '权限审批', icon: 'DocumentChecked' }
    ]
  },
  {
    group: '数据资产',
    icon: 'Coin',
    children: [
      { path: '/indicator', title: '数据指标', icon: 'DataAnalysis' },
      { path: '/tag', title: '数据标签', icon: 'PriceTag' },
      { path: '/dataasset', title: '数据目录', icon: 'FolderOpened' },
      { path: '/qualityrule', title: '质量规则', icon: 'CircleCheck' },
      { path: '/lineage', title: '血缘分析', icon: 'Share' }
    ]
  },
  {
    group: '数据服务',
    icon: 'Link',
    children: [
      { path: '/datamask', title: '数据大屏', icon: 'DataBoard' },
      { path: '/dataapi', title: '接口服务', icon: 'Link' },
      { path: '/apilog', title: '接口日志', icon: 'List' },
      { path: '/blacklist', title: '黑白名单', icon: 'Warning' },
      { path: '/report', title: '数据报表', icon: 'Document' },
      { path: '/form', title: '表单管理', icon: 'DocumentChecked' }
    ]
  },
  {
    group: '系统管理',
    icon: 'Setting',
    children: [
      { path: '/tenant', title: '单位管理', icon: 'OfficeBuilding' },
      { path: '/role', title: '角色管理', icon: 'User' },
      { path: '/org', title: '组织架构', icon: 'OfficeBuilding' },
      { path: '/passwordless', title: '免密登录', icon: 'Key' },
      { path: '/notify', title: '通知配置', icon: 'Bell' },
      { path: '/settings', title: '系统设置', icon: 'Setting' },
      { path: '/license', title: '平台授权', icon: 'Stamp' }
    ]
  },
  {
    group: '个人中心',
    icon: 'User',
    children: [
      { path: '/profile', title: '个人中心', icon: 'User' },
      { path: '/message', title: '消息中心', icon: 'Message' }
    ]
  }
]

// Flat menu list for quick lookup
export const flatMenus = menuConfig.flatMap(g => g.children)

// Icon name to component mapping (all icons used in menu)
export const menuIcons = [
  'ChatDotRound', 'Monitor', 'Coin', 'Setting', 'Box', 'FolderOpened',
  'Layers', 'Grid', 'Stamp', 'Collection',
  'EditPen', 'Share', 'Odometer', 'PriceTag', 'MagicStick', 'Connection', 'DocumentChecked',
  'TrendCharts', 'Clock', 'Bell',
  'DataAnalysis', 'DataBoard', 'Upload',
  'CircleCheck', 'DocumentChecked',
  'Lock', 'Tickets', 'Flag', 'Warning', 'User', 'UserFilled',
  'MapLocation', 'Link', 'Switch', 'Document', 'List',
  'Reading', 'House', 'OfficeBuilding', 'Key', 'Message'
]

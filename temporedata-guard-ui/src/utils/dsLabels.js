/**
 * 共享标签工具: 数据源类型 / 工作流节点类型 品牌化中文显示。
 */

/** 数据源类型显示映射(枚举值 -> 品牌化名称) */
export const DS_LABELS = {
  MYSQL: 'MySQL',
  H2: 'H2',
  POSTGRESQL: 'PostgreSQL',
  POSTGRES: 'PostgreSQL',
  JDBC: 'JDBC 兼容',
  CLICKHOUSE: 'ClickHouse',
  ICEBERG: 'Iceberg 湖仓',
  KAFKA: 'Kafka 事件流'
}

/** 数据源类型标签颜色(Element Plus tag type) */
const DS_TAGS = {
  MYSQL: 'primary',
  POSTGRESQL: 'info',
  H2: '',
  JDBC: '',
  CLICKHOUSE: 'warning',
  ICEBERG: 'success',
  KAFKA: 'danger'
}

export function dsTypeLabel(type) {
  return DS_LABELS[type] || type || '-'
}

export function dsTypeTag(type) {
  return DS_TAGS[type] || 'info'
}

/** JDBC 系别名归一化(前端能力匹配用) */
export function dsPluginKey(type) {
  return type === 'MYSQL' || type === 'H2' || type === 'POSTGRESQL' || type === 'POSTGRES' ? 'JDBC' : type
}

/** 工作流节点类型显示映射 */
export const NODE_LABELS = {
  SQL: 'SQL 查询',
  SPARK_SQL: 'Spark SQL',
  SHELL: 'Shell 脚本',
  PYTHON: 'Python 脚本',
  DATAX: 'DataX 同步',
  CONDITION: '条件分支',
  SUB_WORKFLOW: '子工作流',
  DEPENDENCY: '依赖检查',
  HTTP: 'HTTP 调用',
  EMAIL: '邮件通知',
  DATA_QUALITY: '数据质量',
  LAKEHOUSE_SYNC: '湖仓同步',
  ANALYSIS_LOAD: '分析加载',
  FILE: '资源文件'
}

/** 工作流节点徽标颜色 */
const NODE_TAGS = {
  SQL: '',
  SPARK_SQL: 'danger',
  SHELL: 'warning',
  PYTHON: 'success',
  DATAX: '',
  CONDITION: 'info',
  SUB_WORKFLOW: 'primary',
  DEPENDENCY: 'info',
  HTTP: '',
  EMAIL: 'info',
  DATA_QUALITY: 'primary',
  LAKEHOUSE_SYNC: 'success',
  ANALYSIS_LOAD: 'primary',
  FILE: ''
}

export function nodeTypeLabel(type) {
  return NODE_LABELS[type] || type || 'SQL'
}

export function nodeTypeTag(type) {
  return NODE_TAGS[type] || 'info'
}

/** 采集/CDC 位点展示(source_key 美化: zy-cdc:track_flow_daily:INSERT -> 轨迹流动聚合表 增量) */
export function cdcSourceLabel(key) {
  if (!key) return '-'
  const tableMap = {
    track_flow_daily: '轨迹流动聚合表',
    case_stats_daily: '案件统计表',
    fund_flow_stats: '资金流水表',
    track_point_partitioned: '轨迹点位表',
    watch_alert_daily: '布控预警表'
  }
  const parts = String(key).split(':')
  const table = parts[1] || ''
  const op = parts[2] || ''
  const opLabel = { INSERT: '增量', UPDATE: '变更', DELETE: '删除' }
  const tableLabel = tableMap[table] || table
  return `${tableLabel} ${opLabel[op] || op}`.trim()
}

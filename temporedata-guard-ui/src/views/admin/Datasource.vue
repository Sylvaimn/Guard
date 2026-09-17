<template>
  <SearchTable
    title="数据源管理"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增数据源"
    @add="openAdd"
  >
    <!-- Table -->
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="name" label="数据源名称" min-width="150" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" effect="light">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机地址" min-width="150" />
        <el-table-column prop="port" label="端口" width="80" align="center" />
        <el-table-column prop="database" label="库/命名空间" width="110" align="center" />
        <el-table-column label="插件能力" width="200">
          <template #default="{ row }">
            <el-tag v-for="cap in capsFor(row)" :key="cap" size="small" effect="plain" class="cap-tag">
              {{ capLabel(cap) }}
            </el-tag>
            <span v-if="!capsFor(row).length">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CONNECTED' ? 'success' : 'danger'" effect="dark">
              <el-icon style="margin-right: 4px; vertical-align: middle;"><Connection /></el-icon>
              {{ row.status === 'CONNECTED' ? '已连接' : '已断开' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="crud.dialog.openEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button size="small" type="warning" link @click="handleTest(row)">
              <el-icon><Connection /></el-icon>测试
            </el-button>
            <el-button v-if="hasCap(row, 'meta')" size="small" type="info" link @click="openMeta(row)">
              <el-icon><Files /></el-icon>元数据
            </el-button>
            <el-button v-if="hasCap(row, 'query')" size="small" type="success" link @click="openQueryWithRow(row)">
              <el-icon><Search /></el-icon>查询
            </el-button>
            <el-popconfirm title="确定删除该数据源吗？" @confirm="crud.handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link>
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- Dialog -->
    <template #dialog>
      <el-dialog
        v-model="crud.dialog.visible"
        :title="crud.dialog.isEdit ? '编辑数据源' : '新增数据源'"
        width="540px"
        :close-on-click-modal="false"
        destroy-on-close
      >
        <el-form ref="crud.dialog.formRef" :model="crud.dialog.form" :rules="crud.dialog.rules" label-width="110px" label-position="right">
          <el-form-item label="数据源名称" prop="name">
            <el-input v-model="crud.dialog.form.name" placeholder="请输入数据源名称" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-select v-model="crud.dialog.form.type" placeholder="请选择数据源类型" style="width: 100%" @change="onTypeChange">
              <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value">
                <span>{{ t.label }}</span>
                <span class="type-cap">{{ t.caps.join('/') }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="主机地址" prop="host">
            <el-input v-model="crud.dialog.form.host" placeholder="请输入主机地址" />
          </el-form-item>
          <el-form-item label="端口" prop="port">
            <el-input-number v-model="crud.dialog.form.port" :min="1" :max="65535" style="width: 100%" />
          </el-form-item>
          <el-form-item v-if="formField('database')" :label="formField('database')" prop="database">
            <el-input v-model="crud.dialog.form.database" :placeholder="formField('database') === '命名空间' ? '请输入命名空间(表名)' : '请输入数据库名'" />
          </el-form-item>
          <el-form-item v-if="formField('username')" label="用户名" prop="username">
            <el-input v-model="crud.dialog.form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item v-if="formField('password')" label="密码" prop="password">
            <el-input v-model="crud.dialog.form.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="crud.dialog.close()">取消</el-button>
          <el-button type="primary" :loading="crud.dialog.submitting" @click="crud.handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </template>
  </SearchTable>

  <!-- 元数据面板 -->
  <el-dialog v-model="metaDialog.visible" :title="`元数据 - ${metaDialog.name}`" width="720px">
    <el-table :data="metaDialog.rows" v-loading="metaDialog.loading" stripe height="420">
      <el-table-column v-for="col in metaDialog.columns" :key="col" :prop="col" :label="col" min-width="140" />
    </el-table>
    <div v-if="!metaDialog.rows.length && !metaDialog.loading" class="empty-tip">暂无元数据（或采集失败，请检查连接）</div>
  </el-dialog>

  <!-- 查询预览 -->
  <el-dialog v-model="queryDialog.visible" :title="`SQL 查询 - ${queryDialog.name}`" width="820px">
    <el-input
      v-model="queryDialog.sql"
      type="textarea"
      :rows="3"
      placeholder="SELECT * FROM ... LIMIT 50"
      style="margin-bottom: 12px"
    />
    <div style="display: flex; gap: 8px; margin-bottom: 12px">
      <el-button type="primary" :loading="queryDialog.loading" @click="runQuery">执行</el-button>
      <el-button @click="runQuerySample" v-if="queryDialog.type === 'CLICKHOUSE'">示例: 分析表</el-button>
    </div>
    <el-table :data="queryDialog.rows" v-loading="queryDialog.loading" stripe height="380">
      <el-table-column v-for="col in queryDialog.columns" :key="col" :prop="col" :label="col" min-width="130" />
    </el-table>
    <div v-if="!queryDialog.rows.length && !queryDialog.loading" class="empty-tip">执行后展示结果</div>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { datasourceApi } from '@/api/modules/datasource'
import { useCRUD } from '@/composables/useCRUD'
import { ElMessage } from 'element-plus'
import { Connection, Edit, Delete, Files, Search } from '@element-plus/icons-vue'
import { dsTypeLabel as typeLabel, dsTypeTag as typeTag, dsPluginKey, DS_LABELS } from '@/utils/dsLabels'
import SearchTable from '@/components/common/SearchTable.vue'

const typeOptions = ref([])
const capsByType = reactive({})

const defaultForm = () => ({
  name: '',
  type: '',
  host: '',
  port: 3306,
  database: '',
  username: '',
  password: ''
})

const rules = {
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }]
}

const crud = useCRUD(datasourceApi, defaultForm, rules, { entityName: '数据源' })

const CAP_LABELS = {
  query: '查询', execute: '写执行', test: '测试', meta: '元数据', snapshots: '快照', topics: '主题'
}
const TYPE_FIELD = {
  MYSQL: { database: '数据库', username: true, password: true },
  H2: { database: '数据库', username: true, password: true },
  POSTGRESQL: { database: '数据库', username: true, password: true },
  CLICKHOUSE: { database: '数据库', username: true, password: true },
  ICEBERG: { database: '命名空间', username: false, password: false },
  KAFKA: { database: '', username: false, password: false }
}
const DEFAULT_PORTS = { MYSQL: 3306, H2: 9092, POSTGRESQL: 5432, CLICKHOUSE: 8123, ICEBERG: 9000, KAFKA: 9094 }

function capLabel(c) { return CAP_LABELS[c] || c }
function formField(f) { return (TYPE_FIELD[crud.dialog.form.type] || {})[f] }
function capsFor(row) {
  return capsByType[dsPluginKey(row.type)] || []
}
function hasCap(row, cap) { return capsFor(row).includes(cap) }

async function openAdd() {
  crud.dialog.form = defaultForm()
  crud.dialog.isEdit = false
  crud.dialog.visible = true
}

function onTypeChange(t) {
  crud.dialog.form.port = DEFAULT_PORTS[t] || 3306
}

async function loadPlugins() {
  try {
    const caps = await datasourceApi.plugins()
    Object.keys(caps).forEach(k => { capsByType[k] = caps[k] })
    const preset = ['MYSQL', 'H2', 'POSTGRESQL', 'CLICKHOUSE', 'ICEBERG', 'KAFKA']
    typeOptions.value = preset.map(t => ({
      value: t,
      label: typeLabel(t),
      caps: capsByType[t] || (t === 'MYSQL' || t === 'H2' || t === 'POSTGRESQL' ? capsByType.JDBC || [] : [])
    }))
  } catch (e) {
    typeOptions.value = Object.keys(DS_LABELS).map(t => ({ value: t, label: typeLabel(t), caps: [] }))
  }
}

async function handleTest(row) {
  try {
    const ok = await datasourceApi.test(row.id)
    if (ok) { ElMessage.success('连接测试成功'); crud.table.load() } else { ElMessage.error('连接测试失败') }
  } catch (err) {
    ElMessage.error(err.message || '连接测试失败')
  }
}

const metaDialog = reactive({ visible: false, name: '', rows: [], columns: [], loading: false })
async function openMeta(row) {
  metaDialog.name = row.name
  metaDialog.visible = true
  metaDialog.rows = []
  metaDialog.columns = []
  metaDialog.loading = true
  try {
    const rows = await datasourceApi.meta(row.id) || []
    metaDialog.rows = rows
    if (rows.length) metaDialog.columns = Object.keys(rows[0])
    else metaDialog.columns = ['name']
  } catch (err) {
    ElMessage.error(err.message || '元数据采集失败')
  } finally {
    metaDialog.loading = false
  }
}

const queryDialog = reactive({ visible: false, name: '', type: '', sql: '', rows: [], columns: [], loading: false })
function openQuery(row) {
  queryDialog.name = row.name
  queryDialog.type = row.type
  queryDialog.sql = row.type === 'CLICKHOUSE' ? "SELECT * FROM police_analysis LIMIT 50" : 'SELECT * FROM track_flow_daily LIMIT 50'
  queryDialog.rows = []
  queryDialog.columns = []
  queryDialog.visible = true
}
async function runQuery() {
  if (!queryDialog.sql.trim()) { ElMessage.warning('请输入 SQL'); return }
  queryDialog.loading = true
  try {
    const rows = await datasourceApi.query(queryDialog._id || queryDialog._rowId, queryDialog.sql)
    queryDialog.rows = rows || []
    queryDialog.columns = (rows && rows.length) ? Object.keys(rows[0]) : []
  } catch (err) {
    ElMessage.error(err.message || '查询失败')
  } finally {
    queryDialog.loading = false
  }
}
function runQuerySample() { queryDialog.sql = "SELECT * FROM police_analysis LIMIT 50" }

function openQueryWithRow(row) {
  queryDialog._id = row.id
  queryDialog._rowId = row.id
  openQuery(row)
}

onMounted(() => {
  loadPlugins()
  crud.table.fetch()
})
</script>

<style scoped>
.cap-tag { margin-right: 4px; }
.type-cap { float: right; color: #8492a6; font-size: 12px; }
.empty-tip { text-align: center; color: #909399; padding: 16px 0; font-size: 13px; }
</style>

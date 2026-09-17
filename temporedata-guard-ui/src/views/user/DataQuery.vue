<template>
  <div class="user-query">
    <div class="page-header">
      <h2 class="page-title">数据查询</h2>
      <span class="hint">受控查询 · 权限/密级/数据范围校验后返回脱敏结果</span>
    </div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="数据对象">
          <el-select v-model="table" placeholder="选择已授权数据" style="width: 240px">
            <el-option v-for="opt in options" :key="opt.table" :label="opt.label + ' (' + opt.level + ')'" :value="opt.table" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件">
          <el-input v-model="cond" placeholder="如 region='昌平区'（可选）" style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="runQuery">查询</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="errorMsg" :title="errorMsg" type="warning" :closable="false" style="margin-bottom: 12px" />
      <el-table :data="rows" v-loading="loading" stripe empty-text="选择数据后执行查询">
        <el-table-column v-for="col in columns" :key="col" :prop="col" :label="col" min-width="130" />
      </el-table>
      <div v-if="masked" class="masked-tip">敏感字段已按密级自动脱敏</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/modules/user'

const table = ref('track_flow_daily')
const cond = ref('')
const rows = ref([])
const columns = ref([])
const loading = ref(false)
const errorMsg = ref('')
const masked = ref(false)

const options = ref([])

async function runQuery() {
  loading.value = true
  errorMsg.value = ''
  masked.value = false
  try {
    const data = await userApi.query({ table: table.value, condition: cond.value.trim() })
    rows.value = data.rows || []
    columns.value = data.columns || []
    masked.value = !!data.masked
  } catch (e) {
    errorMsg.value = e.message || '查询失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    options.value = await userApi.queryOptions() || []
    if (options.value.length) table.value = options.value[0].table
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.hint { font-size: 12px; color: #8492a6; }
.masked-tip { margin-top: 10px; font-size: 12px; color: #f59e0b; }
</style>

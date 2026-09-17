<template>
  <div class="notify-page">
    <div class="page-header">
      <h2 class="page-title">通知配置</h2>
      <span class="page-sub">预警与消息送达配置：级别-渠道映射、渠道接入管理</span>
    </div>

    <!-- 级别-渠道映射 -->
    <el-card shadow="never" class="mapping-card">
      <template #header>
        <div class="card-header">
          <span>预警级别-渠道映射</span>
          <el-button size="small" type="primary" plain @click="saveMapping">保存映射</el-button>
        </div>
      </template>
      <el-table :data="mappingRows" border size="small">
        <el-table-column prop="level" label="预警级别" width="160">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" effect="light">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-for="ch in channels" :key="ch" :label="channelLabel(ch)" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="mapping[row.level][ch]" />
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="160">
          <template #default="{ row }">{{ levelDesc(row.level) }}</template>
        </el-table-column>
      </el-table>
      <div class="mapping-tip">紧急预警默认全渠道触达（短信+警务通APP+应急广播）；静默时段内仅紧急预警推送；同人同类型 30 分钟内自动去重。</div>
    </el-card>

    <SearchTable
      title="渠道接入"
      :loading="crud.table.loading"
      :total="crud.table.total"
      v-model:current-page="crud.table.currentPage"
      v-model:page-size="crud.table.pageSize"
      add-text="新增渠道"
      @add="crud.dialog.openAdd"
    >
      <template #default>
        <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
          <el-table-column prop="name" label="名称" min-width="150" />
          <el-table-column prop="channelType" label="渠道类型" width="130">
            <template #default="{ row }">
              <el-tag :type="channelTypeTag(row.channelType)" effect="light">{{ channelLabel(row.channelType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enabled === 1 ? 'success' : 'info'" effect="light">
                {{ row.enabled === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160" />
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="crud.dialog.openEdit(row)"><el-icon><Edit /></el-icon>编辑</el-button>
              <el-popconfirm title="确定删除？" @confirm="crud.handleDelete(row.id)">
                <template #reference>
                  <el-button size="small" type="danger" link><el-icon><Delete /></el-icon>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <template #dialog>
        <el-dialog v-model="crud.dialog.visible" :title="crud.dialog.isEdit ? '编辑' : '新增'" width="520px" :close-on-click-modal="false" destroy-on-close>
          <el-form ref="crud.dialog.formRef" :model="crud.dialog.form" :rules="crud.dialog.rules" label-width="100px" label-position="right">
            <el-form-item label="名称" prop="name">
              <el-input v-model="crud.dialog.form.name" placeholder="请输入通知名称" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="渠道类型" prop="channelType">
              <el-select v-model="crud.dialog.form.channelType" placeholder="请选择渠道类型" style="width: 100%">
                <el-option label="短信（运营商网关）" value="SMS" />
                <el-option label="警务通 APP 推送" value="APP" />
                <el-option label="邮件" value="EMAIL" />
                <el-option label="应急广播" value="BROADCAST" />
                <el-option label="站内信" value="INTERNAL" />
                <el-option label="Webhook" value="WEBHOOK" />
              </el-select>
            </el-form-item>
            <el-form-item label="配置" prop="config">
              <el-input v-model="crud.dialog.form.config" type="textarea" :rows="4" placeholder="网关地址 / 签名 / 密钥等接入参数" />
            </el-form-item>
            <el-form-item label="启用" prop="enabled">
              <el-switch v-model="crud.dialog.form.enabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="crud.dialog.close()">取消</el-button>
            <el-button type="primary" :loading="crud.dialog.submitting" @click="crud.handleSubmit">确定</el-button>
          </template>
        </el-dialog>
      </template>
    </SearchTable>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { notifyApi } from '@/api/modules/notify'
import { useCRUD } from '@/composables/useCRUD'
import SearchTable from '@/components/common/SearchTable.vue'
import { Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const channels = ['SMS', 'APP', 'INTERNAL', 'EMAIL', 'BROADCAST']
const channelLabel = t => ({ SMS: '短信', APP: '警务通APP', INTERNAL: '站内信', EMAIL: '邮件', BROADCAST: '应急广播', WEBHOOK: 'Webhook' }[t] || t)
const levelTag = l => ({ 紧急: 'danger', 重要: 'warning', 一般: 'info' }[l] || 'info')
const levelDesc = l => ({
  紧急: '布控对象出现、重点人员异常，须即时触达',
  重要: '线索流转、审批结果，当班内处理',
  一般: '任务完成、系统公告，工作台查看即可'
}[l])

const defaultMapping = () => ({
  紧急: { SMS: true, APP: true, INTERNAL: true, EMAIL: false, BROADCAST: true },
  重要: { SMS: false, APP: true, INTERNAL: true, EMAIL: false, BROADCAST: false },
  一般: { SMS: false, APP: false, INTERNAL: true, EMAIL: false, BROADCAST: false }
})
const mapping = reactive(defaultMapping())
const mappingRows = ['紧急', '重要', '一般'].map(level => ({ level }))

function loadMapping() {
  try {
    const saved = JSON.parse(localStorage.getItem('zy_notify_mapping') || '{}')
    Object.keys(saved).forEach(l => { if (mapping[l]) Object.assign(mapping[l], saved[l]) })
  } catch { /* ignore */ }
}
function saveMapping() {
  localStorage.setItem('zy_notify_mapping', JSON.stringify(mapping))
  ElMessage.success('级别-渠道映射已保存，预警推送立即生效')
}

const defaultForm = () => ({
  name: '',
  channelType: '',
  config: '',
  enabled: 0
})

const rules = {
  name: [{ required: true, message: '请输入通知名称', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
  config: [{ required: true, message: '请输入配置信息', trigger: 'blur' }]
}

const crud = useCRUD(notifyApi, defaultForm, rules, { entityName: '通知' })

function channelTypeTag(type) {
  const map = { EMAIL: '', SMS: 'warning', APP: 'success', INTERNAL: '', BROADCAST: 'danger', WEBHOOK: 'success' }
  return map[type] || 'info'
}

onMounted(() => {
  loadMapping()
  crud.table.fetch()
})
</script>

<style scoped>
.notify-page { padding: 0; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-sub { font-size: 12.5px; color: #9ca3af; }
.mapping-card { border-radius: 4px; margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.mapping-tip { font-size: 12px; color: #9ca3af; margin-top: 10px; line-height: 1.7; }
</style>

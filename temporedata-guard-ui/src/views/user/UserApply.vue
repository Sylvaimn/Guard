<template>
  <div class="user-apply">
    <div class="page-header">
      <h2 class="page-title">数据申请</h2>
      <span class="hint">申请数据访问 / 导出 / 跨单位 / 临时授权</span>
    </div>
    <el-card shadow="never" class="apply-card">
      <el-form label-width="110px" style="max-width: 640px">
        <el-form-item label="申请类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="数据访问" value="数据访问" />
            <el-option label="数据导出" value="数据导出" />
            <el-option label="跨单位访问" value="跨单位访问" />
            <el-option label="临时授权" value="临时授权" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据对象">
          <el-input v-model="form.target" placeholder="如：人员基础信息表 / 轨迹数据" />
        </el-form-item>
        <el-form-item label="使用目的">
          <el-input v-model="form.purpose" type="textarea" :rows="3" placeholder="说明使用场景与依据" />
        </el-form-item>
        <el-form-item label="使用专案">
          <el-input v-model="form.project" placeholder="关联专案（可选）" />
        </el-form-item>
        <el-form-item label="使用期限">
          <el-date-picker v-model="form.deadline" type="date" style="width: 100%" placeholder="选择截止日期" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
          <el-button @click="$router.push('/workbench/apply')">查看我的申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/modules/user'

const form = reactive({ type: '数据访问', target: '', purpose: '', project: '', deadline: null })
const submitting = ref(false)

async function submit() {
  if (!form.target.trim() || !form.purpose.trim()) {
    ElMessage.warning('请填写数据对象与使用目的')
    return
  }
  submitting.value = true
  try {
    const r = await userApi.apply({
      type: form.type, target: form.target, purpose: form.purpose,
      project: form.project, deadline: form.deadline ? String(form.deadline).slice(0, 10) : ''
    })
    ElMessage.success((r && r.msg) || '申请已提交，等待审批')
    form.target = ''
    form.purpose = ''
    form.project = ''
    form.deadline = null
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.hint { font-size: 12px; color: #8492a6; }
.apply-card { border-radius: 8px; max-width: 760px; }
</style>

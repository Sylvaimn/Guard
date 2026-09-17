<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { onErrorCaptured } from 'vue'
import { ElConfigProvider, ElMessage } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

onErrorCaptured(err => {
  console.error('[App] unhandled error:', err)
  ElMessage.error('系统异常，请稍后重试或联系管理员')
  return false
})
</script>
<template>
  <el-dialog v-model="visible" title="环境变量管理" width="600px" @open="loadEnvs">
    <div class="env-list">
      <div v-for="env in envs" :key="env.id" class="env-row">
        <el-input v-model="env.name" placeholder="变量名" size="small" class="env-name" />
        <el-input v-model="env.value" placeholder="值" size="small" class="env-value" />
        <el-button type="primary" size="small" @click="updateEnv(env)">保存</el-button>
        <el-button type="danger" size="small" circle :icon="Delete" @click="deleteEnv(env.id)" />
      </div>
      <div class="env-row new-env">
        <el-input v-model="newEnv.name" placeholder="新变量名" size="small" class="env-name" />
        <el-input v-model="newEnv.value" placeholder="值" size="small" class="env-value" />
        <el-button type="success" size="small" @click="createEnv">添加</el-button>
      </div>
    </div>
    <div class="env-hint">
      <p>使用 <code v-text="'{{变量名}}'"></code> 语法在 URL、Headers、Body 中引用变量</p>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { apiTesterApi } from '@/api/apiTesterApi'
import { ElMessage, ElMessageBox } from 'element-plus'

const visible = defineModel({ type: Boolean, default: false })

const envs = ref([])
const newEnv = reactive({ name: '', value: '' })

async function loadEnvs() {
  try {
    envs.value = await apiTesterApi.getEnvs()
  } catch (e) {
    console.error('加载环境变量失败', e)
  }
}

async function createEnv() {
  if (!newEnv.name || !newEnv.value) {
    ElMessage.warning('请填写变量名和值')
    return
  }
  try {
    await apiTesterApi.createEnv({ name: newEnv.name, value: newEnv.value })
    newEnv.name = ''
    newEnv.value = ''
    await loadEnvs()
    ElMessage.success('添加成功')
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

async function updateEnv(env) {
  try {
    await apiTesterApi.updateEnv(env.id, { name: env.name, value: env.value })
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function deleteEnv(id) {
  try {
    await ElMessageBox.confirm('确定删除该变量？', '提示', { type: 'warning' })
    await apiTesterApi.deleteEnv(id)
    await loadEnvs()
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.env-list { display: flex; flex-direction: column; gap: 8px; }
.env-row { display: flex; gap: 6px; align-items: center; }
.env-name { flex: 1; }
.env-value { flex: 2; }
.new-env { margin-top: 8px; padding-top: 8px; border-top: 1px solid #eee; }
.env-hint { margin-top: 12px; color: #999; font-size: 12px; }
.env-hint code { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; }
</style>

<template>
  <div class="api-tester">
    <!-- 左侧边栏 -->
    <div class="tester-sidebar">
      <ApiSidebar
        :collections="collections"
        :requestsMap="requestsMap"
        :activeRequestId="activeRequest.id"
        :history="history"
        @selectRequest="loadRequest"
        @createCollection="showCreateCollection"
        @renameCollection="showRenameCollection"
        @deleteCollection="handleDeleteCollection"
        @loadRequests="loadCollectionRequests"
        @loadHistory="loadHistoryItem"
        @clearHistory="handleClearHistory"
        @openEnvManager="envManagerVisible = true"
      />
    </div>

    <!-- 右侧工作区 -->
    <div class="tester-workspace">
      <!-- 管理员本地测试提示 -->
      <div v-if="authStore.isAdmin" class="admin-local-badge">
        <el-tag type="success" effect="plain" size="small">
          <el-icon><Unlock /></el-icon>
          管理员模式 — 可测试 localhost / 内网地址
        </el-tag>
      </div>

      <ApiRequestEditor
        :request="activeRequest"
        :loading="sending"
        @send="sendRequest"
        @save="showSaveDialog"
      />

      <div class="response-divider" />

      <ApiResponsePanel :response="lastResponse" />
    </div>

    <!-- 环境变量管理弹窗 -->
    <ApiEnvManager v-model="envManagerVisible" />

    <!-- 保存请求弹窗 -->
    <el-dialog v-model="saveDialogVisible" title="保存请求" width="400px">
      <el-form label-width="80px">
        <el-form-item label="请求名称">
          <el-input v-model="saveForm.name" placeholder="输入请求名称" />
        </el-form-item>
        <el-form-item label="所属集合">
          <el-select v-model="saveForm.collectionId" placeholder="选择集合（可选）" clearable>
            <el-option
              v-for="col in collections"
              :key="col.id"
              :label="col.name"
              :value="col.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建集合弹窗 -->
    <el-dialog v-model="collectionDialogVisible" :title="editingCollection ? '重命名集合' : '新建集合'" width="400px">
      <el-form label-width="80px">
        <el-form-item label="集合名称">
          <el-input v-model="collectionForm.name" placeholder="输入集合名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="collectionForm.description" type="textarea" placeholder="可选描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collectionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveCollection">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { apiTesterApi } from '@/api/apiTesterApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import ApiSidebar from '@/components/api-tester/ApiSidebar.vue'
import ApiRequestEditor from '@/components/api-tester/ApiRequestEditor.vue'
import ApiResponsePanel from '@/components/api-tester/ApiResponsePanel.vue'
import ApiEnvManager from '@/components/api-tester/ApiEnvManager.vue'

const authStore = useAuthStore()

// 状态
const collections = ref([])
const requestsMap = ref({})
const history = ref([])
const lastResponse = ref(null)
const sending = ref(false)
const envManagerVisible = ref(false)

// 当前活跃的请求
const activeRequest = reactive({
  method: 'GET',
  url: '',
  params: [{ key: '', value: '', enabled: true }],
  headers: [{ key: '', value: '', enabled: true }],
  body: { bodyType: 'json', content: '', formItems: [] },
  authType: 'none',
  authToken: '',
  apiKeyName: '',
  apiKeyValue: ''
})

// 保存弹窗
const saveDialogVisible = ref(false)
const saveForm = reactive({ name: '', collectionId: null })

// 集合弹窗
const collectionDialogVisible = ref(false)
const editingCollection = ref(null)
const collectionForm = reactive({ name: '', description: '' })

// ========== 数据加载 ==========

async function loadCollections() {
  try {
    collections.value = await apiTesterApi.getCollections()
  } catch (e) {
    console.error('加载集合失败', e)
  }
}

async function loadHistory() {
  try {
    const res = await apiTesterApi.getHistory(1, 20)
    history.value = res.records || res || []
  } catch (e) {
    console.error('加载历史失败', e)
  }
}

async function loadCollectionRequests(colId) {
  try {
    const requests = await apiTesterApi.getRequests(colId)
    requestsMap.value[colId] = requests
  } catch (e) {
    console.error('加载请求列表失败', e)
  }
}

// ========== 请求操作 ==========

async function sendRequest() {
  if (!activeRequest.url) {
    ElMessage.warning('请输入请求 URL')
    return
  }

  sending.value = true
  lastResponse.value = null

  try {
    // 构建 headers 对象
    const headers = {}
    activeRequest.headers
      .filter(h => h.enabled && h.key)
      .forEach(h => { headers[h.key] = h.value })

    // 注入 Auth
    if (activeRequest.authType === 'bearer' && activeRequest.authToken) {
      headers['Authorization'] = `Bearer ${activeRequest.authToken}`
    } else if (activeRequest.authType === 'apikey' && activeRequest.apiKeyName) {
      headers[activeRequest.apiKeyName] = activeRequest.apiKeyValue
    }

    // 构建请求参数
    const params = {
      method: activeRequest.method,
      url: buildUrlWithParams(),
      headers,
      bodyType: activeRequest.body.bodyType,
      body: getRequestBody()
    }

    const res = await apiTesterApi.proxy(params)
    lastResponse.value = res

    // 刷新历史
    await loadHistory()
  } catch (e) {
    ElMessage.error('请求失败: ' + (e.message || '未知错误'))
  } finally {
    sending.value = false
  }
}

function buildUrlWithParams() {
  let url = activeRequest.url
  const enabledParams = activeRequest.params.filter(p => p.enabled && p.key)
  if (enabledParams.length > 0) {
    const queryString = enabledParams.map(p => `${encodeURIComponent(p.key)}=${encodeURIComponent(p.value)}`).join('&')
    url += (url.includes('?') ? '&' : '?') + queryString
  }
  return url
}

function getRequestBody() {
  if (activeRequest.method === 'GET') return null
  const body = activeRequest.body
  if (body.bodyType === 'json' || body.bodyType === 'raw') {
    return body.content
  }
  if (body.bodyType === 'form') {
    const formData = {}
    body.formItems.filter(f => f.enabled && f.key).forEach(f => { formData[f.key] = f.value })
    return new URLSearchParams(formData).toString()
  }
  return body.content
}

// ========== 加载请求 ==========

function loadRequest(req) {
  activeRequest.method = req.method || 'GET'
  activeRequest.url = req.url || ''
  activeRequest.body.bodyType = req.bodyType || 'json'
  activeRequest.body.content = req.body || ''

  // 解析 headers
  if (req.headers) {
    try {
      const h = JSON.parse(req.headers)
      activeRequest.headers = Object.entries(h).map(([key, value]) => ({ key, value, enabled: true }))
    } catch {
      activeRequest.headers = [{ key: '', value: '', enabled: true }]
    }
  }
}

function loadHistoryItem(item) {
  activeRequest.method = item.method || 'GET'
  activeRequest.url = item.url || ''
  activeRequest.body.bodyType = item.bodyType || 'json'
  activeRequest.body.content = item.body || ''

  if (item.headers) {
    try {
      const h = JSON.parse(item.headers)
      activeRequest.headers = Object.entries(h).map(([key, value]) => ({ key, value, enabled: true }))
    } catch {
      activeRequest.headers = [{ key: '', value: '', enabled: true }]
    }
  }

  // 显示历史响应
  if (item.statusCode) {
    lastResponse.value = {
      statusCode: item.statusCode,
      time: item.responseTime,
      size: item.responseSize,
      body: item.responseBody,
      headers: item.responseHeaders ? JSON.parse(item.responseHeaders) : {}
    }
  }
}

// ========== 保存请求 ==========

function showSaveDialog() {
  saveForm.name = activeRequest.url ? `${activeRequest.method} ${activeRequest.url}` : ''
  saveForm.collectionId = null
  saveDialogVisible.value = true
}

async function handleSave() {
  if (!saveForm.name) {
    ElMessage.warning('请输入请求名称')
    return
  }

  try {
    const data = {
      name: saveForm.name,
      collectionId: saveForm.collectionId || null,
      method: activeRequest.method,
      url: activeRequest.url,
      headers: JSON.stringify(
        Object.fromEntries(
          activeRequest.headers.filter(h => h.enabled && h.key).map(h => [h.key, h.value])
        )
      ),
      bodyType: activeRequest.body.bodyType,
      body: activeRequest.body.content
    }

    await apiTesterApi.saveRequest(data)
    saveDialogVisible.value = false
    ElMessage.success('保存成功')

    // 刷新集合
    await loadCollections()
    if (saveForm.collectionId) {
      await loadCollectionRequests(saveForm.collectionId)
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

// ========== 集合管理 ==========

function showCreateCollection() {
  editingCollection.value = null
  collectionForm.name = ''
  collectionForm.description = ''
  collectionDialogVisible.value = true
}

function showRenameCollection(col) {
  editingCollection.value = col
  collectionForm.name = col.name
  collectionForm.description = col.description || ''
  collectionDialogVisible.value = true
}

async function handleSaveCollection() {
  if (!collectionForm.name) {
    ElMessage.warning('请输入集合名称')
    return
  }

  try {
    const data = { name: collectionForm.name, description: collectionForm.description }
    if (editingCollection.value) {
      await apiTesterApi.updateCollection(editingCollection.value.id, data)
    } else {
      await apiTesterApi.createCollection(data)
    }
    collectionDialogVisible.value = false
    await loadCollections()
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function handleDeleteCollection(col) {
  try {
    await ElMessageBox.confirm(`确定删除集合"${col.name}"及其所有请求？`, '提示', { type: 'warning' })
    await apiTesterApi.deleteCollection(col.id)
    await loadCollections()
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// ========== 历史管理 ==========

async function handleClearHistory() {
  try {
    await ElMessageBox.confirm('确定清空所有历史记录？', '提示', { type: 'warning' })
    await apiTesterApi.clearHistory()
    history.value = []
    ElMessage.success('已清空')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('清空失败')
  }
}

// ========== 初始化 ==========

onMounted(() => {
  loadCollections()
  loadHistory()
})
</script>

<style scoped>
.api-tester {
  display: flex;
  height: calc(100vh - 140px);
  overflow: hidden;
}

.tester-sidebar {
  width: 260px;
  min-width: 260px;
  border-right: 1px solid #ebeef5;
  overflow-y: auto;
  background: #fafafa;
}

.tester-workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
}

.response-divider {
  height: 1px;
  background: #ebeef5;
  margin: 12px 0;
}

.admin-local-badge {
  margin-bottom: 8px;
}

.admin-local-badge .el-tag {
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 响应区域可滚动 */
.tester-workspace > :last-child {
  flex: 1;
  overflow: auto;
}
</style>

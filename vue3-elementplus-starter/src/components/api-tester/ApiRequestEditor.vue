<template>
  <div class="request-editor">
    <!-- URL 栏 -->
    <div class="url-bar">
      <el-select v-model="request.method" size="default" class="method-select">
        <el-option label="GET" value="GET" />
        <el-option label="POST" value="POST" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
      </el-select>
      <el-input
        v-model="request.url"
        placeholder="输入请求 URL，支持 {{变量名}} 语法"
        size="default"
        class="url-input"
        @keyup.enter="$emit('send')"
      />
      <el-button type="primary" :loading="loading" @click="$emit('send')">发送</el-button>
      <el-button @click="$emit('save')">保存</el-button>
    </div>

    <!-- 请求标签 -->
    <div class="request-tabs">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="Params" name="params">
          <ApiKeyValueEditor v-model="request.params" />
        </el-tab-pane>
        <el-tab-pane label="Headers" name="headers">
          <ApiKeyValueEditor v-model="request.headers" />
        </el-tab-pane>
        <el-tab-pane label="Body" name="body">
          <ApiBodyEditor v-model="request.body" @selectFile="$emit('selectFile', $event)" />
        </el-tab-pane>
        <el-tab-pane label="Auth" name="auth">
          <div class="auth-section">
            <el-select v-model="request.authType" size="small" placeholder="认证类型" class="auth-type-select">
              <el-option label="无认证" value="none" />
              <el-option label="Bearer Token" value="bearer" />
              <el-option label="API Key" value="apikey" />
            </el-select>
            <el-input
              v-if="request.authType === 'bearer'"
              v-model="request.authToken"
              placeholder="Token"
              size="small"
              class="auth-input"
            />
            <template v-if="request.authType === 'apikey'">
              <el-input v-model="request.apiKeyName" placeholder="Header Name" size="small" class="auth-key-name" />
              <el-input v-model="request.apiKeyValue" placeholder="API Key" size="small" class="auth-input" />
            </template>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ApiKeyValueEditor from './ApiKeyValueEditor.vue'
import ApiBodyEditor from './ApiBodyEditor.vue'

defineProps({
  request: { type: Object, required: true },
  loading: { type: Boolean, default: false }
})

defineEmits(['send', 'save', 'selectFile'])

const activeTab = ref('params')
</script>

<style scoped>
.request-editor { display: flex; flex-direction: column; gap: 8px; }
.url-bar { display: flex; gap: 6px; align-items: center; }
.method-select { width: 110px; }
.url-input { flex: 1; }
.url-input :deep(input) { font-family: 'Consolas', 'Monaco', monospace; }
.request-tabs { border: 1px solid #ebeef5; border-radius: 4px; padding: 0 12px; }
.auth-section { display: flex; gap: 8px; align-items: center; padding: 8px 0; }
.auth-type-select { width: 150px; }
.auth-input { flex: 1; }
.auth-key-name { width: 150px; }
</style>

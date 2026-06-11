<template>
  <div class="body-editor">
    <div class="body-type-bar">
      <el-radio-group v-model="bodyType" size="small">
        <el-radio-button value="json">JSON</el-radio-button>
        <el-radio-button value="form">Form</el-radio-button>
        <el-radio-button value="multipart">Multipart</el-radio-button>
        <el-radio-button value="raw">Raw</el-radio-button>
      </el-radio-group>
    </div>

    <div class="body-content">
      <!-- JSON 编辑器 -->
      <div v-if="bodyType === 'json'" class="json-editor">
        <el-input
          v-model="bodyContent"
          type="textarea"
          :rows="12"
          placeholder='{ "key": "value" }'
          class="code-textarea"
          @input="emitUpdate"
        />
        <div class="editor-actions">
          <el-button size="small" @click="formatJson">格式化</el-button>
          <span v-if="jsonError" class="json-error">{{ jsonError }}</span>
          <span v-else-if="bodyContent" class="json-valid">✓ JSON 有效</span>
        </div>
      </div>

      <!-- Form 表单 -->
      <div v-else-if="bodyType === 'form' || bodyType === 'multipart'">
        <div v-for="(item, index) in formItems" :key="index" class="kv-row">
          <el-input v-model="item.key" placeholder="Key" size="small" class="kv-key" />
          <template v-if="bodyType === 'multipart' && item.isFile">
            <el-input v-model="item.value" placeholder="文件路径" size="small" class="kv-value" disabled />
            <el-button size="small" @click="selectFile(index)">选择文件</el-button>
          </template>
          <template v-else>
            <el-input v-model="item.value" placeholder="Value" size="small" class="kv-value" />
          </template>
          <el-checkbox v-if="bodyType === 'multipart'" v-model="item.isFile" size="small" label="文件" />
          <el-button type="danger" :icon="Delete" size="small" circle @click="removeFormItem(index)" />
        </div>
        <el-button size="small" type="primary" plain @click="addFormItem">+ 添加</el-button>
      </div>

      <!-- Raw 文本 -->
      <div v-else>
        <el-input
          v-model="bodyContent"
          type="textarea"
          :rows="12"
          placeholder="输入原始内容..."
          class="code-textarea"
          @input="emitUpdate"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { Delete } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Object, default: () => ({ bodyType: 'json', content: '' }) }
})

const emit = defineEmits(['update:modelValue'])

const bodyType = ref(props.modelValue.bodyType || 'json')
const bodyContent = ref(props.modelValue.content || '')
const formItems = ref(props.modelValue.formItems || [{ key: '', value: '', enabled: true, isFile: false }])

const jsonError = computed(() => {
  if (bodyType.value !== 'json' || !bodyContent.value) return ''
  try {
    JSON.parse(bodyContent.value)
    return ''
  } catch (e) {
    return e.message
  }
})

function formatJson() {
  try {
    const parsed = JSON.parse(bodyContent.value)
    bodyContent.value = JSON.stringify(parsed, null, 2)
    emitUpdate()
  } catch (e) {
    // 格式化失败，忽略
  }
}

function addFormItem() {
  formItems.value.push({ key: '', value: '', enabled: true, isFile: false })
}

function removeFormItem(index) {
  formItems.value.splice(index, 1)
}

function selectFile(index) {
  // 文件选择由父组件处理
  emit('selectFile', index)
}

function emitUpdate() {
  emit('update:modelValue', {
    bodyType: bodyType.value,
    content: bodyContent.value,
    formItems: formItems.value
  })
}

watch(bodyType, emitUpdate)
</script>

<style scoped>
.body-editor { display: flex; flex-direction: column; gap: 8px; }
.body-type-bar { margin-bottom: 4px; }
.body-content { min-height: 200px; }
.code-textarea :deep(textarea) { font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; }
.editor-actions { display: flex; align-items: center; gap: 12px; margin-top: 6px; }
.json-error { color: #f56c6c; font-size: 12px; }
.json-valid { color: #67c23a; font-size: 12px; }
.kv-row { display: flex; gap: 6px; align-items: center; margin-bottom: 6px; }
.kv-key { flex: 1; }
.kv-value { flex: 2; }
</style>

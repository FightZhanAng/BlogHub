<template>
  <div class="key-value-editor">
    <div v-for="(item, index) in items" :key="index" class="kv-row">
      <el-input v-model="item.key" placeholder="Key" size="small" class="kv-key" />
      <el-input v-model="item.value" placeholder="Value" size="small" class="kv-value" />
      <el-checkbox v-model="item.enabled" size="small" class="kv-enabled" />
      <el-button type="danger" :icon="Delete" size="small" circle @click="removeItem(index)" />
    </div>
    <el-button size="small" type="primary" plain @click="addItem">+ 添加</el-button>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { Delete } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue'])

const items = defineModel({ type: Array, default: () => [] })

function addItem() {
  items.value.push({ key: '', value: '', enabled: true })
}

function removeItem(index) {
  items.value.splice(index, 1)
}

// 初始化至少一行
if (items.value.length === 0) {
  addItem()
}
</script>

<style scoped>
.key-value-editor {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.kv-row {
  display: flex;
  gap: 6px;
  align-items: center;
}
.kv-key { flex: 1; }
.kv-value { flex: 2; }
.kv-enabled { margin: 0 4px; }
</style>

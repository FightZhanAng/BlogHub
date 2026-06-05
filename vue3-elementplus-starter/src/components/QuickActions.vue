<template>
  <div class="quick-actions">
    <el-button size="small" text @click="$emit('action', 'summarize')">📝 总结</el-button>
    <el-button size="small" text @click="$emit('action', 'explain')">💡 解释</el-button>
    <el-button size="small" text @click="$emit('action', 'optimize')">✨ 优化</el-button>
    <el-button size="small" text @click="$emit('action', 'translate')">🌐 翻译</el-button>
  </div>
</template>

<script setup>
defineEmits(['action'])
</script>

<style scoped>
.quick-actions {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
}
</style>

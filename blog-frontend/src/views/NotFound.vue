<template>
  <div class="not-found">
    <div class="not-found-content">
      <div class="not-found-code">404</div>
      <h1>页面未找到</h1>
      <p>您访问的页面不存在或已被移除</p>
      <div class="not-found-actions">
        <el-button type="primary" @click="$router.push('/')">
          <el-icon><HomeFilled /></el-icon> 返回首页
        </el-button>
        <el-button @click="$router.back()">
          <el-icon><Back /></el-icon> 返回上一页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'NotFound' })
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 200px);
  text-align: center;
}

.not-found-code {
  font-size: 120px;
  font-weight: 800;
  color: var(--color-accent);
  opacity: 0.3;
  line-height: 1;
  margin-bottom: 16px;
}

.not-found h1 {
  font-size: 24px;
  color: var(--color-text);
  margin: 0 0 8px;
}

.not-found p {
  color: var(--color-text-secondary);
  margin: 0 0 32px;
}

.not-found-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>

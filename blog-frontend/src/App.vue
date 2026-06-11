<template>
  <ErrorBoundary>
    <router-view />
  </ErrorBoundary>
</template>

<script setup>
// 修复 Element Plus 给 body 加 aria-hidden 触发 Chrome 警告
import { onMounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

onMounted(() => {
  const observer = new MutationObserver(() => {
    if (document.body.getAttribute('aria-hidden')) {
      document.body.removeAttribute('aria-hidden')
    }
  })
  observer.observe(document.body, { attributes: true, attributeFilter: ['aria-hidden'] })
})
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei',
    Arial, sans-serif;
}
</style>

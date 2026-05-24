<template>
  <div class="about">
    <el-card shadow="never" class="about-card">
      <div class="about-header">
        <div class="avatar-ring">
          <el-avatar :size="72" icon="UserFilled" />
        </div>
        <h1>关于 BlogHub</h1>
        <p class="about-subtitle">一个简约、现代的博客平台</p>
      </div>

      <el-divider />

      <div class="about-body">
        <h3><el-icon style="vertical-align:middle;margin-right:6px"><InfoFilled /></el-icon>项目介绍</h3>
        <p>BlogHub 是一个基于 Vue 3 + Element Plus 构建的博客前端，后端使用 Spring Boot + MyBatis-Plus + MySQL 提供数据支持。</p>

        <h3><el-icon style="vertical-align:middle;margin-right:6px"><Tools /></el-icon>技术栈</h3>
        <div class="tech-tags">
          <el-tag v-for="t in techStack" :key="t.name" :color="t.color" effect="dark" round>
            {{ t.name }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border class="tech-desc">
          <el-descriptions-item label="前端框架">Vue 3.4 + Composition API</el-descriptions-item>
          <el-descriptions-item label="UI 组件库">Element Plus</el-descriptions-item>
          <el-descriptions-item label="构建工具">Vite 5</el-descriptions-item>
          <el-descriptions-item label="状态管理">Pinia</el-descriptions-item>
          <el-descriptions-item label="后端框架">Spring Boot 2.7</el-descriptions-item>
          <el-descriptions-item label="ORM">MyBatis-Plus</el-descriptions-item>
          <el-descriptions-item label="数据库">MySQL 8.0</el-descriptions-item>
          <el-descriptions-item label="HTTP 客户端">Axios</el-descriptions-item>
        </el-descriptions>

        <h3 style="margin-top:24px"><el-icon style="vertical-align:middle;margin-right:6px"><Link /></el-icon>订阅</h3>
        <div class="tech-tags">
          <el-tag color="#f60" effect="dark" round style="cursor:pointer" @click="openFeed">
            RSS 订阅
          </el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
function openFeed() {
  window.open('http://localhost:8080/feed.xml', '_blank')
}

const techStack = [
  { name: 'Vue 3', color: '#42b883' },
  { name: 'Element Plus', color: '#409eff' },
  { name: 'Vite', color: '#646cff' },
  { name: 'Spring Boot', color: '#6db33f' },
  { name: 'MyBatis-Plus', color: '#e3542d' },
  { name: 'MySQL', color: '#4479a1' },
  { name: 'Axios', color: '#5a29e4' },
]
</script>

<style scoped>
.about {
  max-width: 720px;
  margin: 0 auto;
}

.about-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.about-header {
  text-align: center;
  padding: 20px 0;
}

.avatar-ring {
  display: inline-block;
  padding: 4px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #42b883);
  margin-bottom: 16px;
}

.about-header h1 {
  margin: 0 0 4px 0;
  font-size: 22px;
  color: #303133;
}

.about-subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.about-body {
  padding: 0 8px;
}

.about-body h3 {
  margin: 20px 0 12px 0;
  font-size: 15px;
  color: #303133;
}

.about-body p {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
}

.tech-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.tech-desc {
  margin-top: 16px;
}

.tech-desc :deep(.el-descriptions__cell) {
  padding: 12px 16px;
}
</style>

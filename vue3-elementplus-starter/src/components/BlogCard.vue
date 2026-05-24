<template>
  <el-card shadow="never" class="blog-card" @click="$router.push(`/blog/${slug}`)">
    <!-- 封面图 -->
    <div class="card-cover" v-if="coverImage">
      <img :src="coverImage" alt="" @error="$event.target.style.display='none'" />
    </div>
    <div class="card-content">
      <!-- 标签 -->
      <div class="card-tags" v-if="tags.length">
        <el-tag
          v-for="tag in tags"
          :key="tag"
          size="small"
          round
          effect="plain"
          class="card-tag"
        >
          {{ tag }}
        </el-tag>
      </div>

      <!-- 标题 -->
      <h3 class="card-title">
        <el-tag v-if="isPinned" size="small" type="warning" effect="plain" class="pinned-tag">置顶</el-tag>
        {{ title }}
      </h3>

      <!-- 摘要 -->
      <p class="card-desc" v-if="description">{{ description }}</p>

      <!-- 底部 -->
      <div class="card-footer">
        <span class="card-date">
          <el-icon :size="14"><Calendar /></el-icon>
          {{ date }}
        </span>
        <span class="card-author" v-if="author">
          <el-icon :size="14"><User /></el-icon>
          {{ author }}
        </span>
        <div class="card-actions">
          <span class="stat-item" v-if="likes > 0">
            <el-icon :size="14"><Heart /></el-icon>
            {{ likes }}
          </span>
          <span class="stat-item" v-if="views > 0">
            <el-icon :size="14"><View /></el-icon>
            {{ views }}
          </span>
          <span v-if="isBookmarked" class="bookmark-badge">
            <el-icon :size="14" color="#e6a23c"><StarFilled /></el-icon>
          </span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  slug: String,
  title: String,
  date: String,
  tags: {
    type: Array,
    default: () => [],
  },
  coverImage: String,
  description: String,
  author: String,
  likes: { type: Number, default: 0 },
  views: { type: Number, default: 0 },
  isBookmarked: { type: Boolean, default: false },
  isPinned: { type: Boolean, default: false },
})
</script>

<style scoped>
.blog-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
  overflow: hidden;
}

.card-cover {
  width: 100%;
  height: 160px;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.blog-card:hover .card-cover img {
  transform: scale(1.05);
}

.blog-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
  border-color: #c6e2ff;
}

.blog-card :deep(.el-card__body) {
  padding: 20px;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.card-tag {
  font-size: 11px;
  padding: 0 8px;
  height: 22px;
  line-height: 22px;
  border: 1px solid #e8eaed;
  color: #909399;
}

.pinned-tag {
  margin-right: 6px;
  vertical-align: middle;
}
.card-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #1a1a2e;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
}

.card-date {
  font-size: 12px;
  color: #c0c4cc;
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-author {
  font-size: 12px;
  color: #c0c4cc;
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-item {
  font-size: 12px;
  color: #c0c4cc;
  display: flex;
  align-items: center;
  gap: 3px;
}

.bookmark-badge {
  display: flex;
  align-items: center;
}
</style>

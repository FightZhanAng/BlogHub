<template>
  <div class="blog-card" role="link" tabindex="0" @click="$router.push(`/blog/${slug}`)" @keydown.enter="$router.push(`/blog/${slug}`)" @keydown.space.prevent="$router.push(`/blog/${slug}`)">
    <!-- 封面图 -->
    <div class="card-cover" v-if="coverImage">
      <img :src="coverImage" alt="" @error="$event.target.style.display='none'" />
    </div>

    <div class="card-body">
      <!-- 标签 -->
      <div class="card-tags" v-if="tags.length">
        <span v-for="tag in tags" :key="tag" class="card-tag">{{ tag }}</span>
      </div>

      <!-- 标题 -->
      <h3 class="card-title">
        <span v-if="isPinned" class="pin-badge">置顶</span>
        <span v-if="isPrivate" class="private-badge">🔒 仅自己可见</span>
        <span v-if="isHidden" class="hidden-badge">🚫 已隐藏</span>
        {{ title }}
      </h3>

      <!-- 摘要 -->
      <p class="card-desc" v-if="description">{{ description }}</p>

      <!-- 底部 -->
      <div class="card-footer">
        <div class="card-meta">
          <span class="card-date">{{ date }}</span>
          <span v-if="author" class="card-author">· {{ author }}</span>
        </div>
        <div class="card-stats">
          <span v-if="likes > 0" class="stat-item">{{ likes }} 赞</span>
          <span v-if="views > 0" class="stat-item">{{ views }} 阅读</span>
          <span v-if="isBookmarked" class="bookmark-badge">
            <el-icon :size="14" color="#c9a96e"><StarFilled /></el-icon>
          </span>
        </div>
      </div>
    </div>
  </div>
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
  isPrivate: { type: Boolean, default: false },
  isHidden: { type: Boolean, default: false },
})
</script>

<style scoped>
.blog-card {
  padding: var(--space-lg) 0;
  border-bottom: 1px solid var(--color-border-light);
  cursor: pointer;
  transition: all var(--duration) var(--ease);
}

.blog-card:first-child {
  padding-top: 0;
}

.blog-card:hover {
  padding-left: var(--space-sm);
}

.card-cover {
  width: 100%;
  height: 180px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-md);
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--duration-slow) var(--ease);
}

.blog-card:hover .card-cover img {
  transform: scale(1.03);
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.card-tags {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.card-tag {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-accent);
  letter-spacing: 0.02em;
}

.card-tag::before {
  content: '#';
  margin-right: 1px;
  opacity: 0.6;
}

.pin-badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--color-accent);
  border: 1px solid var(--color-accent);
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  margin-right: 6px;
  vertical-align: middle;
}

.private-badge {
  font-size: 10px;
  font-weight: 600;
  color: #e6a23c;
  border: 1px solid #e6a23c;
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  margin-right: 6px;
  vertical-align: middle;
}

.hidden-badge {
  font-size: 10px;
  font-weight: 600;
  color: #f56c6c;
  border: 1px solid #f56c6c;
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  margin-right: 6px;
  vertical-align: middle;
}

.card-title {
  font-family: var(--font-display);
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.35;
  letter-spacing: -0.01em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color var(--duration) var(--ease);
}

.blog-card:hover .card-title {
  color: var(--color-accent-hover);
}

.card-desc {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
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
  margin-top: var(--space-xs);
}

.card-meta {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.card-stats {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.stat-item {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.bookmark-badge {
  display: flex;
  align-items: center;
}
</style>

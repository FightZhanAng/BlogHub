<template>
  <div class="toc" v-if="headings.length">
    <h4 class="toc-title"><el-icon style="vertical-align:middle;margin-right:4px"><List /></el-icon> 目录</h4>
    <ul class="toc-list">
      <li
        v-for="(item, index) in headings"
        :key="index"
        :class="['toc-item', { 'is-active': index === activeIndex }]"
        :style="{ paddingLeft: (item.level - 1) * 16 + 'px' }"
      >
        <a :href="`#${item.id}`" @click.prevent="scrollTo(item.id)">
          {{ item.text }}
        </a>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  selector: {
    type: String,
    default: '.markdown-body',
  },
})

const headings = ref([])
const activeIndex = ref(-1)
let observer = null

function parseHeadings() {
  const container = document.querySelector(props.selector)
  if (!container) return

  const items = container.querySelectorAll('h1, h2, h3')
  headings.value = Array.from(items).map((el) => ({
    id: el.id || el.textContent.replace(/\s+/g, '-').toLowerCase(),
    text: el.textContent,
    level: parseInt(el.tagName[1]),
  }))

  items.forEach((el, i) => {
    if (!el.id) el.id = headings.value[i].id
  })

  // 用 IntersectionObserver 代替 scroll 事件
  observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          const idx = headings.value.findIndex((h) => h.id === entry.target.id)
          if (idx !== -1) activeIndex.value = idx
        }
      }
    },
    { rootMargin: '-80px 0px -60% 0px' }
  )

  items.forEach((el) => observer.observe(el))
}

function scrollTo(id) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

onMounted(() => {
  parseHeadings()
})

onUnmounted(() => {
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.toc {
  position: sticky;
  top: 20px;
}

.toc-title {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: var(--color-text);
}

.toc-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.toc-item {
  margin-bottom: 4px;
}

.toc-item a {
  display: block;
  padding: 4px 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
  text-decoration: none;
  border-radius: 4px;
  transition: all 0.2s;
}

.toc-item a:hover {
  color: var(--color-accent);
  background: var(--color-accent-light);
}

.toc-item.is-active a {
  color: var(--color-accent);
  font-weight: 500;
  background: var(--color-accent-light);
}
</style>

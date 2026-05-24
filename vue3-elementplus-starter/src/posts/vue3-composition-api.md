---
title: Vue 3 Composition API 入门指南
date: 2024-03-20
tags: [Vue, 前端, 教程]
description: 详解 Vue 3 的 Composition API，从 setup 到响应式系统的核心概念，附实例代码。
---

# Vue 3 Composition API 入门指南 🚀

Vue 3 引入了 **Composition API**，这是一种全新的组织组件逻辑的方式。

## 为什么需要 Composition API？

Options API 在组件变得复杂时，同一个功能的逻辑被分散在 `data`、`methods`、`computed` 等不同选项中：

```vue
<script>
export default {
  data() { return { count: 0 } },
  methods: { increment() { this.count++ } },
  computed: { double() { return this.count * 2 } }
}
</script>
```

Composition API 则允许我们按**功能**组织代码：

```vue
<script setup>
import { ref, computed } from 'vue'

const count = ref(0)
const increment = () => count.value++
const double = computed(() => count.value * 2)
</script>
```

## 核心 API

### `ref` — 响应式数据

```javascript
import { ref } from 'vue'

const count = ref(0)
console.log(count.value) // 0
count.value++
```

### `reactive` — 对象响应式

```javascript
import { reactive } from 'vue'

const state = reactive({
  name: 'Vue',
  version: 3
})
```

### `computed` — 计算属性

```javascript
const doubled = computed(() => count.value * 2)
```

### `watch` — 侦听器

```javascript
watch(count, (newVal, oldVal) => {
  console.log(`从 ${oldVal} 变为了 ${newVal}`)
})
```

## 逻辑复用 (Composables)

Composition API 最大的优势是逻辑复用。把功能提取为函数：

```javascript
// useMouse.js
import { ref, onMounted, onUnmounted } from 'vue'

export function useMouse() {
  const x = ref(0)
  const y = ref(0)

  function update(e) {
    x.value = e.pageX
    y.value = e.pageY
  }

  onMounted(() => window.addEventListener('mousemove', update))
  onUnmounted(() => window.removeEventListener('mousemove', update))

  return { x, y }
}
```

## 小结

Composition API 让 Vue 组件的逻辑组织更加灵活和可复用。强烈推荐新项目使用 `<script setup>` 语法！

<template>
  <div class="tag-selector">
    <!-- 已选标签 -->
    <div class="selected-tags">
      <el-tag
        v-for="(tag, idx) in selectedTags"
        :key="idx"
        closable
        size="small"
        :disable-transitions="false"
        @close="removeTag(idx)"
      >
        {{ tag }}
      </el-tag>
    </div>

    <!-- 标签输入 -->
    <el-autocomplete
      v-model="inputValue"
      :fetch-suggestions="searchTags"
      :trigger-on-focus="true"
      placeholder="输入标签名，回车添加"
      size="small"
      class="tag-input"
      @keydown.enter.prevent="addTag"
      @keydown.,.prevent="addTag"
      @select="handleSelect"
      clearable
    >
      <template #default="{ item }">
        <div class="tag-suggestion">
          <span>{{ item.value }}</span>
          <span class="tag-count">{{ item.count }} 篇</span>
        </div>
      </template>
    </el-autocomplete>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { tagApi } from '@/api/tagApi'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const inputValue = ref('')
const allTags = ref([])

/** 当前已选标签（数组） */
const selectedTags = computed({
  get: () => {
    if (!props.modelValue) return []
    return props.modelValue.split(',').map((s) => s.trim()).filter(Boolean)
  },
  set: (arr) => {
    emit('update:modelValue', arr.join(','))
  },
})

/** 加载全部标签作为候选 */
onMounted(async () => {
  try {
    const res = await tagApi.list()
    if (Array.isArray(res)) {
      allTags.value = res
    }
  } catch {
    // 静默失败，不影响编辑
  }
})

/** 搜索标签（autocomplete 回调） */
function searchTags(query, cb) {
  const q = query.toLowerCase().trim()
  const filtered = q
    ? allTags.value.filter((t) => t.name.toLowerCase().includes(q))
    : allTags.value

  const suggestions = filtered.map((t) => ({
    value: t.name,
    count: t.postCount || 0,
  }))

  // 如果输入了全新标签（不在已有列表中），也作为选项
  if (q && !allTags.value.some((t) => t.name.toLowerCase() === q)) {
    suggestions.unshift({ value: q, count: 0 })
  }

  cb(suggestions)
}

/** 从 autocomplete 下拉选中 */
function handleSelect(item) {
  addTagByName(item.value)
}

/** 回车/逗号添加标签 */
function addTag() {
  const val = inputValue.value.trim()
  if (val) {
    addTagByName(val)
  }
  inputValue.value = ''
}

/** 添加单个标签 */
function addTagByName(name) {
  const trimmed = name.trim()
  if (!trimmed) return
  const current = selectedTags.value
  if (!current.includes(trimmed)) {
    selectedTags.value = [...current, trimmed]
  }
}

/** 删除标签 */
function removeTag(idx) {
  const current = selectedTags.value
  current.splice(idx, 1)
  selectedTags.value = [...current]
}
</script>

<style scoped>
.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-input {
  width: 200px;
}

.tag-suggestion {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.tag-count {
  font-size: 12px;
  color: #909399;
}
</style>

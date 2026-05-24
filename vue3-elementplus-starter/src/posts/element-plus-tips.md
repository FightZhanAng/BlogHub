---
title: Element Plus 实用技巧 10 则
date: 2024-03-25
tags: [Element Plus, UI, 前端]
description: 收集了 Element Plus 日常开发中最实用的 10 个小技巧，提升你的开发效率。
---

# Element Plus 实用技巧 10 则 🎯

日常开发中积累的一些 Element Plus 使用技巧，分享给大家。

## 1. 图标自动注册

在 `main.js` 中批量注册所有图标，就可以在模板中直接使用：

```javascript
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
```

## 2. 表格空数据自定义

```vue
<el-table :data="data" empty-text="暂无数据">
  <template #empty>
    <el-empty description="还没有数据哦" />
  </template>
</el-table>
```

## 3. 表单校验技巧

```vue
<el-form :model="form" :rules="rules" ref="formRef">
  <el-form-item label="邮箱" prop="email">
    <el-input v-model="form.email" />
  </el-form-item>
</el-form>
```

```javascript
const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}
```

## 4. 弹窗表单分离

大型表单放在 Dialog 里时，用 `v-if` 控制每次打开都是全新实例：

```vue
<el-dialog v-model="visible" v-if="visible">
  <!-- 表单内容 -->
</el-dialog>
```

## 5. 按需加载优化

使用 `unplugin-vue-components` 和 `unplugin-auto-import` 实现按需加载，减少打包体积。

## 6. $message 防重复

```javascript
let messageInstance = null
function showMessage(msg) {
  if (messageInstance) messageInstance.close()
  messageInstance = ElMessage.success(msg)
}
```

## 7. 日期快捷选择

```vue
<el-date-picker v-model="date" type="daterange"
  :shortcuts="shortcuts" />
```

```javascript
const shortcuts = [{
  text: '最近一周',
  value: () => [new Date(), new Date(Date.now() - 7 * 86400000)]
}]
```

## 8. 表格行样式

```vue
<el-table :row-class-name="tableRowClassName">
```

```javascript
function tableRowClassName({ rowIndex }) {
  return rowIndex % 2 === 0 ? 'even-row' : ''
}
```

## 9. 下拉加载更多

结合 `InfiniteScroll` 指令实现：

```vue
<div v-infinite-scroll="loadMore" infinite-scroll-distance="100">
  <el-card v-for="item in list">{{ item }}</el-card>
</div>
```

## 10. 主题定制

用 CSS 变量覆盖：

```css
:root {
  --el-color-primary: #409eff;
  --el-border-radius-base: 8px;
}
```

## 总结

Element Plus 组件库功能丰富，掌握这些小技巧能让开发效率翻倍！

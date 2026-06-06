<template>
  <div class="menu-settings">
    <div class="settings-header">
      <h2>菜单配置</h2>
      <p>管理左侧导航栏的菜单分组和菜单项，拖拽可调整顺序</p>
    </div>

    <div class="settings-body">
      <!-- 左栏：菜单组 -->
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">{{ sortMode ? '全局排序' : '菜单分组' }}</span>
          <div class="panel-header-btns">
            <el-button
              size="small"
              :type="sortMode ? 'primary' : 'default'"
              @click="sortMode = !sortMode"
            >
              <el-icon><Sort /></el-icon> {{ sortMode ? '退出排序' : '全局排序' }}
            </el-button>
            <el-button v-if="!sortMode" type="primary" size="small" @click="openGroupDialog()">
              <el-icon><Plus /></el-icon> 新增
            </el-button>
          </div>
        </div>

        <!-- 全局排序模式 -->
        <div v-if="sortMode" class="panel-list">
          <div
            v-for="(entry, idx) in unifiedList"
            :key="entry._key"
            class="list-item"
            :class="{ dragging: dragUnifiedIndex === idx }"
            draggable="true"
            @dragstart="onUnifiedDragStart(idx, $event)"
            @dragover.prevent
            @drop="onUnifiedDrop(idx)"
            @dragend="dragUnifiedIndex = null"
          >
            <div class="item-drag"><el-icon><Rank /></el-icon></div>
            <div class="item-info">
              <div class="item-name">
                <el-tag :type="entry.type === 'group' ? 'primary' : 'info'" size="small" style="margin-right: 6px;">
                  {{ entry.type === 'group' ? '分组' : '菜单' }}
                </el-tag>
                {{ entry.name }}
              </div>
              <div class="item-meta">
                <el-icon v-if="entry.icon"><component :is="entry.icon" /></el-icon>
                <span v-if="entry.path">{{ entry.path }}</span>
              </div>
            </div>
            <div class="item-actions">
              <el-switch
                :model-value="entry.enabled === 1"
                size="small"
                disabled
              />
            </div>
          </div>
          <el-empty v-if="!unifiedList.length" description="暂无数据" :image-size="60" />
        </div>

        <!-- 分组管理模式 -->
        <div v-else class="panel-list">
          <div
            v-for="(group, idx) in groups"
            :key="group.id"
            class="list-item"
            :class="{ active: selectedGroupId === group.id, dragging: dragGroupIndex === idx }"
            draggable="true"
            @dragstart="onGroupDragStart(idx, $event)"
            @dragover.prevent="onGroupDragOver(idx, $event)"
            @drop="onGroupDrop(idx)"
            @dragend="onGroupDragEnd"
            @click="selectGroup(group)"
          >
            <div class="item-drag"><el-icon><Rank /></el-icon></div>
            <div class="item-info">
              <div class="item-name">{{ group.name }}</div>
              <div class="item-meta">
                <el-icon v-if="group.icon"><component :is="group.icon" /></el-icon>
                <el-tag v-if="group.adminOnly" type="warning" size="small">管理员</el-tag>
              </div>
            </div>
            <div class="item-actions">
              <el-switch
                :model-value="group.enabled === 1"
                size="small"
                @change="toggleGroupEnabled(group)"
                @click.stop
              />
              <el-button text size="small" @click.stop="openGroupDialog(group)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button text size="small" type="danger" @click.stop="deleteGroup(group)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
          <el-empty v-if="!groups.length" description="暂无菜单组" :image-size="60" />
        </div>
      </div>

      <!-- 右栏：菜单项 -->
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">
            {{ selectedGroup ? selectedGroup.name + ' — 菜单项' : '独立顶级菜单项' }}
          </span>
          <div class="panel-header-btns">
            <el-button
              size="small"
              :type="showTopLevel ? 'primary' : 'default'"
              @click="toggleTopLevel"
            >
              {{ showTopLevel ? '查看分组项' : '独立顶级项' }}
            </el-button>
            <el-button type="primary" size="small" @click="openItemDialog()">
              <el-icon><Plus /></el-icon> 新增
            </el-button>
          </div>
        </div>
        <div class="panel-list">
          <div
            v-for="(item, idx) in currentItems"
            :key="item.id"
            class="list-item"
            :class="{ dragging: dragItemIndex === idx }"
            draggable="true"
            @dragstart="onItemDragStart(idx, $event)"
            @dragover.prevent="onItemDragOver(idx, $event)"
            @drop="onItemDrop(idx)"
            @dragend="onItemDragEnd"
          >
            <div class="item-drag"><el-icon><Rank /></el-icon></div>
            <div class="item-info">
              <div class="item-name">{{ item.title }}</div>
              <div class="item-meta">{{ item.path }}</div>
            </div>
            <div class="item-actions">
              <el-switch
                :model-value="item.enabled === 1"
                size="small"
                @change="toggleItemEnabled(item)"
              />
              <el-tag v-if="item.adminOnly" type="warning" size="small">管理员</el-tag>
              <el-button text size="small" @click="openItemDialog(item)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button text size="small" type="danger" @click="deleteItem(item)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
          <el-empty v-if="!currentItems.length" description="暂无菜单项" :image-size="60" />
        </div>
      </div>
    </div>

    <!-- 菜单组编辑弹窗 -->
    <el-dialog v-model="groupDialogVisible" :title="editingGroup ? '编辑菜单组' : '新增菜单组'" width="440px">
      <el-form label-width="90px">
        <el-form-item label="分组名称">
          <el-input v-model="groupForm.name" placeholder="如：博客、工具" />
        </el-form-item>
        <el-form-item label="图标">
          <el-select
            v-model="groupForm.icon"
            placeholder="选择图标"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="icon in iconOptions"
              :key="icon"
              :label="icon"
              :value="icon"
            >
              <el-icon style="margin-right: 8px; vertical-align: middle;"><component :is="icon" /></el-icon>
              <span>{{ icon }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="仅管理员">
          <el-switch v-model="groupForm.adminOnly" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="groupForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGroup">保存</el-button>
      </template>
    </el-dialog>

    <!-- 菜单项编辑弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="editingItem ? '编辑菜单项' : '新增菜单项'" width="440px">
      <el-form label-width="90px">
        <el-form-item label="菜单标题">
          <el-input v-model="itemForm.title" placeholder="如：博客列表" />
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="itemForm.path" placeholder="如：/blog、/ai-assistant" />
        </el-form-item>
        <el-form-item label="图标">
          <el-select
            v-model="itemForm.icon"
            placeholder="选择图标"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="icon in iconOptions"
              :key="icon"
              :label="icon"
              :value="icon"
            >
              <el-icon style="margin-right: 8px; vertical-align: middle;"><component :is="icon" /></el-icon>
              <span>{{ icon }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="所属分组">
          <el-select v-model="itemForm.groupId" placeholder="独立顶级项（无分组）" clearable>
            <el-option
              v-for="g in groups"
              :key="g.id"
              :label="g.name"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="仅管理员">
          <el-switch v-model="itemForm.adminOnly" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="itemForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { menuSettingsApi } from '@/api/menuSettingsApi'
import { useMenuStore } from '@/stores/menu'
import { ElMessage, ElMessageBox } from 'element-plus'

const menuStore = useMenuStore()

// 常用菜单图标列表
const iconOptions = [
  'HomeFilled', 'House', 'Notebook', 'Document', 'EditPen', 'Collection', 'CollectionTag',
  'Files', 'Folder', 'FolderOpened', 'Reading', 'DataLine', 'DataAnalysis', 'TrendCharts',
  'User', 'UserFilled', 'Avatar', 'Stamp', 'Postcard',
  'Star', 'StarFilled', 'Trophy', 'Medal', 'Award',
  'Compass', 'Location', 'MapLocation', 'Promotion', 'Position',
  'SetUp', 'Setting', 'Tools', 'Cpu', 'Monitor', 'Platform',
  'ChatDotRound', 'ChatDotSquare', 'ChatLineRound', 'Comment', 'Bell',
  'Picture', 'Camera', 'VideoCamera', 'Mic', 'Headset',
  'Search', 'ZoomIn', 'View', 'Timer', 'Clock',
  'Link', 'Connection', 'Share', 'Upload', 'Download',
  'Lock', 'Unlock', 'Key', 'Shield', 'Warning',
  'CirclePlus', 'Plus', 'Minus', 'Close', 'Check',
  'InfoFilled', 'QuestionFilled', 'SuccessFilled', 'WarningFilled',
  'Menu', 'Grid', 'List', 'Operation', 'More',
  'Calendar', 'Flag', 'Sunrise', 'Sunset', 'Sunny',
]

// ========== 数据 ==========
const groups = ref([])
const itemsMap = ref({})  // groupId → items[]
const topLevelItems = ref([])  // 独立顶级项
const selectedGroupId = ref(null)
const showTopLevel = ref(false)

const selectedGroup = computed(() => groups.value.find(g => g.id === selectedGroupId.value))
const currentItems = computed(() => {
  if (showTopLevel.value) return topLevelItems.value
  if (!selectedGroupId.value) return []
  return itemsMap.value[selectedGroupId.value] || []
})

// ========== 全局排序 ==========
const sortMode = ref(false)
const dragUnifiedIndex = ref(null)

// 合并独立顶级项 + 菜单组为统一列表
const unifiedList = computed(() => {
  const items = topLevelItems.value.map(i => ({
    _key: 'item-' + i.id,
    type: 'item',
    id: i.id,
    name: i.title,
    icon: i.icon,
    path: i.path,
    enabled: i.enabled,
    sortOrder: i.sortOrder,
  }))
  const gs = groups.value.map(g => ({
    _key: 'group-' + g.id,
    type: 'group',
    id: g.id,
    name: g.name,
    icon: g.icon,
    path: null,
    enabled: g.enabled,
    sortOrder: g.sortOrder,
  }))
  // 按 sortOrder 合并排序
  return [...items, ...gs].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
})

function onUnifiedDragStart(idx) {
  dragUnifiedIndex.value = idx
}

async function onUnifiedDrop(targetIdx) {
  if (dragUnifiedIndex.value === null || dragUnifiedIndex.value === targetIdx) return
  const arr = [...unifiedList.value]
  const [moved] = arr.splice(dragUnifiedIndex.value, 1)
  arr.splice(targetIdx, 0, moved)

  // 生成排序请求
  const entries = arr.map(e => ({ type: e.type, id: e.id }))
  try {
    await menuSettingsApi.sortAll(entries)
    await loadAll()
    refreshSidebar()
    ElMessage.success('排序已保存')
  } catch {
    ElMessage.error('排序保存失败')
  }
  dragUnifiedIndex.value = null
}

// ========== 加载数据 ==========
async function loadGroups() {
  groups.value = await menuSettingsApi.getGroups()
  if (!selectedGroupId.value && groups.value.length) {
    selectedGroupId.value = groups.value[0].id
  }
}

async function loadItems() {
  const allItems = await menuSettingsApi.getAllItems()
  // 按 groupId 分组
  const map = {}
  const top = []
  for (const item of allItems) {
    if (item.groupId == null) {
      top.push(item)
    } else {
      if (!map[item.groupId]) map[item.groupId] = []
      map[item.groupId].push(item)
    }
  }
  itemsMap.value = map
  topLevelItems.value = top
}

async function loadAll() {
  await Promise.all([loadGroups(), loadItems()])
}

function selectGroup(group) {
  selectedGroupId.value = group.id
  showTopLevel.value = false
}

function toggleTopLevel() {
  showTopLevel.value = !showTopLevel.value
}

// ========== 刷新侧边栏 ==========
function refreshSidebar() {
  menuStore.fetchMenuTree()
}

// ========== 菜单组 CRUD ==========
const groupDialogVisible = ref(false)
const editingGroup = ref(null)
const groupForm = reactive({ name: '', icon: '', adminOnly: false, enabled: true })

function openGroupDialog(group) {
  if (group) {
    editingGroup.value = group
    groupForm.name = group.name
    groupForm.icon = group.icon || ''
    groupForm.adminOnly = group.adminOnly === 1
    groupForm.enabled = group.enabled === 1
  } else {
    editingGroup.value = null
    groupForm.name = ''
    groupForm.icon = ''
    groupForm.adminOnly = false
    groupForm.enabled = true
  }
  groupDialogVisible.value = true
}

async function saveGroup() {
  if (!groupForm.name) {
    ElMessage.warning('请输入分组名称')
    return
  }
  const data = {
    name: groupForm.name,
    icon: groupForm.icon || null,
    adminOnly: groupForm.adminOnly ? 1 : 0,
    enabled: groupForm.enabled ? 1 : 0,
  }
  try {
    if (editingGroup.value) {
      await menuSettingsApi.updateGroup(editingGroup.value.id, data)
    } else {
      await menuSettingsApi.createGroup(data)
    }
    groupDialogVisible.value = false
    await loadAll()
    refreshSidebar()
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  }
}

async function toggleGroupEnabled(group) {
  const newVal = group.enabled === 1 ? 0 : 1
  await menuSettingsApi.updateGroup(group.id, { enabled: newVal })
  await loadAll()
  refreshSidebar()
}

async function deleteGroup(group) {
  try {
    await ElMessageBox.confirm(`删除分组"${group.name}"将同时删除其下所有菜单项，确定？`, '提示', { type: 'warning' })
    await menuSettingsApi.deleteGroup(group.id)
    if (selectedGroupId.value === group.id) {
      selectedGroupId.value = groups.value.length ? groups.value[0].id : null
    }
    await loadAll()
    refreshSidebar()
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// ========== 菜单项 CRUD ==========
const itemDialogVisible = ref(false)
const editingItem = ref(null)
const itemForm = reactive({ title: '', path: '', icon: '', groupId: null, adminOnly: false, enabled: true })

function openItemDialog(item) {
  if (item) {
    editingItem.value = item
    itemForm.title = item.title
    itemForm.path = item.path
    itemForm.icon = item.icon || ''
    itemForm.groupId = item.groupId
    itemForm.adminOnly = item.adminOnly === 1
    itemForm.enabled = item.enabled === 1
  } else {
    editingItem.value = null
    itemForm.title = ''
    itemForm.path = ''
    itemForm.icon = ''
    itemForm.groupId = showTopLevel.value ? null : selectedGroupId.value
    itemForm.adminOnly = false
    itemForm.enabled = true
  }
  itemDialogVisible.value = true
}

async function saveItem() {
  if (!itemForm.title || !itemForm.path) {
    ElMessage.warning('请输入标题和路径')
    return
  }
  const data = {
    title: itemForm.title,
    path: itemForm.path,
    icon: itemForm.icon || null,
    groupId: itemForm.groupId || null,
    adminOnly: itemForm.adminOnly ? 1 : 0,
    enabled: itemForm.enabled ? 1 : 0,
  }
  try {
    if (editingItem.value) {
      await menuSettingsApi.updateItem(editingItem.value.id, data)
    } else {
      await menuSettingsApi.createItem(data)
    }
    itemDialogVisible.value = false
    await loadItems()
    refreshSidebar()
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  }
}

async function toggleItemEnabled(item) {
  const newVal = item.enabled === 1 ? 0 : 1
  await menuSettingsApi.updateItem(item.id, { enabled: newVal })
  await loadItems()
  refreshSidebar()
}

async function deleteItem(item) {
  try {
    await ElMessageBox.confirm(`确定删除菜单项"${item.title}"？`, '提示', { type: 'warning' })
    await menuSettingsApi.deleteItem(item.id)
    await loadItems()
    refreshSidebar()
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// ========== 拖拽排序（菜单组） ==========
const dragGroupIndex = ref(null)

function onGroupDragStart(idx) {
  dragGroupIndex.value = idx
}
function onGroupDragOver(idx, e) {
  e.dataTransfer.dropEffect = 'move'
}
async function onGroupDrop(targetIdx) {
  if (dragGroupIndex.value === null || dragGroupIndex.value === targetIdx) return
  const arr = [...groups.value]
  const [moved] = arr.splice(dragGroupIndex.value, 1)
  arr.splice(targetIdx, 0, moved)
  groups.value = arr
  await menuSettingsApi.sortGroups(arr.map(g => g.id))
  refreshSidebar()
}
function onGroupDragEnd() {
  dragGroupIndex.value = null
}

// ========== 拖拽排序（菜单项） ==========
const dragItemIndex = ref(null)

function onItemDragStart(idx) {
  dragItemIndex.value = idx
}
function onItemDragOver(idx, e) {
  e.dataTransfer.dropEffect = 'move'
}
async function onItemDrop(targetIdx) {
  if (dragItemIndex.value === null || dragItemIndex.value === targetIdx) return
  const arr = [...currentItems.value]
  const [moved] = arr.splice(dragItemIndex.value, 1)
  arr.splice(targetIdx, 0, moved)
  // 更新本地数据
  if (showTopLevel.value) {
    topLevelItems.value = arr
  } else if (selectedGroupId.value) {
    itemsMap.value[selectedGroupId.value] = arr
  }
  await menuSettingsApi.sortItems(arr.map(i => i.id))
  refreshSidebar()
}
function onItemDragEnd() {
  dragItemIndex.value = null
}

// ========== 初始化 ==========
onMounted(loadAll)
</script>

<style scoped>
.menu-settings {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.settings-header {
  margin-bottom: 16px;
}

.settings-header h2 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #303133;
}

.settings-header p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.settings-body {
  flex: 1;
  display: flex;
  gap: 16px;
  overflow: hidden;
}

.panel {
  flex: 1;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.panel-header-btns {
  display: flex;
  gap: 8px;
}

.panel-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.list-item:hover {
  background: #f5f7fa;
  border-color: #e4e7ed;
}

.list-item.active {
  background: #ecf5ff;
  border-color: #c6e2ff;
}

.list-item.dragging {
  opacity: 0.5;
  background: #f0f5ff;
}

.item-drag {
  color: #c0c4cc;
  cursor: grab;
  flex-shrink: 0;
}

.item-drag:active {
  cursor: grabbing;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.item-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
</style>

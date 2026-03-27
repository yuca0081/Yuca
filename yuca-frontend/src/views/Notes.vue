<template>
  <!-- 第1层：页面容器（暗色遮罩背景） -->
  <div class="notes-page-container">
    <!-- 返回主页按钮 -->
    <n-button class="back-home-btn" @click="goBackHome">
      <template #icon>
        <n-icon :component="ArrowBackOutline" size="18" />
      </template>
      <span>返回主页</span>
    </n-button>

    <!-- 第2层：页面主体容器（毛玻璃卡片） -->
    <div class="notes-main-card">
      <!-- 左侧：侧栏 (可调整宽度，可折叠) -->
      <div class="notes-sidebar" :class="{ collapsed: isCollapsed }" :style="{ width: sidebarWidth + '%' }">
        <!-- 用户区 -->
        <div class="sidebar-top" v-show="!isCollapsed">
          <div class="user-section">
            <div class="user-avatar">
              <img v-if="userAvatar" :src="userAvatar" alt="用户头像" class="avatar-img" />
              <span v-else>{{ userInitial }}</span>
            </div>
            <div class="create-icon" @click="showCreateBookModal = true">
              <n-icon :component="AddCircleOutline" size="24" />
            </div>
          </div>
        </div>

        <!-- 搜索栏 -->
        <div class="sidebar-search" v-show="!isCollapsed">
          <div class="search-box">
            <n-icon :component="SearchOutline" class="search-icon" size="16" />
            <input
              ref="searchInputRef"
              type="text"
              placeholder="搜索笔记本"
              class="search-input"
              v-model="searchQuery"
            />
          </div>
        </div>

        <!-- 导航菜单 -->
        <div class="sidebar-nav">
          <div
            v-for="item in navItems"
            :key="item.key"
            class="nav-item"
            :class="{ active: activeNav === item.key }"
            @click="handleNavClick(item.key)"
          >
            <n-icon :component="item.icon" class="nav-icon" size="20" />
            <span v-show="!isCollapsed">{{ item.label }}</span>
          </div>

          <!-- 笔记本子菜单 -->
          <div v-if="activeNav === 'notebooks' && !isCollapsed" class="nb-submenu">
            <div
              v-for="book in filteredBooks"
              :key="book.id"
              class="nb-item"
              :class="{ active: selectedBookId === book.id }"
              @click="selectNoteBook(book.id)"
            >
              <span>• {{ book.name }}</span>
              <span v-if="book.documentCount" class="doc-count">{{ book.documentCount }}</span>
            </div>
            <div v-if="filteredBooks.length === 0" class="nb-empty-state">
              暂无笔记本
            </div>
          </div>
        </div>

        <!-- 创建按钮已移至顶部 -->
      </div>

      <!-- 可拖动分隔条（带折叠按钮） -->
      <div
        class="resize-divider"
        @mousedown="startResize"
        @dblclick="resetWidth"
      >
        <div class="divider-line"></div>
        <div class="collapse-toggle-btn" @click.stop="toggleSidebar">
          <n-icon :component="isCollapsed ? ChevronForwardOutline : ChevronBackOutline" size="14" />
        </div>
      </div>

      <!-- 右侧：内容区 -->
      <div class="notes-content">
        <!-- 笔记本列表视图 -->
        <div v-if="activeView === 'notebooks'" class="view-notebooks">
          <!-- 页面头部 -->
          <div class="content-page-header">
            <div class="header-left">
              <h2 class="page-title">笔记本</h2>
            </div>
            <div class="header-right">
              <div class="view-toggle">
                <n-button
                  text
                  size="small"
                  :class="{ active: viewMode === 'grid' }"
                  @click="viewMode = 'grid'"
                >
                  <template #icon>
                    <n-icon :component="GridOutline" size="18" />
                  </template>
                </n-button>
                <n-button
                  text
                  size="small"
                  :class="{ active: viewMode === 'list' }"
                  @click="viewMode = 'list'"
                >
                  <template #icon>
                    <n-icon :component="ListOutline" size="18" />
                  </template>
                </n-button>
              </div>
            </div>
          </div>

          <!-- 常用分组 -->
          <div class="nb-section" v-if="commonBooks.length > 0">
            <div class="section-header">
              <h3 class="section-title">常用</h3>
              <div class="collapse-btn" @click="toggleCommonCollapse">
                <span>{{ commonCollapsed ? '展开' : '收起' }}</span>
                <n-icon :component="commonCollapsed ? ChevronDownOutline : ChevronUpOutline" size="14" />
              </div>
            </div>

            <div v-show="!commonCollapsed" :class="viewMode === 'grid' ? 'nb-grid' : 'nb-list'">
              <NoteBookCard
                v-for="book in commonBooks"
                :key="book.id"
                :book="book"
                :mode="viewMode"
                :recent-docs="getBookRecentDocs(book.id)"
                @click="openNoteBook(book)"
              />
            </div>
          </div>

          <!-- 我的笔记本分组 -->
          <div class="nb-section">
            <div v-if="filteredBooks.length === 0" class="empty-nb-state">
              <n-empty description="暂无笔记本，点击左侧创建按钮开始创建" />
            </div>
            <div v-else :class="viewMode === 'grid' ? 'nb-grid' : 'nb-list'">
              <NoteBookCard
                v-for="book in filteredBooks"
                :key="book.id"
                :book="book"
                :mode="viewMode"
                :recent-docs="getBookRecentDocs(book.id)"
                @click="openNoteBook(book)"
              />
            </div>
          </div>
        </div>

        <!-- 编辑器视图 -->
        <div v-else-if="activeView === 'editor'" class="view-editor">
          <!-- 左侧：树形结构 -->
          <div class="editor-sidebar" :style="{ width: editorSidebarWidth + 'px' }">
            <div class="editor-sidebar-header">
              <h3 class="sidebar-title">{{ currentBook?.name }}</h3>
              <n-button text size="small" @click="activeView = 'notebooks'">
                <template #icon>
                  <n-icon :component="ArrowBackOutline" />
                </template>
                返回
              </n-button>
            </div>
            <div class="editor-sidebar-toolbar">
              <n-button size="small" @click="onClickCreateFolder">
                <template #icon>
                  <n-icon :component="FolderOutline" />
                </template>
                新建文件夹
              </n-button>
              <n-button size="small" @click="onClickCreateDocument">
                <template #icon>
                  <n-icon :component="DocumentTextOutline" />
                </template>
                新建文档
              </n-button>
            </div>
            <div class="tree-container">
              <NoteTree
                v-if="selectedBookId"
                :items="noteTree"
                :active-id="activeItemId"
                @click="handleTreeClick"
                @create-folder="handleCreateFolder"
                @create-document="handleCreateDocument"
                @delete="handleDeleteItem"
              />
              <n-empty v-else description="请选择笔记本" />
            </div>
          </div>

          <!-- 可拖动分隔条 -->
          <div
            class="editor-resize-divider"
            @mousedown="startEditorResize"
          >
            <div class="divider-line"></div>
          </div>

          <!-- 右侧：编辑器 -->
          <div class="editor-main">
            <div v-if="currentItem" class="editor-container">
              <div class="editor-toolbar">
                <input
                  v-model="editingTitle"
                  class="title-input"
                  placeholder="文档标题"
                  @blur="handleTitleChange"
                />
                <div class="toolbar-actions">
                  <n-button text size="small" @click="handleTogglePin">
                    <template #icon>
                      <n-icon :component="currentItem.isPinned ? PushOutline : PushOutline" />
                    </template>
                    {{ currentItem.isPinned ? '取消置顶' : '置顶' }}
                  </n-button>
                </div>
              </div>
              <NoteEditor
                v-model="currentContent"
                :item-id="currentItem.id"
                @save="handleEditorSave"
              />
            </div>
            <n-empty v-else description="请选择或创建文档" />
          </div>
        </div>
      </div>
    </div>

    <!-- 创建笔记本弹窗 -->
    <n-modal v-model:show="showCreateBookModal" preset="card" title="创建笔记本" class="create-modal" style="width: 500px">
      <n-form ref="createFormRef" :model="createForm" :rules="createRules" label-placement="left" label-width="80">
        <n-form-item label="名称" path="name">
          <n-input v-model:value="createForm.name" placeholder="请输入笔记本名称" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input
            v-model:value="createForm.description"
            type="textarea"
            placeholder="请输入笔记本描述"
            :rows="3"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <n-button @click="showCreateBookModal = false">取消</n-button>
          <n-button type="primary" @click="handleCreateBook" :loading="createLoading">创建</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 创建文件夹弹窗 -->
    <n-modal v-model:show="showCreateFolderModal" preset="card" title="新建文件夹" class="create-modal" style="width: 400px">
      <n-form ref="createFolderFormRef" :model="createFolderForm" :rules="createFolderRules" label-placement="left" label-width="80">
        <n-form-item label="文件夹名称" path="name">
          <n-input v-model:value="createFolderForm.name" placeholder="请输入文件夹名称" @keyup.enter="handleConfirmCreateFolder" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <n-button @click="showCreateFolderModal = false">取消</n-button>
          <n-button type="primary" @click="handleConfirmCreateFolder" :loading="createFolderLoading">创建</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 创建文档弹窗 -->
    <n-modal v-model:show="showCreateDocumentModal" preset="card" title="新建文档" class="create-modal" style="width: 400px">
      <n-form ref="createDocumentFormRef" :model="createDocumentForm" :rules="createDocumentRules" label-placement="left" label-width="80">
        <n-form-item label="文档标题" path="title">
          <n-input v-model:value="createDocumentForm.title" placeholder="请输入文档标题" @keyup.enter="handleConfirmCreateDocument" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <n-button @click="showCreateDocumentModal = false">取消</n-button>
          <n-button type="primary" @click="handleConfirmCreateDocument" :loading="createDocumentLoading">创建</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, h } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NIcon,
  NEmpty,
  NModal,
  NForm,
  NFormItem,
  NInput,
  useMessage
} from 'naive-ui'
import { useNoteStore } from '@/stores/note'
import NoteBookCard from '@/components/NoteBookCard.vue'
import NoteTree from '@/components/NoteTree.vue'
import NoteEditor from '@/components/NoteEditor.vue'
import type { NoteBook, NoteItem } from '@/types/note'
import {
  AddOutline,
  AddCircleOutline,
  ArrowBackOutline,
  ChevronForwardOutline,
  ChevronBackOutline,
  ChevronDownOutline,
  ChevronUpOutline,
  NotificationsOutline,
  SearchOutline,
  GridOutline,
  ListOutline,
  FolderOutline,
  DocumentTextOutline,
  PushOutline
} from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()
const noteStore = useNoteStore()

// ========== 侧栏相关 ==========
const sidebarWidth = ref(20)
const isResizing = ref(false)
const startX = ref(0)
const startWidth = ref(20)
const isCollapsed = ref(false)
const savedWidth = ref(20)

// 编辑器侧栏相关
const editorSidebarWidth = ref(280)
const isEditorResizing = ref(false)
const editorStartX = ref(0)
const editorStartWidth = ref(280)

// 用户信息
const userInitial = computed(() => {
  const userStr = localStorage.getItem('user_info')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      return user.username ? user.username.charAt(0).toUpperCase() : 'U'
    } catch {
      return 'U'
    }
  }
  return 'U'
})

const userAvatar = computed(() => {
  // 从 localStorage 获取缓存的头像 base64
  const cachedAvatar = localStorage.getItem('user_avatar')
  if (cachedAvatar) {
    return cachedAvatar
  }
  return null
})

// 搜索
const searchInputRef = ref<HTMLInputElement>()
const searchQuery = ref('')

// 导航菜单
const navItems = ref([
  {
    key: 'notebooks',
    label: '笔记本',
    icon: () => h(NIcon, null, { default: () => h(DocumentTextOutline) })
  }
])
const activeNav = ref('notebooks')

// ========== 视图相关 ==========
const activeView = ref<'notebooks' | 'editor'>('notebooks')
const viewMode = ref<'grid' | 'list'>('grid')
const commonCollapsed = ref(false)

// ========== 数据相关 ==========
const selectedBookId = ref<number | null>(null)
const activeItemId = ref<number | null>(null)
const currentContent = ref('')
const editingTitle = ref('')

// 创建笔记本弹窗
const showCreateBookModal = ref(false)
const createLoading = ref(false)
const createForm = ref({
  name: '',
  description: ''
})
const createRules = {
  name: { required: true, message: '请输入笔记本名称', trigger: 'blur' }
}

// 创建文件夹弹窗
const showCreateFolderModal = ref(false)
const createFolderLoading = ref(false)
const createFolderForm = ref({
  name: ''
})
const createFolderRules = {
  name: { required: true, message: '请输入文件夹名称', trigger: 'blur' }
}

// 创建文档弹窗
const showCreateDocumentModal = ref(false)
const createDocumentLoading = ref(false)
const createDocumentForm = ref({
  title: ''
})
const createDocumentRules = {
  title: { required: true, message: '请输入文档标题', trigger: 'blur' }
}
const currentParentId = ref<number | null>(null)

// ========== 计算属性 ==========
const noteBooks = computed(() => noteStore.noteBooks)
const currentBook = computed(() => noteStore.currentBook)
const noteTree = computed(() => noteStore.noteTree)
const currentItem = computed(() => noteStore.currentItem)

const commonBooks = computed(() => {
  return noteBooks.value
    .filter((book: NoteBook) => book.isFrequentlyUsed)
    .slice(0, 4)
})

const filteredBooks = computed(() => {
  let result = noteBooks.value
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter((book: NoteBook) =>
      book.name.toLowerCase().includes(query) ||
      (book.description && book.description.toLowerCase().includes(query))
    )
  }
  return result
})

// ========== 方法 ==========

// 处理导航菜单点击
const handleNavClick = (key: string) => {
  activeNav.value = key
  if (key === 'notebooks') {
    selectedBookId.value = null
    activeView.value = 'notebooks'
  }
}

// 侧栏折叠
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  if (isCollapsed.value) {
    savedWidth.value = sidebarWidth.value
  } else {
    sidebarWidth.value = savedWidth.value || 20
  }
}

// 选择笔记本
const selectNoteBook = async (bookId: number) => {
  selectedBookId.value = bookId
  activeView.value = 'editor'
  noteStore.currentBookId = bookId
  await noteStore.loadNoteTree(bookId)
}

// 打开笔记本
const openNoteBook = async (book: NoteBook) => {
  await selectNoteBook(book.id)
}

// 切换常用分组折叠
const toggleCommonCollapse = () => {
  commonCollapsed.value = !commonCollapsed.value
}

// 获取笔记本的最近文档
const getBookRecentDocs = (_bookId: number): NoteItem[] => {
  const tree = noteTree.value
  const docs: NoteItem[] = []

  const collectDocs = (items: NoteItem[]) => {
    items.forEach(item => {
      if (item.type === 'DOCUMENT') {
        docs.push(item)
      }
      if (item.children) {
        collectDocs(item.children)
      }
    })
  }

  collectDocs(tree)
  return docs.slice(0, 3)
}

// 创建笔记本
const handleCreateBook = async () => {
  try {
    createLoading.value = true
    await noteStore.createNoteBook({
      name: createForm.value.name,
      description: createForm.value.description
    })
    message.success('笔记本创建成功')
    showCreateBookModal.value = false
    createForm.value = { name: '', description: '' }
  } catch (error: any) {
    console.error('创建笔记本失败:', error)
    message.error(error.message || '创建笔记本失败')
  } finally {
    createLoading.value = false
  }
}

// 创建文件夹
const handleCreateFolder = async (parentId: number | null = null) => {
  if (!selectedBookId.value) return
  currentParentId.value = parentId
  createFolderForm.value.name = '新建文件夹'
  showCreateFolderModal.value = true
}

// 确认创建文件夹
const handleConfirmCreateFolder = async () => {
  if (!selectedBookId.value) return
  try {
    createFolderLoading.value = true
    await noteStore.createItem({
      bookId: selectedBookId.value,
      parentId: currentParentId.value,
      type: 'FOLDER',
      title: createFolderForm.value.name
    })
    message.success('文件夹创建成功')
    showCreateFolderModal.value = false
    createFolderForm.value.name = ''
  } catch (error: any) {
    message.error(error.message || '创建文件夹失败')
  } finally {
    createFolderLoading.value = false
  }
}

// 创建文档
const handleCreateDocument = async (parentId: number | null = null) => {
  if (!selectedBookId.value) return
  currentParentId.value = parentId
  createDocumentForm.value.title = '新建文档'
  showCreateDocumentModal.value = true
}

// 确认创建文档
const handleConfirmCreateDocument = async () => {
  if (!selectedBookId.value) return
  try {
    createDocumentLoading.value = true
    await noteStore.createItem({
      bookId: selectedBookId.value,
      parentId: currentParentId.value,
      type: 'DOCUMENT',
      title: createDocumentForm.value.title,
      content: '',
      contentType: 'MARKDOWN'
    })
    message.success('文档创建成功')
    showCreateDocumentModal.value = false
    createDocumentForm.value.title = ''
  } catch (error: any) {
    message.error(error.message || '创建文档失败')
  } finally {
    createDocumentLoading.value = false
  }
}

// 按钮点击包装函数（忽略事件参数）
const onClickCreateFolder = () => handleCreateFolder()
const onClickCreateDocument = () => handleCreateDocument()

// 删除项目
const handleDeleteItem = async (item: NoteItem) => {
  if (!confirm(`确定要删除"${item.title}"吗？`)) return
  try {
    await noteStore.deleteItem(item.id)
    message.success('删除成功')
    if (activeItemId.value === item.id) {
      activeItemId.value = null
      currentContent.value = ''
      noteStore.currentItem = null
    }
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

// 树节点点击
const handleTreeClick = async (item: NoteItem) => {
  if (item.type === 'FOLDER') {
    noteStore.toggleExpand(item.id)
    return
  }

  activeItemId.value = item.id
  editingTitle.value = item.title
  try {
    const data = await noteStore.loadItem(item.id)
    currentContent.value = data.content || ''
  } catch (error: any) {
    message.error(error.message || '加载文档失败')
  }
}

// 编辑器保存
const handleEditorSave = async (content: string) => {
  if (!currentItem.value) return
  try {
    await noteStore.updateItem(currentItem.value.id, { content })
  } catch (error: any) {
    message.error(error.message || '保存失败')
  }
}

// 标题修改
const handleTitleChange = async () => {
  if (!currentItem.value || !editingTitle.value.trim()) return
  try {
    await noteStore.updateItem(currentItem.value.id, { title: editingTitle.value })
  } catch (error: any) {
    message.error(error.message || '更新标题失败')
  }
}

// 置顶切换
const handleTogglePin = async () => {
  if (!currentItem.value) return
  try {
    await noteStore.togglePin(currentItem.value.id)
  } catch (error: any) {
    message.error(error.message || '操作失败')
  }
}

// 拖动调整宽度
const startResize = (e: MouseEvent) => {
  isResizing.value = true
  startX.value = e.clientX
  startWidth.value = sidebarWidth.value

  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
  e.preventDefault()
}

const onResize = (e: MouseEvent) => {
  if (!isResizing.value) return

  const mainCard = document.querySelector('.notes-main-card') as HTMLElement
  if (!mainCard) return

  const containerWidth = mainCard.offsetWidth
  const deltaX = e.clientX - startX.value
  const deltaPercent = (deltaX / containerWidth) * 100

  let newWidth = startWidth.value + deltaPercent
  newWidth = Math.max(15, Math.min(60, newWidth))

  sidebarWidth.value = newWidth
}

const stopResize = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}

const resetWidth = () => {
  sidebarWidth.value = 20
}

// 编辑器侧栏拖动
const startEditorResize = (e: MouseEvent) => {
  isEditorResizing.value = true
  editorStartX.value = e.clientX
  editorStartWidth.value = editorSidebarWidth.value

  document.addEventListener('mousemove', onEditorResize)
  document.addEventListener('mouseup', stopEditorResize)
  e.preventDefault()
}

const onEditorResize = (e: MouseEvent) => {
  if (!isEditorResizing.value) return

  const deltaX = e.clientX - editorStartX.value
  let newWidth = editorStartWidth.value + deltaX

  // 限制最小和最大宽度
  newWidth = Math.max(200, Math.min(600, newWidth))

  editorSidebarWidth.value = newWidth
}

const stopEditorResize = () => {
  isEditorResizing.value = false
  document.removeEventListener('mousemove', onEditorResize)
  document.removeEventListener('mouseup', stopEditorResize)
}

// 返回主页
const goBackHome = () => {
  router.push('/')
}

// 快捷键
const handleKeyDown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'j') {
    e.preventDefault()
    searchInputRef.value?.focus()
  }
}

onMounted(async () => {
  await noteStore.loadNoteBooks()
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('mousemove', onEditorResize)
  document.removeEventListener('mouseup', stopEditorResize)
})
</script>

<script lang="ts">
export default {
  name: 'Notes'
}
</script>

<style scoped>
/* ========== 第1层：页面容器 ========== */
.notes-page-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  min-height: 100vh;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7) 0%, rgba(30, 30, 30, 0.8) 100%) !important;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 1;
}

/* ========== 第2层：页面主体容器 ========== */
.notes-main-card {
  width: 93%;
  height: 85vh;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
  display: flex;
  overflow: hidden;
  animation: fadeInUp 0.5s ease-out;
  user-select: none;
}

/* ========== 返回主页按钮 ========== */
.back-home-btn {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 100;
  padding: 8px 14px !important;
  font-size: 13px !important;
  height: 36px !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(20px) !important;
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.2) !important;
  color: var(--color-text-primary) !important;
  transition: all 0.3s ease;
}

.back-home-btn:hover {
  background: rgba(255, 255, 255, 1) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(0, 0, 0, 0.25) !important;
}

/* ========== 左侧侧栏 ========== */
.notes-sidebar {
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
  min-width: 60px;
  max-width: 35%;
}

.notes-sidebar.collapsed {
  width: 60px !important;
}

.notes-sidebar.collapsed .sidebar-top,
.notes-sidebar.collapsed .sidebar-search,
.notes-sidebar.collapsed .nb-submenu,
.notes-sidebar.collapsed .sidebar-footer {
  opacity: 0;
  pointer-events: none;
}

.sidebar-top {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  transition: opacity 0.3s ease;
}

.user-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notification-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.notification-icon:hover {
  background: rgba(0, 0, 0, 0.05);
}

.create-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.create-icon:hover {
  background: rgba(0, 0, 0, 0.05);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  overflow: hidden;
}

.user-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sidebar-search {
  padding: 12px;
  transition: opacity 0.3s ease;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  color: var(--color-text-secondary);
  z-index: 1;
}

.search-input {
  width: 100%;
  height: 36px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  padding: 0 12px 0 36px;
  font-size: 13px;
  color: var(--color-text-primary);
  transition: all 0.2s ease;
}

.search-input:focus {
  outline: none;
  background: rgba(255, 255, 255, 0.7);
  border-color: #4a5568;
  box-shadow: 0 0 0 3px rgba(74, 85, 104, 0.1);
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--color-text-primary);
  font-size: 14px;
  margin-bottom: 2px;
  user-select: none;
}

.nav-item:hover {
  background: rgba(0, 0, 0, 0.05);
}

.nav-item.active {
  background: rgba(0, 0, 0, 0.06);
  color: var(--color-text-primary);
  font-weight: 700;
}

.nav-item.active:hover {
  background: rgba(0, 0, 0, 0.08);
}

.nav-icon {
  margin-right: 10px;
  flex-shrink: 0;
}

.nb-submenu {
  margin-left: 16px;
  padding: 8px 0;
  border-left: 2px solid rgba(0, 0, 0, 0.06);
}

.nb-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.nb-item:hover {
  color: var(--color-text-primary);
  background: rgba(0, 0, 0, 0.04);
}

.nb-item.active {
  color: #4a5568;
  background: rgba(74, 85, 104, 0.1);
  font-weight: 500;
}

.nb-item .doc-count {
  font-size: 11px;
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 10px;
}

.nb-empty-state {
  padding: 12px;
  font-size: 12px;
  color: var(--color-text-secondary);
  text-align: center;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  transition: opacity 0.3s ease;
}

/* ========== 可拖动分隔条 ========== */
.resize-divider {
  width: 16px;
  background: transparent;
  cursor: col-resize;
  position: relative;
  transition: all 0.3s ease;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.resize-divider:hover {
  background: rgba(74, 85, 104, 0.08);
}

.divider-line {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 2px;
  height: 40px;
  background: rgba(74, 85, 104, 0.3);
  border-radius: 1px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.resize-divider:hover .divider-line {
  opacity: 1;
}

.collapse-toggle-btn {
  width: 18px;
  height: 18px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.15);
  color: var(--color-text-secondary);
  position: relative;
  z-index: 1;
  opacity: 0;
  transform: scale(0.9);
  pointer-events: none;
}

.resize-divider:hover .collapse-toggle-btn {
  opacity: 1;
  transform: scale(1);
  pointer-events: auto;
}

.collapse-toggle-btn:hover {
  background: rgba(255, 255, 255, 1);
  border-color: rgba(74, 85, 104, 0.4);
  color: #4a5568;
  transform: scale(1.08);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* ========== 右侧内容区 ========== */
.notes-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  user-select: text;
}

/* 笔记本列表视图 */
.view-notebooks {
  padding: 20px;
  overflow-y: auto;
  height: 100%;
}

.content-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.view-toggle {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 8px;
  padding: 2px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.view-toggle :deep(.n-button) {
  padding: 6px 10px !important;
  border-radius: 6px !important;
}

.view-toggle :deep(.n-button.active) {
  background: rgba(74, 85, 104, 0.15) !important;
  color: #4a5568 !important;
}

.nb-section {
  margin-bottom: 28px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.collapse-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--color-text-primary);
}

.nb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.nb-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-nb-state {
  padding: 40px 0;
}

/* 编辑器视图 */
.view-editor {
  display: flex;
  height: 100%;
}

.editor-sidebar {
  min-width: 200px;
  max-width: 600px;
  background: rgba(255, 255, 255, 0.3);
  border-right: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: none;
}

.editor-sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.editor-sidebar-toolbar {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.editor-sidebar-toolbar :deep(.n-button) {
  flex: 1;
}

.tree-container {
  flex: 1;
  overflow-y: auto;
}

/* 编辑器侧栏可拖动分隔条 */
.editor-resize-divider {
  width: 16px;
  background: transparent;
  cursor: col-resize;
  position: relative;
  transition: all 0.3s ease;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.editor-resize-divider:hover {
  background: rgba(74, 85, 104, 0.08);
}

.editor-resize-divider .divider-line {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 2px;
  height: 40px;
  background: rgba(74, 85, 104, 0.3);
  border-radius: 1px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.editor-resize-divider:hover .divider-line {
  opacity: 1;
}

.editor-divider {
  width: 1px;
  background: rgba(0, 0, 0, 0.06);
}

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.3);
}

.title-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.title-input::placeholder {
  color: var(--color-text-secondary);
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

/* ========== 动画 ========== */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 滚动条样式 ========== */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

/* ========== 响应式设计 ========== */
@media (max-width: 1024px) {
  .nb-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .notes-main-card {
    width: 98%;
    height: 90vh;
    flex-direction: column;
  }

  .back-home-btn {
    top: 12px;
    left: 12px;
    padding: 6px 12px !important;
    font-size: 12px !important;
    height: 32px !important;
  }

  .resize-divider {
    width: 20px;
  }

  .notes-sidebar {
    width: 100% !important;
    height: 200px;
    border-right: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  }

  .notes-sidebar.collapsed {
    display: none;
  }

  .notes-content {
    width: 100%;
    flex: 1;
  }

  .nb-grid {
    grid-template-columns: 1fr;
  }

  .view-editor {
    flex-direction: column;
  }

  .editor-sidebar {
    width: 100% !important;
    height: 250px;
    border-right: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  }

  .editor-resize-divider {
    display: none;
  }
}

/* ========== 弹窗样式 ========== */
.create-modal :deep(.n-card),
:deep(.n-modal) {
  border-radius: 20px !important;
}

:deep(.n-card__header) {
  border-radius: 20px 20px 0 0 !important;
}

:deep(.n-button) {
  border-radius: 6px !important;
}

:deep(.n-input) {
  border-radius: 10px !important;
}

:deep(.n-input__input-el:focus),
:deep(.n-input__textarea-el:focus) {
  border-color: rgba(0, 0, 0, 0.2) !important;
  box-shadow: 0 0 0 3px rgba(74, 85, 104, 0.1) !important;
}
</style>

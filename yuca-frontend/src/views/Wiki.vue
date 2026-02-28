<template>
  <!-- 第1层：页面容器（暗色遮罩背景） -->
  <div class="wiki-page-container">
    <!-- 返回主页按钮 -->
    <n-button class="back-home-btn" @click="goBackHome">
      <template #icon>
        <n-icon :component="ArrowBackOutline" size="18" />
      </template>
      <span>返回主页</span>
    </n-button>

    <!-- 第2层：页面主体容器（毛玻璃卡片） -->
    <div class="wiki-main-card">
      <!-- 左侧：知识库列表 (可调整宽度，可折叠) -->
      <div class="wiki-sidebar" :class="{ collapsed: isCollapsed }" :style="{ width: sidebarWidth + '%' }">
        <!-- 用户区 -->
        <div class="sidebar-top" v-show="!isCollapsed">
          <div class="user-section">
            <div class="user-avatar">
              <img v-if="userAvatar" :src="userAvatar" alt="用户头像" class="avatar-img" />
              <span v-else>{{ userInitial }}</span>
            </div>
            <div class="create-icon" @click="showCreateKbModal = true">
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
              placeholder="搜索"
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

          <!-- 知识库子菜单 -->
          <div v-if="activeNav === 'knowledge'" v-show="!isCollapsed" class="kb-submenu">
            <div
              v-for="kb in knowledgeBases"
              :key="kb.id"
              class="kb-item"
              :class="{ active: selectedKbId === kb.id }"
              @click="selectKnowledgeBase(kb.id)"
              @mouseenter="hoveredKbId = kb.id"
              @mouseleave="hoveredKbId = null"
            >
              <span class="kb-name">• {{ kb.name }}</span>
              <div class="kb-menu-wrapper">
                <n-dropdown
                  :show="showDropdown && currentKbForMenu?.id === kb.id"
                  :options="dropdownOptions"
                  @select="handleDropdownSelect"
                  @clickoutside="showDropdown = false"
                  placement="right-start"
                  trigger="manual"
                >
                  <div
                    class="kb-menu-btn"
                    @click.stop="showKbMenu(kb, $event)"
                  >
                    <n-icon :component="EllipsisHorizontalOutline" size="16" />
                  </div>
                </n-dropdown>
              </div>
            </div>
            <div v-if="knowledgeBases.length === 0" class="kb-empty-state">
              暂无知识库
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
        <!-- 折叠按钮 -->
        <div class="collapse-toggle-btn" @click.stop="toggleSidebar">
          <n-icon :component="isCollapsed ? ChevronForwardOutline : ChevronBackOutline" size="14" />
        </div>
      </div>

      <!-- 右侧：内容区 (剩余宽度) -->
      <div class="wiki-content">
        <!-- 知识库列表视图（新风格） -->
        <div v-if="activeView === 'list'">
          <!-- 页面头部 -->
          <div class="content-page-header">
            <div class="header-left">
              <h2 class="page-title">知识库</h2>
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
          <div class="kb-section" v-if="commonKbs.length > 0">
            <div class="section-header">
              <h3 class="section-title">常用</h3>
              <div class="collapse-btn" @click="toggleCommonCollapse">
                <span>{{ commonCollapsed ? '展开' : '收起' }}</span>
                <n-icon :component="commonCollapsed ? ChevronDownOutline : ChevronUpOutline" size="14" />
              </div>
            </div>

            <div v-show="!commonCollapsed" :class="viewMode === 'grid' ? 'kb-grid' : 'kb-list'">
              <KnowledgeCard
                v-for="kb in commonKbs"
                :key="kb.id"
                :kb="kb"
                :mode="viewMode"
                :recent-docs="getKbRecentDocs(kb.id)"
                @click="openKb(kb)"
              />
            </div>
          </div>

          <!-- 我的知识库分组 -->
          <div class="kb-section">
            <div v-if="filteredKbs.length === 0" class="empty-kb-state">
              <n-empty description="暂无知识库，点击左侧创建按钮开始创建" />
            </div>
            <div v-else :class="viewMode === 'grid' ? 'kb-grid' : 'kb-list'">
              <KnowledgeCard
                v-for="kb in filteredKbs"
                :key="kb.id"
                :kb="kb"
                :mode="viewMode"
                :recent-docs="getKbRecentDocs(kb.id)"
                @click="openKb(kb)"
              />
            </div>
          </div>
        </div>

        <!-- 文档列表视图（原有功能） -->
        <div v-else-if="activeView === 'docs' && selectedKb">
          <div class="content-header">
            <h3 class="content-title">{{ selectedKb.name }}</h3>
            <n-button text size="small" class="upload-doc-btn" @click="showUploadModal = true">
              <template #icon>
                <n-icon :component="CloudUploadOutline" />
              </template>
              上传文档
            </n-button>
          </div>

          <div class="doc-list" :class="{ 'has-docs': documents.length > 0 }">
            <n-spin :show="docLoading">
              <div v-if="documents.length === 0 && !docLoading" class="doc-empty-wrapper">
                <n-empty description="暂无文档" />
              </div>
              <div
                v-for="doc in documents"
                :key="doc.id"
                class="doc-card"
                @click="viewChunks(doc)"
              >
                <div class="doc-icon">
                  <n-icon :component="FileTrayOutline" />
                </div>
                <div class="doc-name">{{ doc.docName }}</div>
                <div class="doc-meta">
                  <span>{{ doc.docFormat.toUpperCase() }}</span>
                  <span>{{ formatFileSize(doc.docSize) }}</span>
                  <span>{{ doc.chunkCount }} 个切片</span>
                </div>
              </div>
            </n-spin>
          </div>
        </div>

        <!-- 切片视图 -->
        <div v-else-if="activeView === 'chunks' && selectedDoc">
          <div class="content-header">
            <div class="header-left">
              <n-button text @click="activeView = 'docs'">
                <template #icon>
                  <n-icon :component="ArrowBackOutline" />
                </template>
                返回
              </n-button>
              <span class="doc-title">{{ selectedDoc.docName }}</span>
            </div>
          </div>

          <div class="chunks-grid">
            <n-spin :show="chunkLoading">
              <n-empty v-if="chunks.length === 0 && !chunkLoading" description="暂无切片" />
              <div
                v-for="chunk in chunks"
                :key="chunk.id"
                class="chunk-card"
              >
                <div class="chunk-content">{{ chunk.content }}</div>
                <div class="chunk-footer">
                  <span class="chunk-id">#{{ chunk.chunkIndex + 1 }}</span>
                </div>
              </div>
            </n-spin>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <n-empty description="请选择一个知识库查看文档" />
        </div>
      </div>
    </div>

    <!-- 创建知识库弹窗 -->
    <n-modal v-model:show="showCreateKbModal" preset="card" title="创建知识库" class="create-modal" style="width: 500px">
      <n-form ref="createFormRef" :model="createForm" :rules="createRules" label-placement="left" label-width="80">
        <n-form-item label="名称" path="name">
          <n-input v-model:value="createForm.name" placeholder="请输入知识库名称" class="modal-input" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input
            v-model:value="createForm.description"
            type="textarea"
            placeholder="请输入知识库描述"
            :rows="3"
            class="modal-input"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <n-button @click="showCreateKbModal = false">取消</n-button>
          <n-button type="primary" class="modal-primary-btn" @click="handleCreateKb" :loading="createLoading">创建</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 上传文档弹窗 -->
    <n-modal v-model:show="showUploadModal" preset="card" title="上传文档" style="width: 500px">
      <n-upload
        :custom-request="handleUpload"
        :show-file-list="false"
        accept=".md,.txt,.pdf"
        :max="1"
      >
        <n-upload-dragger>
          <div style="margin-bottom: 12px">
            <n-icon :component="CloudUploadOutline" size="48" :depth="3" />
          </div>
          <n-text style="font-size: 16px">
            点击或拖拽文件到此区域上传
          </n-text>
          <n-p depth="3" style="margin: 8px 0 0 0">
            支持 Markdown、TXT、PDF 格式，文件大小不超过 50MB
          </n-p>
        </n-upload-dragger>
      </n-upload>
      <template #footer>
        <n-button @click="showUploadModal = false">关闭</n-button>
      </template>
    </n-modal>

    <!-- 编辑知识库弹窗 -->
    <n-modal v-model:show="showRenameModal" preset="card" title="编辑知识库" class="create-modal" style="width: 500px">
      <n-form ref="renameFormRef" :model="renameForm" :rules="{ name: { required: true, message: '请输入知识库名称', trigger: 'blur' } }" label-placement="left" label-width="80">
        <n-form-item label="名称" path="name">
          <n-input v-model:value="renameForm.name" placeholder="请输入知识库名称" class="modal-input" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <n-button @click="showRenameModal = false">取消</n-button>
          <n-button type="primary" class="modal-primary-btn" @click="handleRenameKb" :loading="renameLoading">确定</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NIcon,
  NSpin,
  NEmpty,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NUpload,
  NUploadDragger,
  NP,
  NText,
  NDropdown,
  useMessage,
  useDialog
} from 'naive-ui'
import type { UploadCustomRequestOptions } from 'naive-ui'
import * as knowledgeApi from '@/api/knowledge'
import type { KnowledgeBase, KnowledgeDoc, KnowledgeChunk } from '@/types/knowledge'
import KnowledgeCard from '@/components/KnowledgeCard.vue'
import {
  AddOutline,
  AddCircleOutline,
  CloudUploadOutline,
  DocumentTextOutline,
  DocumentOutline,
  CopyOutline,
  FileTrayOutline,
  ArrowBackOutline,
  FolderOutline,
  ChevronForwardOutline,
  ChevronBackOutline,
  ChevronDownOutline,
  ChevronUpOutline,
  NotificationsOutline,
  SearchOutline,
  GridOutline,
  ListOutline,
  EllipsisHorizontalOutline
} from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

// ========== 侧栏相关 ==========
// 侧栏宽度状态
const sidebarWidth = ref(20) // 默认20%
const isResizing = ref(false)
const startX = ref(0)
const startWidth = ref(20)
const isCollapsed = ref(false) // 侧栏折叠状态
const savedWidth = ref(20) // 保存折叠前的宽度

// 用户信息
const userInitial = computed(() => {
  // 从 localStorage 获取用户信息
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
    console.log('从缓存读取到头像，长度:', cachedAvatar.length)
    return cachedAvatar
  }

  console.log('缓存中没有头像')
  return null
})

// 搜索
const searchInputRef = ref<HTMLInputElement>()
const searchQuery = ref('')

// 导航菜单
const navItems = ref([
  { key: 'knowledge', label: '知识库', icon: FolderOutline }
  // 可以扩展其他菜单项
])
const activeNav = ref('knowledge')

// ========== 视图相关 ==========
// activeView: 'list' | 'docs' | 'chunks'
// list: 知识库列表视图（新风格）
// docs: 文档列表视图（原有）
// chunks: 切片视图（原有）
const activeView = ref<'list' | 'docs' | 'chunks'>('list')

// 视图模式（网格/列表）
const viewMode = ref<'grid' | 'list'>('grid')

// Tab 切换
const activeTab = ref<'personal' | 'invited'>('personal')

// 常用分组折叠状态
const commonCollapsed = ref(false)

// ========== 数据相关 ==========
// 知识库列表
const knowledgeBases = ref<KnowledgeBase[]>([])
const kbLoading = ref(false)

// 当前选中的知识库和文档
const selectedKbId = ref<number | null>(null)
const selectedKb = computed(() => knowledgeBases.value.find(kb => kb.id === selectedKbId.value) || null)
const selectedDoc = ref<KnowledgeDoc | null>(null)

// 所有文档列表（按知识库ID分组）
const allDocuments = ref<Map<number, KnowledgeDoc[]>>(new Map())

// 文档列表
const documents = ref<KnowledgeDoc[]>([])
const docLoading = ref(false)

// 切片列表
const chunks = ref<KnowledgeChunk[]>([])
const chunkLoading = ref(false)

// 创建知识库弹窗
const showCreateKbModal = ref(false)
const createLoading = ref(false)
const createForm = ref({
  name: '',
  description: ''
})
const createRules = {
  name: { required: true, message: '请输入知识库名称', trigger: 'blur' }
}

// 上传文档弹窗
const showUploadModal = ref(false)

// 下拉菜单相关
const showDropdown = ref(false)
const hoveredKbId = ref<number | null>(null)
const currentKbForMenu = ref<KnowledgeBase | null>(null)

// 重命名弹窗
const showRenameModal = ref(false)
const renameForm = ref({
  name: ''
})
const renameLoading = ref(false)

// ========== 计算属性 ==========

// 常用知识库（最近访问）
const commonKbs = computed(() => {
  return knowledgeBases.value
    .filter((kb: KnowledgeBase) => kb.isFrequentlyUsed)
    .slice(0, 4)
})

// 过滤后的知识库列表
const filteredKbs = computed(() => {
  // 搜索过滤
  let result = knowledgeBases.value

  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter((kb: KnowledgeBase) =>
      kb.name.toLowerCase().includes(query) ||
      (kb.description && kb.description.toLowerCase().includes(query))
    )
  }

  // Tab 过滤
  if (activeTab.value === 'personal') {
    return result.filter((kb: KnowledgeBase) => kb.isOwner !== false)
  } else {
    return result.filter((kb: KnowledgeBase) => kb.isOwner === false)
  }
})

// ========== 方法 ==========

// 处理导航菜单点击
const handleNavClick = (key: string) => {
  activeNav.value = key
  // 如果点击的是知识库，重置为列表视图
  if (key === 'knowledge') {
    selectedKbId.value = null
    activeView.value = 'list'
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

// 打开知识库
const openKb = async (kb: KnowledgeBase) => {
  selectedKbId.value = kb.id
  activeView.value = 'docs'
  await loadDocuments()
}

// 切换常用分组折叠
const toggleCommonCollapse = () => {
  commonCollapsed.value = !commonCollapsed.value
}

// 获取知识库的最近文档
const getKbRecentDocs = (kbId: number): KnowledgeDoc[] => {
  const docs = allDocuments.value.get(kbId) || []
  return docs.slice(0, 3) // 最多显示3个
}

// 加载知识库列表
const loadKnowledgeBases = async () => {
  try {
    kbLoading.value = true
    const data = await knowledgeApi.getKnowledgeBaseList()
    // 为每个知识库添加默认的 UI 字段
    knowledgeBases.value = data.map((kb: KnowledgeBase) => ({
      ...kb,
      isOwner: true, // 默认为所有者
      isFrequentlyUsed: false, // 默认不是常用
      lastAccessTime: kb.updatedAt
    }))

    // 加载所有知识库的文档
    for (const kb of knowledgeBases.value) {
      try {
        const docs = await knowledgeApi.getDocumentList(kb.id, 1, 1000)
        allDocuments.value.set(kb.id, docs.records)
      } catch (error) {
        console.error(`加载知识库 ${kb.id} 的文档失败:`, error)
        allDocuments.value.set(kb.id, [])
      }
    }
  } catch (error) {
    console.error('加载知识库列表失败:', error)
    knowledgeBases.value = []
  } finally {
    kbLoading.value = false
  }
}

// 选择知识库
const selectKnowledgeBase = async (kbId: number) => {
  selectedKbId.value = kbId
  activeView.value = 'docs'
  await loadDocuments()
}

// 加载文档列表
const loadDocuments = async () => {
  if (!selectedKbId.value) return

  try {
    docLoading.value = true
    const data = await knowledgeApi.getDocumentList(selectedKbId.value, 1, 100)
    documents.value = data.records
    allDocuments.value.set(selectedKbId.value, data.records)
  } catch (error) {
    console.error('加载文档列表失败:', error)
    message.error('加载文档列表失败')
  } finally {
    docLoading.value = false
  }
}

// 查看文档切片
const viewChunks = async (doc: KnowledgeDoc) => {
  selectedDoc.value = doc
  activeView.value = 'chunks'
  await loadChunks()
}

// 加载切片列表
const loadChunks = async () => {
  if (!selectedDoc.value) return

  try {
    chunkLoading.value = true
    const data = await knowledgeApi.getChunkList(selectedDoc.value.id)
    chunks.value = data
  } catch (error) {
    console.error('加载切片列表失败:', error)
    message.error('加载切片列表失败')
  } finally {
    chunkLoading.value = false
  }
}

// 创建知识库
const handleCreateKb = async () => {
  try {
    createLoading.value = true
    await knowledgeApi.createKnowledgeBase({
      name: createForm.value.name,
      description: createForm.value.description
    })
    message.success('知识库创建成功')
    showCreateKbModal.value = false
    createForm.value = { name: '', description: '' }
    await loadKnowledgeBases()
  } catch (error: any) {
    console.error('创建知识库失败:', error)
    message.error(error.message || '创建知识库失败')
  } finally {
    createLoading.value = false
  }
}

// 上传文档
const handleUpload = async (options: UploadCustomRequestOptions) => {
  if (!selectedKbId.value) {
    message.error('请先选择知识库')
    return
  }

  try {
    const file = options.file.file as File
    await knowledgeApi.uploadDocument(selectedKbId.value, file)
    message.success('文档上传成功')
    showUploadModal.value = false
    await loadDocuments()
    const docs = await knowledgeApi.getDocumentList(selectedKbId.value, 1, 1000)
    allDocuments.value.set(selectedKbId.value, docs.records)
  } catch (error) {
    console.error('文档上传失败:', error)
    message.error('文档上传失败')
  }
}

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 下拉菜单选项
const dropdownOptions = [
  {
    label: '编辑',
    key: 'rename'
  },
  {
    label: '删除',
    key: 'delete'
  }
]

// 处理下拉菜单选择
const handleDropdownSelect = (key: string) => {
  if (!currentKbForMenu.value) return

  if (key === 'rename') {
    renameForm.value.name = currentKbForMenu.value.name
    showRenameModal.value = true
  } else if (key === 'delete') {
    handleDeleteKb(currentKbForMenu.value)
  }
  showDropdown.value = false
}

// 显示下拉菜单
const showKbMenu = (kb: KnowledgeBase, e: MouseEvent) => {
  e.stopPropagation()
  currentKbForMenu.value = kb
  showDropdown.value = true
}

// 删除知识库
const handleDeleteKb = async (kb: KnowledgeBase) => {
  dialog.warning({
    title: '删除知识库',
    content: `确定要删除知识库"${kb.name}"吗？此操作不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await knowledgeApi.deleteKnowledgeBase(kb.id)
        message.success('知识库删除成功')

        // 如果删除的是当前选中的知识库，清除选中状态
        if (selectedKbId.value === kb.id) {
          selectedKbId.value = null
          activeView.value = 'list'
        }

        await loadKnowledgeBases()
      } catch (error: any) {
        console.error('删除知识库失败:', error)
        message.error(error.message || '删除知识库失败')
      }
    }
  })
}

// 重命名知识库
const handleRenameKb = async () => {
  if (!currentKbForMenu.value || !renameForm.value.name.trim()) return

  try {
    renameLoading.value = true
    await knowledgeApi.updateKnowledgeBase(currentKbForMenu.value.id, {
      name: renameForm.value.name.trim()
    })
    message.success('知识库编辑成功')
    showRenameModal.value = false
    renameForm.value.name = ''
    await loadKnowledgeBases()
  } catch (error: any) {
    console.error('编辑知识库失败:', error)
    message.error(error.message || '编辑知识库失败')
  } finally {
    renameLoading.value = false
  }
}

// 开始拖动调整宽度
const startResize = (e: MouseEvent) => {
  isResizing.value = true
  startX.value = e.clientX
  startWidth.value = sidebarWidth.value

  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
  e.preventDefault()
}

// 拖动中
const onResize = (e: MouseEvent) => {
  if (!isResizing.value) return

  const mainCard = document.querySelector('.wiki-main-card') as HTMLElement
  if (!mainCard) return

  const containerWidth = mainCard.offsetWidth
  const deltaX = e.clientX - startX.value
  const deltaPercent = (deltaX / containerWidth) * 100

  let newWidth = startWidth.value + deltaPercent
  newWidth = Math.max(15, Math.min(60, newWidth))

  sidebarWidth.value = newWidth
}

// 停止拖动
const stopResize = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}

// 重置宽度（双击分隔条）
const resetWidth = () => {
  sidebarWidth.value = 20
}

// 返回主页
const goBackHome = () => {
  router.push('/')
}

// 快捷键：Ctrl+J 聚焦搜索
const handleKeyDown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'j') {
    e.preventDefault()
    searchInputRef.value?.focus()
  }
}

onMounted(() => {
  loadKnowledgeBases()
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
/* ========== 第1层：页面容器 ========== */
.wiki-page-container {
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
.wiki-main-card {
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
  background: rgba(255, 255, 255, 0.75) !important;
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(20px) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.2) !important;
  color: var(--color-text-primary) !important;
  transition: all 0.3s ease;
}

.back-home-btn:hover {
  background: rgba(255, 255, 255, 0.85) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(0, 0, 0, 0.25) !important;
}

/* ========== 左侧侧栏 ========== */
.wiki-sidebar {
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

/* 折叠状态 */
.wiki-sidebar.collapsed {
  width: 60px !important;
}

.wiki-sidebar.collapsed .sidebar-top,
.wiki-sidebar.collapsed .sidebar-search,
.wiki-sidebar.collapsed .kb-submenu,
.wiki-sidebar.collapsed .sidebar-footer {
  opacity: 0;
  pointer-events: none;
}

/* 原有的折叠按钮样式已移除 */

/* 顶部用户区 */
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

/* 搜索栏 */
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
  padding: 0 70px 0 36px;
  font-size: 13px;
  color: var(--color-text-primary);
  transition: all 0.2s ease;
}

.search-input::placeholder {
  color: var(--color-text-secondary);
}

.search-input:focus {
  outline: none;
  background: rgba(255, 255, 255, 0.7);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.1);
}

.search-shortcut {
  position: absolute;
  right: 8px;
  font-size: 11px;
  color: var(--color-text-secondary);
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
}

/* 导航菜单 */
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

/* 知识库子菜单 */
.kb-submenu {
  margin-left: 16px;
  padding: 8px 0;
  border-left: 2px solid rgba(0, 0, 0, 0.06);
}

.submenu-header {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  padding: 4px 8px 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.kb-item {
  position: relative;
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

.kb-item:hover {
  color: var(--color-text-primary);
  background: rgba(0, 0, 0, 0.04);
}

.kb-item.active {
  color: #14b8a6;
  background: rgba(20, 184, 166, 0.1);
  font-weight: 500;
}

.kb-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-menu-wrapper {
  position: relative;
  margin-left: 8px;
}

.kb-menu-btn {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  opacity: 0;
  transition: all 0.2s ease;
  cursor: pointer;
}

.kb-item:hover .kb-menu-btn,
.kb-menu-btn:hover {
  opacity: 1;
  background: rgba(0, 0, 0, 0.06);
}

.kb-menu-btn:hover {
  background: rgba(0, 0, 0, 0.1);
}

.kb-empty-state {
  padding: 12px;
  font-size: 12px;
  color: var(--color-text-secondary);
  text-align: center;
}

/* 侧栏底部 */
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  transition: opacity 0.3s ease;
}

/* ========== 可拖动分隔条 ========== */
.resize-divider {
  width: 24px;
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
  background: rgba(20, 184, 166, 0.08);
}

.resize-divider:active {
  background: rgba(20, 184, 166, 0.12);
}

.divider-line {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 2px;
  height: 40px;
  background: rgba(20, 184, 166, 0.3);
  border-radius: 1px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.resize-divider:hover .divider-line {
  opacity: 1;
}

/* 折叠按钮 */
.collapse-toggle-btn {
  width: 24px;
  height: 24px;
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
  border-color: rgba(20, 184, 166, 0.4);
  color: #14b8a6;
  transform: scale(1.08);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.collapse-toggle-btn:active {
  transform: scale(0.95);
}

/* ========== 右侧内容区 ========== */
.wiki-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  user-select: text;
}

/* 页面头部（新风格） */
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

/* 视图切换 */
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
  background: rgba(0, 0, 0, 0.1) !important;
  color: #1a1a1a !important;
  font-weight: 500;
}

/* 分组区域 */
.kb-section {
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

/* Tab 切换 */
.kb-tabs {
  display: flex;
  align-items: center;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
  padding: 3px;
  margin-bottom: 16px;
  width: fit-content;
}

.kb-tab {
  padding: 6px 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.kb-tab:hover {
  color: var(--color-text-primary);
}

.kb-tab.active {
  background: rgba(255, 255, 255, 0.9);
  color: var(--color-text-primary);
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

/* 卡片网格 */
.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

/* 卡片列表 */
.kb-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 空状态 */
.empty-kb-state {
  padding: 40px 0;
}

/* ========== 原有内容区样式 ========== */
.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.content-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.doc-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text-primary);
}

/* 文档列表 - 列表形式 */
.doc-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-list:not(.has-docs) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.doc-empty-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.doc-card {
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-card:hover {
  background: rgba(128, 128, 128, 0.15);
  border-color: rgba(128, 128, 128, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.1);
}

.doc-card:active {
  background: rgba(128, 128, 128, 0.2);
}

.doc-icon {
  width: 32px;
  height: 32px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  font-size: 18px;
  flex-shrink: 0;
}

.doc-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  flex: 1;
}

.doc-meta {
  font-size: 12px;
  color: var(--color-text-secondary);
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 切片网格 - 增加间距，减少悬浮效果 */
.chunks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
}

.chunk-card {
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  padding: 20px;
  min-height: 150px;
  transition: all 0.2s ease;
  cursor: pointer;
}

.chunk-card:hover {
  background: rgba(128, 128, 128, 0.15);
  border-color: rgba(128, 128, 128, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.12);
}

.chunk-card:active {
  background: rgba(128, 128, 128, 0.2);
}

.chunk-content {
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-primary);
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 6;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.chunk-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.chunk-id {
  font-size: 11px;
  color: var(--color-text-secondary);
  background: rgba(0, 0, 0, 0.05);
  padding: 4px 8px;
  border-radius: 4px;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
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
  .wiki-main-card {
    width: 95%;
    height: 85vh;
  }

  .doc-list {
    flex-direction: column;
    gap: 8px;
  }

  .chunks-grid {
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
  }

  .kb-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .wiki-main-card {
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

  .wiki-sidebar {
    width: 100% !important;
    height: 200px;
    border-right: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  }

  .wiki-sidebar.collapsed {
    display: none;
  }

  .wiki-content {
    width: 100%;
    flex: 1;
  }

  .doc-list {
    flex-direction: column;
    gap: 8px;
  }

  .doc-card {
    padding: 8px 10px;
  }

  .chunks-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .kb-grid {
    grid-template-columns: 1fr;
  }
}

/* ========== 弹窗样式 ========== */
.create-modal :deep(.n-card),
:deep(.n-modal) {
  border-radius: 32px !important;
}

:deep(.n-card__header) {
  border-radius: 32px 32px 0 0 !important;
}

/* 删除确认对话框圆角 */
:deep(.n-dialog) {
  border-radius: 32px !important;
}

:deep(.n-dialog__title) {
  border-radius: 32px 32px 0 0 !important;
}

/* 上传文档按钮 - 浅色背景，深灰色边框 */
.upload-doc-btn {
  background: rgba(0, 0, 0, 0.02) !important;
  color: var(--color-text-primary) !important;
  border: 1px solid rgba(0, 0, 0, 0.2) !important;
  box-shadow: none !important;
  padding: 4px 12px !important;
  border-radius: 6px !important;
  font-size: 13px !important;
}

.upload-doc-btn:hover {
  background: rgba(0, 0, 0, 0.05) !important;
  border-color: rgba(0, 0, 0, 0.3) !important;
}

.upload-doc-btn:focus {
  background: rgba(0, 0, 0, 0.05) !important;
  border-color: rgba(0, 0, 0, 0.3) !important;
}

/* 按钮圆角 */
:deep(.n-button) {
  border-radius: 6px !important;
}

:deep(.n-button--primary) {
  border-radius: 6px !important;
}

:deep(.n-button--default) {
  border-radius: 6px !important;
}

/* 输入框圆角 */
:deep(.n-input) {
  border-radius: 10px !important;
}

:deep(.n-input__textarea-el) {
  border-radius: 10px !important;
}

/* 上传区域圆角 */
:deep(.n-upload-dragger) {
  border-radius: 16px !important;
}

/* 输入框 focus 样式 */
:deep(.n-input__input-el:focus),
:deep(.n-input__textarea-el:focus) {
  border-color: rgba(0, 0, 0, 0.2) !important;
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.1) !important;
}

:deep(.n-input:focus),
:deep(.n-input.n-input--focus) {
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.1) !important;
}

/* ========== 弹窗样式 ========== */
/* 模态框主按钮 - 使用主题色 */
.modal-primary-btn {
  background: var(--color-primary) !important;
  color: #ffffff !important;
  border: none !important;
}

.modal-primary-btn:hover {
  background: var(--color-secondary) !important;
}

.modal-primary-btn:active {
  background: #0d9488 !important;
}

/* 模态框输入框 - 灰色边框 */
:deep(.modal-input .n-input__input-el),
:deep(.modal-input .n-input__textarea-el) {
  border-color: rgba(0, 0, 0, 0.15) !important;
}

:deep(.modal-input .n-input__input-el:focus),
:deep(.modal-input .n-input__textarea-el:focus) {
  border-color: rgba(0, 0, 0, 0.3) !important;
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.08) !important;
}

:deep(.modal-input.n-input:focus),
:deep(.modal-input.n-input.n-input--focus) {
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.08) !important;
}

/* 模态框默认按钮 - 灰色 */
.create-modal :deep(.n-button--default) {
  background: rgba(0, 0, 0, 0.05) !important;
  color: var(--color-text-primary) !important;
  border: 1px solid rgba(0, 0, 0, 0.1) !important;
}

.create-modal :deep(.n-button--default:hover) {
  background: rgba(0, 0, 0, 0.1) !important;
  border-color: rgba(0, 0, 0, 0.15) !important;
}

/* ========== 对话框黑白灰样式 ========== */
/* 对话框确认按钮 - 红色（删除操作） */
:deep(.n-dialog__action .n-button--warning) {
  background: #dc2626 !important;
  color: #ffffff !important;
  border: none !important;
}

:deep(.n-dialog__action .n-button--warning:hover) {
  background: #b91c1c !important;
}

/* 对话框取消按钮 - 灰色 */
:deep(.n-dialog__action .n-button--default) {
  background: rgba(0, 0, 0, 0.05) !important;
  color: var(--color-text-primary) !important;
  border: 1px solid rgba(0, 0, 0, 0.1) !important;
}

:deep(.n-dialog__action .n-button--default:hover) {
  background: rgba(0, 0, 0, 0.1) !important;
}
</style>

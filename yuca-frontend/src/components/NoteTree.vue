<template>
  <div class="note-tree">
    <!-- 空状态 -->
    <div v-if="items.length === 0" class="tree-empty">
      <n-empty description="暂无文档，点击右上角创建" />
    </div>

    <!-- 树节点列表 -->
    <div v-else class="tree-nodes">
      <TreeNode
        v-for="item in items"
        :key="item.id"
        :item="item"
        :active-id="activeId"
        :expanded-ids="expandedIds"
        @click="handleClick"
        @toggle="handleToggle"
        @context-menu="handleContextMenu"
      />
    </div>

    <!-- 右键菜单 -->
    <n-dropdown
      placement="bottom-start"
      trigger="manual"
      :show="showContextMenu"
      :options="contextMenuOptions"
      :x="contextMenuX"
      :y="contextMenuY"
      @clickoutside="closeContextMenu"
      @select="handleContextMenuSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h } from 'vue'
import { NEmpty, NDropdown, NIcon } from 'naive-ui'
import type { NoteItem } from '@/types/note'
import TreeNode from './TreeNode.vue'
import {
  AddOutline,
  CreateOutline,
  TrashOutline,
  DocumentOutline
} from '@vicons/ionicons5'

interface Props {
  items: NoteItem[]
  activeId?: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  click: [item: NoteItem]
  'create-folder': [parentId: number | null]
  'create-document': [parentId: number | null]
  rename: [item: NoteItem]
  delete: [item: NoteItem]
}>()

// 展开的节点 ID 集合
const expandedIds = ref<Set<number>>(new Set())

// 右键菜单相关
const showContextMenu = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuItem = ref<NoteItem | null>(null)

// 右键菜单选项
const contextMenuOptions = computed(() => {
  const item = contextMenuItem.value
  if (!item) return []

  const options = [
    {
      label: '新建文件夹',
      key: 'create-folder',
      icon: () => h(NIcon, null, { default: () => h(AddOutline) })
    },
    {
      label: '新建文档',
      key: 'create-document',
      icon: () => h(NIcon, null, { default: () => h(DocumentOutline) })
    },
    {
      type: 'divider',
      key: 'd1'
    },
    {
      label: '重命名',
      key: 'rename',
      icon: () => h(NIcon, null, { default: () => h(CreateOutline) })
    },
    {
      label: '删除',
      key: 'delete',
      icon: () => h(NIcon, null, { default: () => h(TrashOutline) })
    }
  ]

  return options
})

// 处理节点点击
const handleClick = (item: NoteItem) => {
  emit('click', item)
}

// 处理节点展开/折叠
const handleToggle = (item: NoteItem) => {
  if (expandedIds.value.has(item.id)) {
    expandedIds.value.delete(item.id)
  } else {
    expandedIds.value.add(item.id)
  }
}

// 处理右键菜单
const handleContextMenu = (item: NoteItem, e: MouseEvent) => {
  e.preventDefault()
  contextMenuItem.value = item
  contextMenuX.value = e.clientX
  contextMenuY.value = e.clientY
  showContextMenu.value = true
}

// 关闭右键菜单
const closeContextMenu = () => {
  showContextMenu.value = false
}

// 处理右键菜单选择
const handleContextMenuSelect = (key: string) => {
  const item = contextMenuItem.value
  if (!item) return

  closeContextMenu()

  switch (key) {
    case 'create-folder':
      emit('create-folder', item.id)
      break
    case 'create-document':
      emit('create-document', item.id)
      break
    case 'rename':
      emit('rename', item)
      break
    case 'delete':
      emit('delete', item)
      break
  }
}

// 展开/折叠所有
const expandAll = () => {
  const addIds = (items: NoteItem[]) => {
    items.forEach(item => {
      expandedIds.value.add(item.id)
      if (item.children && item.children.length > 0) {
        addIds(item.children)
      }
    })
  }
  addIds(props.items)
}

const collapseAll = () => {
  expandedIds.value.clear()
}

defineExpose({
  expandAll,
  collapseAll
})
</script>

<script lang="ts">
export default {
  name: 'NoteTree'
}
</script>

<style scoped>
.note-tree {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tree-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.tree-nodes {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}
</style>

<template>
  <div class="tree-node-wrapper">
    <!-- 当前节点 -->
    <div
      class="tree-node"
      :class="{
        'active': isActive,
        'folder': item.type === 'FOLDER',
        'document': item.type === 'DOCUMENT',
        'pinned': item.isPinned
      }"
      :style="{ paddingLeft: `${item.level || 0 * 20 + 12}px` }"
      @click="handleClick"
      @contextmenu="handleContextMenu"
    >
      <!-- 展开/折叠图标 -->
      <span
        v-if="hasChildren"
        class="expand-icon"
        :class="{ 'expanded': isExpanded }"
        @click.stop="handleToggle"
      >
        <n-icon :component="ChevronForwardOutline" size="16" />
      </span>
      <span v-else class="expand-icon-placeholder"></span>

      <!-- 节点图标 -->
      <span class="node-icon">
        <n-icon :component="nodeIcon" size="18" />
      </span>

      <!-- 节点标题 -->
      <span class="node-title" :title="item.title">{{ item.title }}</span>

      <!-- 置顶标记 -->
      <span v-if="item.isPinned" class="pinned-icon">
        <n-icon :component="PushOutline" size="14" />
      </span>

      <!-- 文档数量（仅文件夹显示） -->
      <span v-if="item.type === 'FOLDER' && item.childCount" class="child-count">
        {{ item.childCount }}
      </span>
    </div>

    <!-- 子节点（递归渲染） -->
    <div v-if="hasChildren && isExpanded" class="tree-children">
      <TreeNode
        v-for="child in item.children"
        :key="child.id"
        :item="child"
        :active-id="activeId"
        :expanded-ids="expandedIds"
        @click="handleChildClick"
        @toggle="handleChildToggle"
        @context-menu="handleChildContextMenu"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon } from 'naive-ui'
import {
  ChevronForwardOutline,
  FolderOutline,
  DocumentTextOutline,
  PushOutline
} from '@vicons/ionicons5'
import type { NoteItem } from '@/types/note'

interface Props {
  item: NoteItem
  activeId?: number | null
  expandedIds: Set<number>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  click: [item: NoteItem]
  toggle: [item: NoteItem]
  'context-menu': [item: NoteItem, e: MouseEvent]
}>()

// 子节点事件处理
const handleChildClick = (item: NoteItem) => {
  emit('click', item)
}

const handleChildToggle = (item: NoteItem) => {
  emit('toggle', item)
}

const handleChildContextMenu = (item: NoteItem, e: MouseEvent) => {
  emit('context-menu', item, e)
}

// 是否激活
const isActive = computed(() => props.activeId === props.item.id)

// 是否展开
const isExpanded = computed(() => props.expandedIds.has(props.item.id))

// 是否有子节点
const hasChildren = computed(() =>
  props.item.children && props.item.children.length > 0
)

// 节点图标
const nodeIcon = computed(() => {
  if (props.item.type === 'FOLDER') {
    return FolderOutline
  }
  return DocumentTextOutline
})

// 处理点击
const handleClick = () => {
  emit('click', props.item)
}

// 处理展开/折叠
const handleToggle = () => {
  emit('toggle', props.item)
}

// 处理右键菜单
const handleContextMenu = (e: MouseEvent) => {
  emit('context-menu', props.item, e)
}
</script>

<style scoped>
.tree-node-wrapper {
  user-select: none;
}

.tree-node {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 6px;
  margin: 2px 4px;
  position: relative;
}

.tree-node:hover {
  background: rgba(0, 0, 0, 0.05);
}

.tree-node.active {
  background: rgba(20, 184, 166, 0.15);
  color: #14b8a6;
  font-weight: 500;
}

.tree-node.folder .node-icon {
  color: #f59e0b;
}

.tree-node.document .node-icon {
  color: #6366f1;
}

.tree-node.pinned {
  opacity: 0.9;
}

/* 展开/折叠图标 */
.expand-icon {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 4px;
  transition: transform 0.2s ease;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.expand-icon:hover {
  color: var(--color-text-primary);
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.expand-icon-placeholder {
  width: 16px;
  margin-right: 4px;
  display: inline-block;
}

/* 节点图标 */
.node-icon {
  margin-right: 8px;
  flex-shrink: 0;
}

/* 节点标题 */
.node-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

/* 置顶图标 */
.pinned-icon {
  margin-left: 4px;
  color: #f59e0b;
  flex-shrink: 0;
}

/* 子节点数量 */
.child-count {
  margin-left: 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 10px;
  flex-shrink: 0;
}

/* 子节点容器 */
.tree-children {
  animation: expand 0.2s ease;
}

@keyframes expand {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

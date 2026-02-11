<template>
  <div class="nb-card" :class="[`nb-card--${mode}`]" @click="$emit('click')">
    <!-- 卡片头部 -->
    <div class="nb-card-header">
      <div class="nb-icon" :style="{ background: colorStyle }">
        <n-icon :component="BookOutline" />
      </div>
      <div class="nb-info">
        <div class="nb-title" :title="book.name">{{ book.name }}</div>
        <div class="nb-subtitle" :title="book.description || '暂无描述'">
          {{ book.description || '暂无描述' }}
        </div>
      </div>
      <div class="nb-more" @click.stop="handleMore">
        <n-icon :component="EllipsisVerticalOutline" />
      </div>
    </div>

    <!-- 分隔线 -->
    <div class="nb-divider"></div>

    <!-- 文档列表 -->
    <div class="nb-doc-list">
      <div
        v-for="doc in docs"
        :key="doc.id"
        class="nb-doc-item"
      >
        <span class="doc-name" :title="doc.title">• {{ doc.title }}</span>
        <span class="doc-time">{{ formatTime(doc.updatedAt) }}</span>
      </div>
      <div v-if="docs.length === 0" class="nb-empty">
        暂无文档
      </div>
    </div>

    <!-- 卡片底部 -->
    <div class="nb-footer">
      <span class="doc-count">{{ book.documentCount }} 个文档</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon } from 'naive-ui'
import { BookOutline, EllipsisVerticalOutline } from '@vicons/ionicons5'
import type { NoteBook, NoteItem } from '@/types/note'

interface Props {
  book: NoteBook
  recentDocs?: NoteItem[]
  mode?: 'grid' | 'list'
}

const props = defineProps<Props>()

defineEmits<{
  click: []
  more: [book: NoteBook]
}>()

// 获取最近文档（避免 undefined）
const docs = computed(() => props.recentDocs || [])

// 颜色样式
const colorStyle = computed(() => {
  if (props.book.color) {
    return props.book.color
  }
  return 'linear-gradient(135deg, #14b8a6 0%, #06b6d4 100%)'
})

// 格式化时间
const formatTime = (dateStr: string): string => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`

  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}-${day.toString().padStart(2, '0')}`
}

const handleMore = () => {
  // 可以添加更多操作菜单
}
</script>

<style scoped>
.nb-card {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
}

.nb-card:hover {
  background: rgba(255, 255, 255, 0.55);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  border-color: rgba(255, 255, 255, 0.5);
}

.nb-card--grid:hover {
  transform: translateY(-4px);
}

.nb-card--list:hover {
  transform: translateX(4px);
}

.nb-card:active {
  transform: translateY(-2px);
}

/* 列表模式 */
.nb-card--list {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  gap: 16px;
  flex-direction: row;
}

.nb-card--list .nb-card-header {
  margin-bottom: 0;
  flex: 1;
  min-width: 0;
}

.nb-card--list .nb-icon {
  width: 36px;
  height: 36px;
}

.nb-card--list .nb-divider {
  display: none;
}

.nb-card--list .nb-doc-list {
  display: none;
}

.nb-card--list .nb-footer {
  display: none;
}

/* 卡片头部 */
.nb-card-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.nb-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #14b8a6 0%, #06b6d4 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  flex-shrink: 0;
}

.nb-info {
  flex: 1;
  min-width: 0;
}

.nb-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nb-subtitle {
  font-size: 13px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nb-more {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.nb-more:hover {
  background: rgba(0, 0, 0, 0.05);
}

/* 分隔线 */
.nb-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 12px 0;
}

/* 文档列表 */
.nb-doc-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  margin-bottom: 12px;
}

.nb-doc-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.doc-name {
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.doc-time {
  color: #999;
  font-size: 12px;
  flex-shrink: 0;
  margin-left: 8px;
}

.nb-empty {
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 13px;
  padding: 12px 0;
}

/* 卡片底部 */
.nb-footer {
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.doc-count {
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>

<template>
  <div class="kb-card" :class="[`kb-card--${mode}`]" @click="$emit('click')">
    <!-- 卡片头部 -->
    <div class="kb-card-header">
      <div class="kb-icon">
        <n-icon :component="FolderOutline" />
      </div>
      <div class="kb-info">
        <div class="kb-title" :title="kb.name">{{ kb.name }}</div>
        <div class="kb-subtitle" :title="kb.description || '暂无描述'">
          {{ kb.description || '暂无描述' }}
        </div>
      </div>
      <div class="kb-more" @click.stop="handleMore">
        <n-icon :component="EllipsisVerticalOutline" />
      </div>
    </div>

    <!-- 分隔线 -->
    <div class="kb-divider"></div>

    <!-- 文档列表 -->
    <div class="kb-doc-list">
      <div
        v-for="doc in docs"
        :key="doc.id"
        class="kb-doc-item"
      >
        <span class="doc-name" :title="doc.docName">• {{ doc.docName }}</span>
        <span class="doc-time">{{ formatTime(doc.createdAt) }}</span>
      </div>
      <div v-if="docs.length === 0" class="kb-empty">
        暂无文档
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon } from 'naive-ui'
import { FolderOutline, EllipsisVerticalOutline } from '@vicons/ionicons5'
import type { KnowledgeBase, KnowledgeDoc } from '@/types/knowledge'

interface Props {
  kb: KnowledgeBase
  recentDocs?: KnowledgeDoc[]
  mode?: 'grid' | 'list'
}

const props = defineProps<Props>()

defineEmits<{
  click: []
  more: [kb: KnowledgeBase]
}>()

// 获取最近文档（避免 undefined）
const docs = computed(() => props.recentDocs || [])

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
.kb-card {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.kb-card:hover {
  background: rgba(255, 255, 255, 0.55);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  border-color: rgba(255, 255, 255, 0.5);
}

.kb-card--grid:hover {
  transform: translateY(-4px);
}

.kb-card--list:hover {
  transform: translateX(4px);
}

.kb-card:active {
  transform: translateY(-2px);
}

/* 列表模式 */
.kb-card--list {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  gap: 16px;
}

.kb-card--list .kb-card-header {
  margin-bottom: 0;
  flex: 1;
  min-width: 0;
}

.kb-card--list .kb-icon {
  width: 36px;
  height: 36px;
}

.kb-card--list .kb-divider {
  display: none;
}

.kb-card--list .kb-doc-list {
  display: none;
}

/* 卡片头部 */
.kb-card-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.kb-icon {
  width: 40px;
  height: 40px;
  background: rgba(24, 144, 255, 0.1);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1890ff;
  font-size: 24px;
  flex-shrink: 0;
}

.kb-info {
  flex: 1;
  min-width: 0;
}

.kb-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-subtitle {
  font-size: 13px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-more {
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

.kb-more:hover {
  background: rgba(0, 0, 0, 0.05);
}

/* 分隔线 */
.kb-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 12px 0;
}

/* 文档列表 */
.kb-doc-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kb-doc-item {
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

.kb-empty {
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 13px;
  padding: 12px 0;
}
</style>

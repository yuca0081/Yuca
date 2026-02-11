<template>
  <!-- 第1层：页面容器（暗色遮罩背景） -->
  <div class="tools-page-container">
    <!-- 返回主页按钮 -->
    <n-button class="back-home-btn" @click="goBackHome">
      <template #icon>
        <n-icon :component="ArrowBackOutline" size="16" />
      </template>
      <span>返回主页</span>
    </n-button>

    <!-- 搜索框 -->
    <div class="tools-search-container">
      <n-input
        v-model:value="searchQuery"
        class="tools-search-input"
        placeholder="搜索工具..."
        clearable
        size="large"
      >
        <template #prefix>
          <n-icon :component="SearchOutline" size="18" />
        </template>
      </n-input>
    </div>

    <!-- 工具卡片容器 -->
    <div class="tools-cards-container">
      <div class="tools-grid">
        <div
          v-for="tool in filteredTools"
          :key="tool.id"
          class="tool-card"
          @click="navigateToTool(tool.route)"
        >
          <div class="tool-icon">
            <n-icon :component="tool.icon" size="28" />
          </div>
          <div class="tool-name">{{ tool.name }}</div>
          <div class="tool-description">{{ tool.description }}</div>
        </div>
      </div>

      <!-- 空状态 -->
      <n-empty
        v-if="filteredTools.length === 0"
        description="未找到匹配的工具"
        style="margin-top: 60px;"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NInput, NEmpty } from 'naive-ui'
import {
  ArrowBackOutline,
  SearchOutline,
  CodeWorkingOutline,
  ShieldCheckmarkOutline,
  SwapHorizontalOutline,
  LinkOutline,
  TimeOutline,
  KeyOutline,
  BarcodeOutline
} from '@vicons/ionicons5'

const router = useRouter()
const searchQuery = ref('')

// 工具定义
interface Tool {
  id: string
  name: string
  description: string
  icon: any
  route: string
  category: string
}

// 开发者工具列表
const developerTools: Tool[] = [
  {
    id: 'json-formatter',
    name: 'JSON 格式化',
    description: 'JSON 美化、压缩、验证',
    icon: CodeWorkingOutline,
    route: '/tools/json',
    category: 'developer'
  },
  {
    id: 'jwt-tool',
    name: 'JWT 加解密',
    description: 'JWT 令牌生成、解析、验证',
    icon: ShieldCheckmarkOutline,
    route: '/tools/jwt',
    category: 'developer'
  },
  {
    id: 'base64-tool',
    name: 'Base64 编解码',
    description: 'Base64 编码和解码',
    icon: SwapHorizontalOutline,
    route: '/tools/base64',
    category: 'developer'
  },
  {
    id: 'url-tool',
    name: 'URL 编解码',
    description: 'URL 编码和解码',
    icon: LinkOutline,
    route: '/tools/url',
    category: 'developer'
  },
  {
    id: 'timestamp-tool',
    name: '时间戳转换',
    description: 'Unix 时间戳转换',
    icon: TimeOutline,
    route: '/tools/timestamp',
    category: 'developer'
  },
  {
    id: 'hash-tool',
    name: '哈希生成',
    description: 'MD5、SHA1、SHA256 等',
    icon: KeyOutline,
    route: '/tools/hash',
    category: 'developer'
  },
  {
    id: 'regex-tool',
    name: '正则表达式测试',
    description: '正则表达式在线测试',
    icon: SearchOutline,
    route: '/tools/regex',
    category: 'developer'
  },
  {
    id: 'uuid-tool',
    name: 'UUID 生成器',
    description: 'UUID 生成和验证',
    icon: BarcodeOutline,
    route: '/tools/uuid',
    category: 'developer'
  }
]

// 搜索过滤
const filteredTools = computed(() => {
  if (!searchQuery.value) {
    return developerTools
  }

  const query = searchQuery.value.toLowerCase().trim()
  return developerTools.filter(tool =>
    tool.name.toLowerCase().includes(query) ||
    tool.description.toLowerCase().includes(query)
  )
})

// 返回主页
const goBackHome = () => {
  router.push('/')
}

// 导航到工具详情页
const navigateToTool = (route: string) => {
  router.push(route)
}
</script>

<style scoped>
/* 第1层：页面容器 - 暗色遮罩背景 */
.tools-page-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  min-height: 100vh;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7) 0%, rgba(30, 30, 30, 0.8) 100%) !important;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px 20px;
  z-index: 1;
}

/* 返回主页按钮 */
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

/* 搜索框容器 */
.tools-search-container {
  width: 100%;
  max-width: 600px;
  margin-bottom: 32px;
}

/* 搜索框 */
.tools-search-input {
  width: 100%;
}

.tools-search-input :deep(.n-input__input-el) {
  height: 48px;
  background: rgba(255, 255, 255, 0.75) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  border-radius: 24px !important;
  padding: 0 24px;
  font-size: 16px;
  color: var(--color-text-primary);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.1);
}

.tools-search-input :deep(.n-input__input-el:focus) {
  background: rgba(255, 255, 255, 0.85) !important;
  box-shadow: 0 6px 20px 0 rgba(0, 0, 0, 0.15);
}

.tools-search-input :deep(.n-input__border),
.tools-search-input :deep(.n-input__state-border) {
  border: none !important;
}

/* 卡片容器（可滚动） */
.tools-cards-container {
  width: 100%;
  max-width: 1200px;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  padding: 4px;
}

.tools-cards-container::-webkit-scrollbar {
  width: 8px;
}

.tools-cards-container::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.tools-cards-container::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.tools-cards-container::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

/* 工具卡片网格 */
.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

/* 工具卡片 */
.tool-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  aspect-ratio: 1;
}

.tool-card:hover {
  transform: translateY(-4px);
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 12px 40px 0 rgba(0, 0, 0, 0.45);
}

.tool-card:hover .tool-icon {
  background: rgba(156, 163, 175, 0.2);
  border-color: rgba(156, 163, 175, 0.5);
}

.tool-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(156, 163, 175, 0.15);
  border: 2px solid rgba(156, 163, 175, 0.4);
  border-radius: 16px;
  color: #6b7280;
}

.tool-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  text-align: center;
}

.tool-description {
  font-size: 12px;
  color: var(--color-text-secondary);
  text-align: center;
  line-height: 1.4;
}

/* 动画 */
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

.tool-card {
  animation: fadeInUp 0.3s ease-out;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .tools-page-container {
    padding: 20px 12px;
  }

  .tools-search-container {
    margin-bottom: 20px;
  }

  .tools-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 12px;
  }

  .back-home-btn {
    top: 12px;
    left: 12px;
    padding: 6px 12px !important;
    font-size: 12px !important;
    height: 32px !important;
  }
}
</style>

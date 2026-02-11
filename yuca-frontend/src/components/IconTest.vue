<template>
  <div class="icon-test">
    <h2>xicons 图标测试</h2>
    <div class="icon-list">
      <div class="icon-item">
        <n-icon :component="HomeOutline" size="32" color="#18a058" />
        <span>HomeOutline</span>
      </div>
      <div class="icon-item">
        <n-icon :component="SettingsOutline" size="32" color="#18a058" />
        <span>SettingsOutline</span>
      </div>
      <div class="icon-item">
        <n-icon :component="DocumentTextOutline" size="32" color="#18a058" />
        <span>DocumentTextOutline</span>
      </div>
      <div class="icon-item">
        <n-icon :component="FolderOutline" size="32" color="#18a058" />
        <span>FolderOutline</span>
      </div>
      <div class="icon-item">
        <n-icon :component="SearchOutline" size="32" color="#18a058" />
        <span>SearchOutline</span>
      </div>
    </div>

    <h3>在按钮中使用</h3>
    <div class="button-group">
      <n-button type="primary">
        <template #icon>
          <n-icon :component="HomeOutline" />
        </template>
        主页
      </n-button>

      <n-button type="info">
        <template #icon>
          <n-icon :component="SettingsOutline" />
        </template>
        设置
      </n-button>

      <n-button type="success">
        <template #icon>
          <n-icon :component="DocumentTextOutline" />
        </template>
        文档
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NIcon, NButton } from 'naive-ui'
import {
  HomeOutline,
  SettingsOutline,
  DocumentTextOutline,
  FolderOutline,
  SearchOutline
} from '@vicons/ionicons5'
</script>

<style scoped>
.icon-test {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
}

h2, h3 {
  margin-bottom: 20px;
  color: #333;
}

.icon-list {
  display: flex;
  gap: 20px;
  margin-bottom: 40px;
  flex-wrap: wrap;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  min-width: 100px;
}

.icon-item span {
  font-size: 12px;
  color: #666;
}

.button-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>

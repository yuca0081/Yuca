<script setup lang="ts">
import { NDialogProvider, NMessageProvider, NNotificationProvider } from 'naive-ui'
</script>

<template>
  <n-message-provider>
    <n-dialog-provider>
      <n-notification-provider>
        <router-view />
      </n-notification-provider>
    </n-dialog-provider>
  </n-message-provider>
</template>

<style>
/* 全局样式已在 main.css 中定义 */
</style>

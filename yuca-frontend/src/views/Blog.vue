<template>
  <div class="blog-container">
    <div class="card blog-card">
      <h1>博客</h1>
      <p>这里是我的博客内容区域</p>
      <p class="hint">功能开发中...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
// 博客页面
</script>

<style scoped>
.blog-container {
  min-height: 100vh;
  padding: 40px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.blog-card {
  max-width: 800px;
  width: 100%;
  padding: 40px;
}

.blog-card h1 {
  font-size: 32px;
  color: var(--color-text-primary);
  margin-bottom: 16px;
}

.blog-card p {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.hint {
  margin-top: 24px;
  font-style: italic;
}
</style>

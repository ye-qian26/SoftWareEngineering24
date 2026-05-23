<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
</script>

<template>
  <a-config-provider
    :theme="{
      token: {
        colorPrimary: '#0066FF',
        borderRadius: 12,
        fontFamily:
          'Inter, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica Neue, Arial, sans-serif',
      },
    }"
  >
    <BasicLayout />
  </a-config-provider>
</template>

<style>
/* 全局变量 - 极致简约风格 */
:root {
  --primary-color: #0066ff;
  --bg-color: #ffffff;
  --secondary-bg: #f5f5f7;
  --text-main: #1d1d1f;
  --text-secondary: #86868b;
  --border-color: rgba(0, 0, 0, 0.06);
  --card-shadow: 0 8px 30px rgba(0, 0, 0, 0.04);
  --hover-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  --transition-base: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue',
    Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: var(--bg-color);
  color: var(--text-main);
  overflow-x: hidden;
}

/* 隐藏滚动条但保留功能 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 10px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

html {
  overflow-x: hidden;
  scroll-behavior: smooth;
}

a {
  color: var(--primary-color);
  text-decoration: none;
  transition: var(--transition-base);
}

a:hover {
  opacity: 0.8;
}

/* 覆盖 Ant Design 默认样式以匹配简约风格 */
.ant-btn {
  box-shadow: none !important;
  font-weight: 500;
}

.ant-card {
  border: none !important;
  box-shadow: var(--card-shadow);
  border-radius: 16px !important;
}

.ant-input,
.ant-input-password,
.ant-input-affix-wrapper {
  border-radius: 8px !important;
  background: var(--secondary-bg) !important;
  border: 1px solid transparent !important;
  transition: var(--transition-base) !important;
}

.ant-input:focus,
.ant-input-affix-wrapper-focused {
  background: #fff !important;
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(0, 102, 255, 0.1) !important;
}
</style>

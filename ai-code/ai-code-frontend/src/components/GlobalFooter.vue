<template>

</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
.footer {
  background: var(--bg-color);
  text-align: center;
  padding: 40px 20px;
  margin-top: 60px;
  border-top: 1px solid var(--border-color);
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
}

.copyright {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  letter-spacing: 0.5px;
}

.author-link {
  color: var(--text-secondary);
  text-decoration: none;
  transition: var(--transition-base);
  font-weight: 500;
}

.author-link:hover {
  color: var(--primary-color);
}
</style>

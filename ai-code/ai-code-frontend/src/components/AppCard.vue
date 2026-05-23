<template>
  <div class="app-card" @click="handleViewChat">
    <div v-if="featured" class="featured-badge">精选</div>
    <div class="app-preview">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
      <div v-else class="app-placeholder">
        <span>🤖</span>
      </div>
      <div class="app-overlay">
        <a-space size="middle">
          <a-button type="primary" shape="round" @click.stop="handleViewChat">
            立即体验
          </a-button>
          <a-button
            v-if="app.deployKey"
            type="default"
            ghost
            shape="round"
            @click.stop="handleViewWork"
          >
            查看作品
          </a-button>
        </a-space>
      </div>
    </div>
    <div class="app-info">
      <div class="app-info-left">
        <a-avatar :src="app.user?.userAvatar" :size="40">
          {{ app.user?.userName?.charAt(0) || 'U' }}
        </a-avatar>
      </div>
      <div class="app-info-right">
        <h3 class="app-title">{{ app.appName || '未命名应用' }}</h3>
        <p class="app-author">
          {{ app.user?.userName || (featured ? '官方推荐' : '未知用户') }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<Emits>()

const handleViewChat = () => {
  emit('view-chat', props.app.id)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}
</script>

<style scoped>
.app-card {
  background: var(--bg-color);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
  transition: var(--transition-base);
  cursor: pointer;
  position: relative;
}

.app-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--hover-shadow);
  border-color: var(--primary-color);
}

.app-preview {
  height: 200px;
  background: var(--secondary-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.app-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.app-card:hover .app-preview img {
  transform: scale(1.05);
}

.app-placeholder {
  font-size: 56px;
  opacity: 0.8;
}

.app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: var(--transition-base);
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.app-info {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-info-left {
  flex-shrink: 0;
}

.app-info-right {
  flex-grow: 1;
  overflow: hidden;
}

.app-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-author {
  margin: 4px 0 0;
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.featured-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  background: var(--primary-color);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  z-index: 10;
  box-shadow: 0 4px 12px rgba(0, 102, 255, 0.3);
}
</style>

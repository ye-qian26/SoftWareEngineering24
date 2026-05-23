<template>
  <a-modal v-model:open="visible" title="部署成功" :footer="null" centered width="480px">
    <div class="deploy-success">
      <div class="success-icon">
        <div class="icon-wrapper">
          <CheckOutlined />
        </div>
      </div>
      <h3 class="success-title">发布成功</h3>
      <p class="success-desc">您的网站已成功部署到云端，现在可以向全世界分享了！</p>
      
      <div class="url-card">
        <div class="url-label">访问链接</div>
        <div class="url-content">
          <span class="url-text">{{ deployUrl }}</span>
          <a-button type="text" size="small" @click="handleCopyUrl" class="copy-btn">
            <template #icon><CopyOutlined /></template>
          </a-button>
        </div>
      </div>

      <div class="deploy-actions">
        <a-button type="primary" size="large" block @click="handleOpenSite" class="open-btn">
          立即访问
        </a-button>
        <a-button type="text" size="large" block @click="handleClose" class="close-btn">
          返回编辑器
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { CheckOutlined, CopyOutlined } from '@ant-design/icons-vue'

interface Props {
  open: boolean
  deployUrl: string
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'open-site'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const handleCopyUrl = async () => {
  try {
    await navigator.clipboard.writeText(props.deployUrl)
    message.success('链接已复制')
  } catch (error) {
    message.error('复制失败')
  }
}

const handleOpenSite = () => {
  emit('open-site')
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.deploy-success {
  text-align: center;
  padding: 12px 0;
}

.success-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.icon-wrapper {
  width: 80px;
  height: 80px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #52c41a;
  box-shadow: 0 8px 24px rgba(82, 196, 26, 0.1);
}

.success-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 12px;
}

.success-desc {
  font-size: 15px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 32px;
}

.url-card {
  background: var(--secondary-bg);
  padding: 20px;
  border-radius: 16px;
  text-align: left;
  margin-bottom: 32px;
}

.url-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.url-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.url-text {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 14px;
  color: var(--primary-color);
  word-break: break-all;
  font-weight: 600;
}

.copy-btn {
  color: var(--text-secondary);
  transition: var(--transition-base);
}

.copy-btn:hover {
  color: var(--primary-color);
  background: white;
}

.deploy-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.open-btn {
  height: 54px;
  font-weight: 700;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0, 102, 255, 0.2) !important;
}

.close-btn {
  height: 48px;
  font-weight: 600;
  color: var(--text-secondary);
}

:deep(.ant-modal-content) {
  border-radius: 32px !important;
  padding: 40px !important;
}

:deep(.ant-modal-header) {
  display: none;
}
</style>

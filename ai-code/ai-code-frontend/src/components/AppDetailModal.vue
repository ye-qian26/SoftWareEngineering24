<template>
  <a-modal v-model:open="visible" title="应用详情" :footer="null" centered width="440px">
    <div class="app-detail-content">
      <!-- 应用基础信息 -->
      <div class="app-basic-info">
        <div class="info-item">
          <span class="info-label">创建者</span>
          <UserInfo :user="app?.user" size="small" />
        </div>
        <div class="info-item">
          <span class="info-label">创建时间</span>
          <span class="info-value">{{ formatTime(app?.createTime) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">生成类型</span>
          <a-tag v-if="app?.codeGenType" color="processing">
            {{ formatCodeGenType(app.codeGenType) }}
          </a-tag>
          <span v-else class="info-value">未知类型</span>
        </div>
      </div>

      <!-- 操作栏（仅本人或管理员可见） -->
      <div v-if="showActions" class="app-actions">
        <a-space size="middle">
          <a-button type="default" @click="handleEdit" shape="round">
            <template #icon><EditOutlined /></template>
            编辑信息
          </a-button>
          <a-popconfirm
            title="确定要删除这个应用吗？"
            @confirm="handleDelete"
            ok-text="确定"
            cancel-text="取消"
          >
            <a-button danger type="primary" ghost shape="round">
              <template #icon><DeleteOutlined /></template>
              删除应用
            </a-button>
          </a-popconfirm>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import UserInfo from './UserInfo.vue'
import { formatTime } from '@/utils/time'
import {formatCodeGenType} from "../utils/codeGenTypes.ts";

interface Props {
  open: boolean
  app?: API.AppVO
  showActions?: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'edit'): void
  (e: 'delete'): void
}

const props = withDefaults(defineProps<Props>(), {
  showActions: false,
})

const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const handleEdit = () => {
  emit('edit')
}

const handleDelete = () => {
  emit('delete')
}
</script>

<style scoped>
.app-detail-content {
  padding: 12px 0;
}

.app-basic-info {
  background: var(--secondary-bg);
  padding: 20px;
  border-radius: 16px;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.info-label {
  color: var(--text-secondary);
  font-weight: 500;
}

.info-value {
  color: var(--text-main);
  font-weight: 600;
}

.app-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

:deep(.ant-modal-content) {
  border-radius: 24px !important;
  padding: 32px !important;
}

:deep(.ant-modal-header) {
  margin-bottom: 24px !important;
}

:deep(.ant-modal-title) {
  font-size: 20px !important;
  font-weight: 700 !important;
}
</style>

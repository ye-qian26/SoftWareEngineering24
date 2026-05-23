<template>
  <div class="app-chat-page">
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '应用对话' }}</h1>
        <a-tag v-if="appInfo?.codeGenType" color="processing">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
      </div>
      <a-space class="header-actions" wrap>
        <a-button type="text" @click="showAppDetail">
          <template #icon><InfoCircleOutlined /></template>
          详情
        </a-button>
        <a-button v-if="canManageApp" :loading="downloading" @click="downloadCode">
          <template #icon><DownloadOutlined /></template>
          下载代码
        </a-button>
        <a-button v-if="canManageApp" type="primary" :loading="deploying" @click="deployApp">
          <template #icon><CloudUploadOutlined /></template>
          部署
        </a-button>
      </a-space>
    </div>

    <div class="main-content">
      <section class="chat-section">
        <div class="section-header">
          <h3>AI 对话</h3>
          <a-tag v-if="isOwner" color="success">当前应用作者</a-tag>
        </div>

        <div class="chat-card">
          <div ref="messagesContainer" class="messages-container">
            <div v-if="hasMoreHistory" class="load-more">
              <a-button type="link" size="small" :loading="loadingHistory" @click="loadMoreHistory">
                加载更多历史消息
              </a-button>
            </div>

            <div v-for="item in messages" :key="item.id" class="message-item">
              <div v-if="item.type === 'user'" class="message-row user-row">
                <div class="message-bubble user-bubble">{{ item.content }}</div>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
              <div v-else class="message-row ai-row">
                <a-avatar :src="aiAvatar" />
                <div class="message-bubble ai-bubble">
                  <MarkdownRenderer v-if="item.content" :content="item.content" />
                  <div v-if="item.loading" class="loading-indicator">
                    <a-spin size="small" />
                    <span>AI 正在持续输出...</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <a-alert
            v-if="selectedElementInfo"
            class="selected-alert"
            type="info"
            show-icon
            closable
            @close="clearSelectedElement"
          >
            <template #message>
              已选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
            </template>
            <template #description>
              <div class="selected-meta">
                <code>{{ selectedElementInfo.selector }}</code>
                <span v-if="selectedElementInfo.id">ID: {{ selectedElementInfo.id }}</span>
              </div>
            </template>
          </a-alert>

          <div class="input-container">
            <a-textarea
              v-model:value="userInput"
              :rows="4"
              :maxlength="1000"
              :placeholder="inputPlaceholder"
              :disabled="!canSendMessage || isGenerating"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <div class="input-actions">
              <span class="input-tip">
                {{ canSendMessage ? 'Enter 发送，Shift + Enter 换行' : '仅应用作者可继续生成代码' }}
              </span>
              <a-button
                type="primary"
                :loading="isGenerating"
                :disabled="!canSendMessage"
                @click="sendMessage"
              >
                <template #icon><SendOutlined /></template>
                发送
              </a-button>
            </div>
          </div>
        </div>
      </section>

      <section class="preview-section">
        <div class="section-header">
          <h3>实时预览</h3>
          <a-space size="small" wrap>
            <a-button
              v-if="previewUrl"
              type="text"
              size="small"
              :danger="isEditMode"
              @click="toggleEditMode"
            >
              <template #icon><EditOutlined /></template>
              {{ isEditMode ? '退出选区模式' : '选区编辑模式' }}
            </a-button>
            <a-button v-if="previewUrl" type="text" size="small" @click="openInNewTab">
              <template #icon><ExportOutlined /></template>
              新窗口打开
            </a-button>
          </a-space>
        </div>

        <div class="preview-card">
          <a-alert
            v-if="previewStatusText"
            :message="previewStatusText"
            :type="previewUrl ? 'info' : 'warning'"
            banner
          />

          <div v-if="!previewUrl && isGenerating" class="preview-state">
            <a-spin size="large" />
            <p>正在生成或构建预览，请稍候。</p>
          </div>

          <div v-else-if="!previewUrl" class="preview-state">
            <div class="preview-icon">Preview</div>
            <p>代码生成完成后，会在这里显示预览。</p>
          </div>

          <iframe
            v-else
            class="preview-iframe"
            :src="previewUrl"
            frameborder="0"
            @load="onIframeLoad"
          />
        </div>
      </section>
    </div>

    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="canManageApp"
      @edit="editApp"
      @delete="deleteApp"
    />

    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CloudUploadOutlined,
  DownloadOutlined,
  EditOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  SendOutlined,
} from '@ant-design/icons-vue'
import { deleteApp as deleteAppApi, deployApp as deployAppApi, getAppVoById } from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { useLoginUserStore } from '@/stores/loginUser'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import { type ElementInfo, VisualEditor } from '@/utils/visualEditor'
import aiAvatar from '@/assets/touxiang.jpg'

interface ChatMessage {
  id: string
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

interface StreamErrorPayload {
  code?: number
  message?: string
  retryable?: boolean
  preservePreview?: boolean
}

interface GenerationPromptPayload {
  prompt: string
  expectsProjectChange: boolean
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appInfo = ref<API.AppVO>()
const appId = ref<string>()
const messages = ref<ChatMessage[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const historyLoaded = ref(false)
const lastCreateTime = ref<string>()
const messagesContainer = ref<HTMLElement>()

const previewUrl = ref('')
const previewReady = ref(false)
const previewStatusText = ref('等待生成预览')

const appDetailVisible = ref(false)
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')
const downloading = ref(false)

const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

let currentEventSource: EventSource | null = null
let localMessageId = 0

const isOwner = computed(() => {
  const appUserId = appInfo.value?.userId
  const loginUserId = loginUserStore.loginUser.id
  return appUserId !== undefined && loginUserId !== undefined && String(appUserId) === String(loginUserId)
})
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const canManageApp = computed(() => isOwner.value || isAdmin.value)
const canSendMessage = computed(() => isOwner.value)
const inputPlaceholder = computed(() => {
  if (selectedElementInfo.value) {
    return `描述你想修改的 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，AI 会结合选中节点继续生成。`
  }
  return '描述你想生成或修改的页面、组件、交互和样式。'
})

const createMessageId = () => {
  localMessageId += 1
  return `${Date.now()}-${localMessageId}`
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const closeCurrentStream = () => {
  currentEventSource?.close()
  currentEventSource = null
}

const appendSystemNote = (content: string, note: string) => {
  const trimmed = content.trim()
  return trimmed ? `${trimmed}\n\n---\n${note}` : note
}

const buildVueProjectEditRules = () => {
  return [
    '[PROJECT_EDIT_RULES]',
    '- This is a follow-up modification request for an existing generated Vue project.',
    '- Do not answer with explanation only.',
    '- Inspect the current files when needed, then apply real code changes with modifyFile or writeFile.',
    '- Prefer targeted edits to the page or component that renders the requested area.',
    '- Keep unrelated files unchanged.',
    '- End with a short completion message after the edits are done.',
  ].join('\n')
}

const buildSelectedElementContext = (elementInfo: ElementInfo) => {
  const lines = [
    '[SELECTED_ELEMENT_CONTEXT]',
    `- Tag: ${elementInfo.tagName.toLowerCase()}`,
    `- Selector: ${elementInfo.selector}`,
    `- Route hint: ${elementInfo.pagePath || '/'}`,
  ]

  if (elementInfo.id) {
    lines.push(`- ID: ${elementInfo.id}`)
  }
  if (elementInfo.className) {
    lines.push(`- Class names: ${elementInfo.className}`)
  }
  if (elementInfo.textContent) {
    lines.push(`- Text snippet: ${elementInfo.textContent.slice(0, 160)}`)
  }

  return lines.join('\n')
}

const GENERAL_CHAT_PATTERNS = [
  /^(你好|您好|hello|hi|hey|嗨|在吗|早上好|下午好|晚上好)[\s!,.，。？！]*$/i,
  /你是谁|你能做什么|介绍一下|解释一下|总结一下|聊聊|这个项目.*(是什么|干嘛|做什么)|为什么|怎么(做|实现|回事|用)/i,
]

const PROJECT_EDIT_PATTERNS = [
  /修改|改成|改为|改下|调整|优化|新增|增加|添加|删除|去掉|移除|替换|重写|实现|修复|做成|做个|加个|换成|补充|完善|美化|重构|布局/i,
  /\b(change|modify|update|edit|add|remove|delete|fix|implement|create|replace|rewrite|redesign|refactor|style)\b/i,
]

const shouldTreatAsProjectEdit = (rawPrompt: string) => {
  const prompt = rawPrompt.trim()
  if (!prompt) {
    return false
  }
  if (PROJECT_EDIT_PATTERNS.some((pattern) => pattern.test(prompt))) {
    return true
  }
  if (GENERAL_CHAT_PATTERNS.some((pattern) => pattern.test(prompt))) {
    return false
  }
  return true
}

const buildGenerationPrompt = (rawPrompt: string): GenerationPromptPayload => {
  const sections = [rawPrompt]
  const expectsProjectChange =
    appInfo.value?.codeGenType === CodeGenTypeEnum.VUE_PROJECT
      ? shouldTreatAsProjectEdit(rawPrompt)
      : true

  if (appInfo.value?.codeGenType === CodeGenTypeEnum.VUE_PROJECT && expectsProjectChange) {
    sections.push(buildVueProjectEditRules())
  }

  if (selectedElementInfo.value) {
    sections.push(buildSelectedElementContext(selectedElementInfo.value))
  }

  return {
    prompt: sections.join('\n\n'),
    expectsProjectChange,
  }
}

const buildPreviewUrl = () => {
  const type = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
  const baseUrl = getStaticPreviewUrl(type, String(appId.value))
  const separator = baseUrl.includes('?') ? '&' : '?'
  return {
    probeUrl: baseUrl,
    iframeUrl: `${baseUrl}${separator}t=${Date.now()}`,
    codeGenType: type,
  }
}

const updatePreview = async (options?: {
  preserveCurrent?: boolean
  fallbackUrl?: string
  fallbackReady?: boolean
}) => {
  if (!appId.value) {
    return false
  }

  const previousUrl = options?.fallbackUrl ?? previewUrl.value
  const previousReady = options?.fallbackReady ?? previewReady.value
  const { probeUrl, iframeUrl, codeGenType } = buildPreviewUrl()

  try {
    const response = await fetch(probeUrl, {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    })

    if (!response.ok) {
      if (options?.preserveCurrent && previousUrl) {
        previewUrl.value = previousUrl
        previewReady.value = previousReady
      } else {
        previewUrl.value = ''
        previewReady.value = false
      }
      previewStatusText.value =
        codeGenType === CodeGenTypeEnum.VUE_PROJECT
          ? 'Vue 预览文件尚未生成完成，请稍后再试。'
          : '预览资源暂未就绪。'
      return false
    }

    previewUrl.value = iframeUrl
    previewReady.value = false
    previewStatusText.value = '预览已更新'
    return true
  } catch (error) {
    console.error('Failed to refresh preview', error)
    if (options?.preserveCurrent && previousUrl) {
      previewUrl.value = previousUrl
      previewReady.value = previousReady
    } else {
      previewUrl.value = ''
      previewReady.value = false
    }
    previewStatusText.value = '预览检查失败，请稍后刷新重试。'
    return false
  }
}

const loadChatHistory = async (loadMore = false) => {
  if (!appId.value || loadingHistory.value) {
    return
  }

  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    if (loadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }

    const res = await listAppChatHistory(params)
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '加载历史消息失败')
    }

    const records = res.data.data.records || []
    const historyMessages = records
      .map((item) => ({
        id: createMessageId(),
        type: item.messageType === 'user' ? 'user' : 'ai',
        content: item.message || '',
        createTime: item.createTime,
      }))
      .reverse() as ChatMessage[]

    if (loadMore) {
      messages.value.unshift(...historyMessages)
    } else {
      messages.value = historyMessages
    }

    lastCreateTime.value = records[records.length - 1]?.createTime
    hasMoreHistory.value = records.length === 10
    historyLoaded.value = true
  } catch (error) {
    console.error('Failed to load chat history', error)
    message.error('加载历史消息失败')
  } finally {
    loadingHistory.value = false
    await nextTick()
    scrollToBottom()
  }
}

const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

const fetchAppInfo = async (allowInitMessage = true) => {
  const currentId = String(route.params.id || '').trim()
  if (!/^\d+$/.test(currentId)) {
    message.error('应用 ID 无效')
    router.push('/')
    return
  }

  appId.value = currentId

  try {
    const res = await getAppVoById({ id: currentId })
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '获取应用信息失败')
    }

    appInfo.value = res.data.data
    await loadChatHistory()

    if (messages.value.length > 0) {
      await updatePreview({ preserveCurrent: true })
    } else {
      previewStatusText.value = '代码生成完成后，会在这里显示预览。'
    }

    if (allowInitMessage && appInfo.value.initPrompt && isOwner.value && messages.value.length === 0 && historyLoaded.value) {
      await sendInitialMessage(appInfo.value.initPrompt)
    }
  } catch (error) {
    console.error('Failed to fetch app info', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

const sendInitialMessage = async (prompt: string) => {
  messages.value.push({
    id: createMessageId(),
    type: 'user',
    content: prompt,
  })
  const aiMessageIndex = messages.value.length
  messages.value.push({
    id: createMessageId(),
    type: 'ai',
    content: '',
    loading: true,
  })
  await nextTick()
  scrollToBottom()
  await generateCode(prompt, aiMessageIndex, true)
}

const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value || !canSendMessage.value) {
    return
  }

  const rawPrompt = userInput.value.trim()
  const { prompt: finalPrompt, expectsProjectChange } = buildGenerationPrompt(rawPrompt)
  const displayContent = rawPrompt

  userInput.value = ''
  messages.value.push({
    id: createMessageId(),
    type: 'user',
    content: displayContent,
  })
  const aiMessageIndex = messages.value.length
  messages.value.push({
    id: createMessageId(),
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()
  await generateCode(finalPrompt, aiMessageIndex, expectsProjectChange)
}

const generateCode = async (prompt: string, aiMessageIndex: number, expectsProjectChange = true) => {
  if (!appId.value) {
    return
  }

  closeCurrentStream()
  isGenerating.value = true

  const previousPreviewUrl = previewUrl.value
  const previousPreviewReady = previewReady.value
  let fullContent = messages.value[aiMessageIndex]?.content || ''
  let streamCompleted = false

  const finishStream = async () => {
    if (streamCompleted) {
      return
    }
    streamCompleted = true
    closeCurrentStream()
    isGenerating.value = false
    if (messages.value[aiMessageIndex]) {
      messages.value[aiMessageIndex].loading = false
    }
    await nextTick()
    scrollToBottom()
  }

  const applyStreamError = async (payload: StreamErrorPayload, level: 'warning' | 'error' = 'warning') => {
    const errorText = payload.message || '生成过程中发生系统异常，本次输出已中断。'
    const note = payload.retryable
      ? `系统中断：${errorText}。已保留当前输出和可用预览，你可以继续补充需求后再次生成。`
      : `系统异常：${errorText}。已保留当前输出和可用预览。`

    if (messages.value[aiMessageIndex]) {
      messages.value[aiMessageIndex].content = appendSystemNote(fullContent, note)
      messages.value[aiMessageIndex].loading = false
    }

    previewStatusText.value = errorText
    if (level === 'error') {
      message.error(errorText)
    } else {
      message.warning(errorText)
    }

    await finishStream()
    if (expectsProjectChange && payload.preservePreview !== false) {
      await updatePreview({
        preserveCurrent: true,
        fallbackUrl: previousPreviewUrl,
        fallbackReady: previousPreviewReady,
      })
    }
  }

  try {
    const searchParams = new URLSearchParams({
      appId: String(appId.value),
      message: prompt,
    })
    const baseUrl = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL
    const eventSource = new EventSource(`${baseUrl}/app/chat/gen/code?${searchParams.toString()}`, {
      withCredentials: true,
    })
    currentEventSource = eventSource

    eventSource.onmessage = async (event: MessageEvent<string>) => {
      if (streamCompleted) {
        return
      }
      try {
        const data = JSON.parse(event.data)
        const chunk = typeof data?.d === 'string' ? data.d : ''
        if (!chunk) {
          return
        }
        fullContent += chunk
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].content = fullContent
        }
        await nextTick()
        scrollToBottom()
      } catch (error) {
        console.error('Failed to parse stream message', error, event.data)
        await applyStreamError(
          {
            message: '服务端返回了无法解析的流式数据。',
            retryable: true,
            preservePreview: true,
          },
          'error',
        )
      }
    }

    eventSource.addEventListener('done', async () => {
      await finishStream()
      if (expectsProjectChange) {
        await updatePreview({
          preserveCurrent: true,
          fallbackUrl: previousPreviewUrl,
          fallbackReady: previousPreviewReady,
        })
      }
    })

    eventSource.addEventListener('business-error', async (event: MessageEvent<string>) => {
      try {
        const errorData = JSON.parse(event.data) as StreamErrorPayload
        await applyStreamError(
          {
            ...errorData,
            preservePreview: true,
          },
          'error',
        )
      } catch (error) {
        console.error('Failed to parse business-error event', error, event.data)
        await applyStreamError(
          {
            message: '服务端返回了无法解析的错误信息。',
            retryable: false,
            preservePreview: true,
          },
          'error',
        )
      }
    })

    eventSource.addEventListener('stream-error', async (event: MessageEvent<string>) => {
      try {
        const errorData = JSON.parse(event.data) as StreamErrorPayload
        await applyStreamError(errorData, 'warning')
      } catch (error) {
        console.error('Failed to parse stream-error event', error, event.data)
        await applyStreamError(
          {
            message: '服务端返回了无法解析的流错误信息。',
            retryable: true,
            preservePreview: true,
          },
          'error',
        )
      }
    })

    eventSource.onerror = async () => {
      if (streamCompleted || !isGenerating.value) {
        return
      }
      await applyStreamError(
        {
          message: '连接已中断，本次生成已停止。',
          retryable: true,
          preservePreview: true,
        },
        'error',
      )
    }
  } catch (error) {
    console.error('Failed to create EventSource', error)
    await applyStreamError(
      {
        message: '无法建立生成连接。',
        retryable: true,
        preservePreview: true,
      },
      'error',
    )
  }
}

const downloadCode = async () => {
  if (!appId.value) {
    return
  }

  downloading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/app/download/${appId.value}`, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`Download failed with status ${response.status}`)
    }

    const disposition = response.headers.get('Content-Disposition') || ''
    const fileName = disposition.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    const blob = await response.blob()
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = fileName
    link.click()
    URL.revokeObjectURL(objectUrl)
    message.success('代码包下载成功')
  } catch (error) {
    console.error('Failed to download code', error)
    message.error('下载代码失败')
  } finally {
    downloading.value = false
  }
}

const deployApp = async () => {
  if (!appId.value) {
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({ appId: appId.value })
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '部署失败')
    }
    deployUrl.value = res.data.data
    deployModalVisible.value = true
    message.success('部署成功')
  } catch (error) {
    console.error('Failed to deploy app', error)
    message.error('部署失败')
  } finally {
    deploying.value = false
  }
}

const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank', 'noopener,noreferrer')
  }
}

const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank', 'noopener,noreferrer')
  }
}

const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement | null
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

const toggleEditMode = () => {
  if (!previewUrl.value || !previewReady.value) {
    message.warning('预览还没有准备好，暂时无法选择页面元素。')
    return
  }
  isEditMode.value = visualEditor.toggleEditMode()
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const showAppDetail = () => {
  appDetailVisible.value = true
}

const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

const deleteApp = async () => {
  if (!appInfo.value?.id) {
    return
  }

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '删除失败')
    }
    message.success('应用已删除')
    appDetailVisible.value = false
    router.push('/')
  } catch (error) {
    console.error('Failed to delete app', error)
    message.error('删除应用失败')
  }
}

const handlePreviewMessage = (event: MessageEvent) => {
  visualEditor.handleIframeMessage(event)
}

onMounted(() => {
  fetchAppInfo()
  window.addEventListener('message', handlePreviewMessage)
})

onUnmounted(() => {
  closeCurrentStream()
  window.removeEventListener('message', handlePreviewMessage)
})
</script>

<style scoped>
.app-chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 72px);
  background: var(--secondary-bg);
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 24px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.app-name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}

.main-content {
  display: grid;
  grid-template-columns: minmax(360px, 520px) minmax(0, 1fr);
  gap: 20px;
  flex: 1;
  min-height: 0;
  padding: 20px 24px 24px;
}

.chat-section,
.preview-section {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.section-header h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.chat-card,
.preview-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: var(--card-shadow);
}

.messages-container {
  flex: 1;
  min-height: 0;
  padding: 24px;
  overflow-y: auto;
  background: #fafafa;
}

.load-more {
  text-align: center;
  margin-bottom: 12px;
}

.message-item + .message-item {
  margin-top: 18px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.user-row {
  justify-content: flex-end;
}

.ai-row {
  justify-content: flex-start;
}

.message-bubble {
  max-width: min(100%, 640px);
  border-radius: 18px;
  padding: 14px 16px;
  line-height: 1.6;
  word-break: break-word;
}

.user-bubble {
  background: var(--primary-color);
  color: #fff;
  border-top-right-radius: 6px;
}

.ai-bubble {
  width: 100%;
  background: #fff;
  border: 1px solid var(--border-color);
  border-top-left-radius: 6px;
}

.loading-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

.selected-alert {
  margin: 0 24px 16px;
  border-radius: 12px;
}

.selected-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.selected-meta code {
  font-family: Consolas, monospace;
}

.input-container {
  border-top: 1px solid var(--border-color);
  padding: 18px 20px 20px;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.input-tip {
  color: var(--text-secondary);
  font-size: 12px;
}

.preview-card {
  position: relative;
}

.preview-state {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 32px;
  color: var(--text-secondary);
  text-align: center;
}

.preview-icon {
  padding: 10px 16px;
  border-radius: 999px;
  background: #f0f5ff;
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.preview-iframe {
  flex: 1;
  width: 100%;
  min-height: 0;
  border: none;
  background: #fff;
}

@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .chat-section {
    min-height: 460px;
  }

  .preview-section {
    min-height: 520px;
  }
}

@media (max-width: 768px) {
  .app-chat-page {
    height: auto;
    min-height: calc(100vh - 72px);
  }

  .header-bar,
  .main-content {
    padding-left: 16px;
    padding-right: 16px;
  }

  .header-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .messages-container {
    padding: 16px;
  }

  .input-container {
    padding: 16px;
  }

  .input-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

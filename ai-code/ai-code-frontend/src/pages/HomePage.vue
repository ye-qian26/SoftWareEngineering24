<template>
  <div class="home-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="gradient-text">AI 驱动</span> 创意无限
        </h1>
        <p class="hero-subtitle">
          输入您的想法，AI 瞬间为您构建功能完备、设计精美的应用程序
        </p>

        <div class="create-input-container">
          <a-input-search
            v-model:value="userPrompt"
            placeholder="描述您想创建的应用，例如：一个极简风格的待办事项..."
            size="large"
            :loading="creating"
            @search="createApp"
          >
            <template #enterButton>
              <a-button type="primary" size="large" :loading="creating">
                <template #icon><ThunderboltOutlined /></template>
                立即生成
              </a-button>
            </template>
          </a-input-search>
          <div class="quick-prompts">
            <span>试一试：</span>
            <a-tag @click="setPrompt('一个极简风格的待办事项')">极简待办</a-tag>
            <a-tag @click="setPrompt('程序员专属的记账工具')">记账工具</a-tag>
            <a-tag @click="setPrompt('基于 Markdown 的个人博客')">个人博客</a-tag>
          </div>
        </div>
      </div>
    </section>

    <!-- Content Section -->
    <div class="content-container">
      <!-- 精选应用 -->
      <section class="app-section">
        <div class="section-header">
          <h2 class="section-title">精选作品</h2>
          <p class="section-desc">探索社区中最优秀的 AI 生成作品</p>
        </div>
        <a-list
          :grid="{ gutter: 24, xs: 1, sm: 2, md: 2, lg: 3, xl: 3, xxl: 3 }"
          :data-source="featuredApps"
          :pagination="featuredAppsPage.total > featuredAppsPage.pageSize ? featuredAppsPage : false"
        >
          <template #renderItem="{ item }">
            <a-list-item style="padding: 0">
              <AppCard :app="item" featured @view-chat="handleViewChat" @view-work="handleViewWork" />
            </a-list-item>
          </template>
        </a-list>
      </section>

      <!-- 我的应用 -->
      <section v-if="loginUserStore.loginUser.id" class="app-section">
        <div class="section-header">
          <h2 class="section-title">我的创作</h2>
          <p class="section-desc">您所有的创意结晶都在这里</p>
        </div>
        <a-list
          :grid="{ gutter: 24, xs: 1, sm: 2, md: 2, lg: 3, xl: 3, xxl: 3 }"
          :data-source="myApps"
          :pagination="myAppsPage.total > myAppsPage.pageSize ? myAppsPage : false"
        >
          <template #renderItem="{ item }">
            <a-list-item style="padding: 0">
              <AppCard :app="item" @view-chat="handleViewChat" @view-work="handleViewWork" />
            </a-list-item>
          </template>
        </a-list>
      </section>
    </div>

    <!-- 作品详情弹窗 -->
    <AppDetailModal v-model:open="showWorkModal" :app="selectedApp" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import AppCard from '@/components/AppCard.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import { ThunderboltOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 详情弹窗控制
const showWorkModal = ref(false)
const selectedApp = ref<API.AppVO>()

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
  onChange: (page: number) => {
    myAppsPage.current = page
    loadMyApps()
  },
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
  onChange: (page: number) => {
    featuredAppsPage.current = page
    loadFeaturedApps()
  },
})

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) return

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

// 处理查看对话
const handleViewChat = (appId?: string | number) => {
  if (appId === undefined || appId === null) {
    return
  }
  router.push(`/app/chat/${appId}`)
}

// 处理查看作品
const handleViewWork = (app: API.AppVO) => {
  selectedApp.value = app
  showWorkModal.value = true
}

onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
})
</script>

<style scoped>
.home-page {
  background-color: var(--bg-color);
  min-height: 100vh;
}

.hero-section {
  padding: 100px 20px 80px;
  background: radial-gradient(circle at 50% -20%, #e6f0ff 0%, #ffffff 60%);
  text-align: center;
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 56px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 24px;
  letter-spacing: -2px;
  line-height: 1.1;
}

.gradient-text {
  background: linear-gradient(135deg, #0066ff 0%, #00d2ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  font-size: 20px;
  color: var(--text-secondary);
  margin-bottom: 48px;
  line-height: 1.6;
  font-weight: 500;
}

.create-input-container {
  max-width: 640px;
  margin: 0 auto;
}

:deep(.ant-input-search .ant-input) {
  height: 60px;
  font-size: 16px;
  padding: 0 24px;
  border-radius: 30px !important;
  border: 1px solid var(--border-color) !important;
  background: white !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04) !important;
}

:deep(.ant-input-search .ant-input-group-addon) {
  background: transparent;
}

:deep(.ant-input-search .ant-input-search-button) {
  height: 60px;
  border-radius: 0 30px 30px 0 !important;
  padding: 0 32px;
  font-weight: 600;
  font-size: 16px;
  margin-left: -50px;
  z-index: 10;
}

.quick-prompts {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.quick-prompts .ant-tag {
  cursor: pointer;
  border-radius: 12px;
  padding: 4px 12px;
  background: var(--secondary-bg);
  border: none;
  transition: var(--transition-base);
  font-weight: 500;
}

.quick-prompts .ant-tag:hover {
  background: var(--primary-color);
  color: white;
}

.content-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 80px;
}

.app-section {
  margin-bottom: 80px;
}

.section-header {
  margin-bottom: 32px;
  text-align: left;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 8px;
}

.section-desc {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0;
}

:deep(.ant-pagination) {
  margin-top: 40px;
  text-align: center;
}
</style>

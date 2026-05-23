<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <!-- 左侧：Logo和标题 -->
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="header-left">
            <img class="logo" src="@/assets/touxiang.jpg" alt="Logo" />
            <h1 class="site-title">AI应用生成</h1>
          </div>
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：用户操作区域 -->
      <a-col>
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { LogoutOutlined, HomeOutlined } from '@ant-design/icons-vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()
// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to, from, next) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },

]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 0 40px;
  height: 72px;
  line-height: 72px;
  position: sticky;
  top: 0;
  z-index: 1000;
  border-bottom: 1px solid var(--border-color);
  transition: var(--transition-base);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 72px;
}

.logo {
  height: 32px;
  width: 32px;
  transition: transform 0.3s ease;
}

.header-left:hover .logo {
  transform: rotate(15deg);
}

.site-title {
  margin: 0;
  font-size: 20px;
  color: var(--text-main);
  font-weight: 700;
  letter-spacing: -0.5px;
}

.ant-menu-horizontal {
  border-bottom: none !important;
  background: transparent;
  line-height: 72px;
}

:deep(.ant-menu-item) {
  font-weight: 500;
  color: var(--text-secondary) !important;
  padding: 0 20px !important;
  transition: var(--transition-base) !important;
}

:deep(.ant-menu-item-selected) {
  color: var(--primary-color) !important;
  background: transparent !important;
}

:deep(.ant-menu-item-selected::after) {
  border-bottom-width: 3px !important;
  border-bottom-color: var(--primary-color) !important;
  border-radius: 2px;
}

:deep(.ant-menu-item:hover) {
  color: var(--primary-color) !important;
}

.user-login-status {
  display: flex;
  align-items: center;
  height: 72px;
}

.user-name {
  font-weight: 600;
  color: var(--text-main);
}

.login-btn {
  border-radius: 20px;
  padding: 0 24px;
  height: 38px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(0, 102, 255, 0.2) !important;
}
</style>

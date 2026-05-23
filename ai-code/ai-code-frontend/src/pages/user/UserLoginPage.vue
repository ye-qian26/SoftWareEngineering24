<template>
  <div class="user-auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <img class="logo" src="@/assets/touxiang.jpg" alt="Logo" />
        <h2 class="title">欢迎回来</h2>
        <p class="desc">不写一行代码，生成完整应用</p>
      </div>

      <a-form :model="formState" name="basic" layout="vertical" @finish="handleSubmit">
        <a-form-item
          label="账号"
          name="userAccount"
          :rules="[{ required: true, message: '请输入账号' }]"
        >
          <a-input v-model:value="formState.userAccount" placeholder="您的账号" size="large" />
        </a-form-item>

        <a-form-item
          label="密码"
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码长度不能小于 8 位' },
          ]"
        >
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="您的密码"
            size="large"
          />
        </a-form-item>

        <div class="auth-footer">
          <RouterLink to="/user/register">还没有账号？立即注册</RouterLink>
        </div>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block class="submit-btn">
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { reactive } from 'vue'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const res = await userLogin(values)
  // 登录成功，把登录态保存到全局状态中
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,
    })
  } else {
    message.error('登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.user-auth-page {
  min-height: calc(100vh - 72px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 50% 50%, #e6f0ff 0%, #ffffff 100%);
  padding: 20px;
}

.auth-card {
  width: 100%;
  max-width: 440px;
  padding: 48px;
  background: white;
  border-radius: 32px;
  box-shadow: var(--hover-shadow);
  border: 1px solid var(--border-color);
}

.auth-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  height: 64px;
  width: 64px;
  margin-bottom: 24px;
}

.title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.desc {
  color: var(--text-secondary);
  font-size: 16px;
  margin: 0;
}

:deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: var(--text-main);
}

.auth-footer {
  text-align: right;
  margin-bottom: 24px;
  font-size: 14px;
}

.auth-footer a {
  color: var(--primary-color);
  font-weight: 500;
}

.submit-btn {
  height: 50px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 102, 255, 0.2) !important;
}
</style>

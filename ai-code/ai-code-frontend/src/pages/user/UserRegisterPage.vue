<template>
  <div class="user-auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <img class="logo" src="@/assets/touxiang.jpg" alt="Logo" />
        <h2 class="title">加入我们</h2>
        <p class="desc">开启您的 AI 创作之旅</p>
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
            { min: 8, message: '密码不能小于 8 位' },
          ]"
        >
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="您的密码"
            size="large"
          />
        </a-form-item>

        <a-form-item
          label="确认密码"
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            { min: 8, message: '密码不能小于 8 位' },
            { validator: validateCheckPassword },
          ]"
        >
          <a-input-password
            v-model:value="formState.checkPassword"
            placeholder="请再次输入密码"
            size="large"
          />
        </a-form-item>

        <div class="auth-footer">
          <RouterLink to="/user/login">已有账号？立即登录</RouterLink>
        </div>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block class="submit-btn">
            注册
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { reactive } from 'vue'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

/**
 * 验证确认密码
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
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

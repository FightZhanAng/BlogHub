<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 品牌区域 -->
      <div class="login-brand">
        <div class="brand-mark">B</div>
        <h1 class="brand-name">BlogHub</h1>
        <div class="brand-divider"></div>
        <p class="brand-tagline">{{ isLogin ? '登录你的账号' : '创建新账号' }}</p>
      </div>

      <!-- 表单区域 -->
      <div class="auth-form-wrapper">
        <div class="auth-tabs">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'login' }"
            @click="handleTabChange('login')"
          >登录</button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'register' }"
            @click="handleTabChange('register')"
          >注册</button>
        </div>

        <!-- 登录表单 -->
        <el-form
          v-show="activeTab === 'login'"
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          class="auth-form"
          @keyup.enter="submitLogin"
        >
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="submitLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 注册表单 -->
        <el-form
          v-show="activeTab === 'register'"
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="auth-form"
          @keyup.enter="submitRegister"
        >
          <el-form-item prop="username">
            <el-input v-model="registerForm.username" placeholder="用户名" :prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item prop="nickname">
            <el-input v-model="registerForm.nickname" placeholder="昵称（可选）" :prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="registerForm.password" type="password" placeholder="密码（至少 6 位）" :prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="submitRegister">
              注 册
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 底部提示 -->
      <div class="login-footer">
        <span>默认管理员：admin / admin123</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/authApi'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const activeTab = ref('login')
const isLogin = ref(true)
const loading = ref(false)

const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

function handleTabChange(name) {
  activeTab.value = name
  isLogin.value = name === 'login'
  // 清除另一个表单的校验状态，避免残留错误提示
  nextTick(() => {
    if (name === 'login') registerFormRef.value?.clearValidate()
    else loginFormRef.value?.clearValidate()
  })
}

async function submitLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login(loginForm.username, loginForm.password)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/blog')
  } catch {
    // 错误已在拦截器提示
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.register(registerForm.username, registerForm.password)
    ElMessage.success('注册成功')
    router.push(route.query.redirect || '/blog')
  } catch {
    // 错误已在拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}

.login-card {
  width: 400px;
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  padding: 48px 40px 40px;
}

/* 品牌区域 */
.login-brand {
  text-align: center;
  margin-bottom: 36px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  background: var(--color-text);
  color: var(--color-card);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  margin: 0 auto 16px;
}

.brand-name {
  font-family: var(--font-display);
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: -0.02em;
}

.brand-divider {
  width: 32px;
  height: 2px;
  background: var(--color-accent);
  margin: 0 auto 12px;
}

.brand-tagline {
  margin: 0;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

/* 自定义标签页 */
.auth-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 28px;
  border-bottom: 1px solid var(--color-border-light);
}

.tab-btn {
  flex: 1;
  padding: 10px 0;
  border: none;
  background: none;
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-tertiary);
  cursor: pointer;
  position: relative;
  transition: color var(--duration) var(--ease);
}

.tab-btn:hover {
  color: var(--color-text-secondary);
}

.tab-btn.active {
  color: var(--color-text);
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-accent);
}

/* 表单 */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.auth-btn {
  width: 100%;
  height: 44px;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.05em;
  margin-top: 8px;
}

/* 底部 */
.login-footer {
  text-align: center;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-light);
}

.login-footer span {
  font-size: 12px;
  color: var(--color-text-placeholder);
}
</style>

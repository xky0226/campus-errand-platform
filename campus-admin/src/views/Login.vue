<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="title">校园互助跑腿系统</h2>
      <p class="subtitle">管理员登录</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            size="large"
            placeholder="请输入管理员账号"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            size="large"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="login-button"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { post } from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

async function handleLogin() {
  await formRef.value.validate()

  loading.value = true
  try {
    const result = await post('/auth/admin-login', {
      username: form.username,
      password: form.password
    })

    localStorage.setItem('admin_token', result.token)
    localStorage.setItem('admin_info', JSON.stringify(result.user))

    ElMessage.success('登录成功')
    router.push('/')
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
  background: linear-gradient(135deg, #1e3a8a, #0f172a);
}

.login-card {
  width: 380px;
  padding: 40px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}

.title {
  margin: 0;
  text-align: center;
  color: #1e293b;
  font-size: 26px;
}

.subtitle {
  margin: 12px 0 32px;
  text-align: center;
  color: #64748b;
}

.login-button {
  width: 100%;
}
</style>

<template>
  <div class="container">
    <h1>牛头王 (Take 6!) 在线游戏</h1>
    <div class="auth-box">
      <div class="tabs">
        <button :class="{ active: activeTab === 'signin' }" @click="activeTab = 'signin'">登录</button>
        <button :class="{ active: activeTab === 'signup' }" @click="activeTab = 'signup'">注册</button>
      </div>

      <div v-if="activeTab === 'signin'" class="form-section">
        <h2>登录</h2>
        <input v-model="signinForm.username" type="text" placeholder="用户名" />
        <input v-model="signinForm.password" type="password" placeholder="密码" />
        <button @click="signin" :disabled="loading">登录</button>
      </div>

      <div v-if="activeTab === 'signup'" class="form-section">
        <h2>注册</h2>
        <input v-model="signupForm.username" type="text" placeholder="用户名" />
        <input v-model="signupForm.email" type="email" placeholder="邮箱" />
        <div class="code-row">
            <input v-model="signupForm.emailVerificationCode" type="text" placeholder="验证码" style="flex:1;" />
            <button @click="getVerificationCode" :disabled="codeLoading || codeTimer > 0">
                {{ codeTimer > 0 ? `${codeTimer}s` : '获取验证码' }}
            </button>
        </div>
        <input v-model="signupForm.password" type="password" placeholder="密码" />
        <button @click="signup" :disabled="loading">注册</button>
      </div>

      <div v-if="message" :class="['message', messageType]">{{ message }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import api from '../services/api';

const router = useRouter();
const activeTab = ref('signin');
const loading = ref(false);
const codeLoading = ref(false);
const message = ref('');
const messageType = ref('info');
const codeTimer = ref(0);

const signinForm = reactive({
  username: '',
  password: ''
});

const signupForm = reactive({
  username: '',
  email: '',
  password: '',
  emailVerificationCode: ''
});

const showMessage = (msg, type = 'info') => {
  message.value = msg;
  messageType.value = type;
};

const signin = async () => {
  if (!signinForm.username || !signinForm.password) {
    showMessage('请输入用户名和密码', 'error');
    return;
  }
  loading.value = true;
  try {
    const response = await api.post('/auth/signin', signinForm);
    if (response.data.code === 1000) {
      const data = response.data.data;
      localStorage.setItem('jwt_token', data.token);
      localStorage.setItem('user_info', JSON.stringify({
        id: data.id,
        username: data.username,
        nickname: data.nickname,
        email: data.email,
        vipStatus: data.vipStatus
      }));
      router.push('/lobby');
    } else {
      showMessage(response.data.message || '登录失败', 'error');
    }
  } catch (error) {
    showMessage(error.response?.data?.message || error.message || '登录失败', 'error');
  } finally {
    loading.value = false;
  }
};

const getVerificationCode = async () => {
    if (!signupForm.email) {
        showMessage('请输入邮箱', 'error');
        return;
    }
    codeLoading.value = true;
    try {
        const response = await api.post('/auth/request-verification-code', { email: signupForm.email });
        if (response.data.code === 1000) {
            showMessage('验证码已发送', 'success');
            codeTimer.value = 60;
            const interval = setInterval(() => {
                codeTimer.value--;
                if (codeTimer.value <= 0) clearInterval(interval);
            }, 1000);
        } else {
            showMessage(response.data.message || '发送失败', 'error');
        }
    } catch (error) {
        showMessage(error.response?.data?.message || error.message || '发送失败', 'error');
    } finally {
        codeLoading.value = false;
    }
};

const signup = async () => {
    if (!signupForm.username || !signupForm.password || !signupForm.email || !signupForm.emailVerificationCode) {
        showMessage('请填写所有字段', 'error');
        return;
    }
    loading.value = true;
    try {
        const response = await api.post('/auth/signup', signupForm);
        if (response.data.code === 1000 || response.status === 201) {
            showMessage('注册成功，请登录', 'success');
            activeTab.value = 'signin';
            signinForm.username = signupForm.username;
            signinForm.password = signupForm.password;
        } else {
             showMessage(response.data.message || '注册失败', 'error');
        }
    } catch (error) {
        showMessage(error.response?.data?.message || error.message || '注册失败', 'error');
    } finally {
        loading.value = false;
    }
};
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 50px;
}
.auth-box {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
  width: 400px;
  max-width: 90%;
}
.tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
}
.tabs button {
  flex: 1;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  color: #666;
  border-radius: 0;
  padding: 10px;
}
.tabs button.active {
  border-bottom-color: #3498db;
  color: #3498db;
  font-weight: bold;
}
.form-section {
  display: flex;
  flex-direction: column;
}
input {
  margin-bottom: 15px;
}
.code-row {
    display: flex;
    gap: 10px;
}
.message {
  margin-top: 15px;
  padding: 10px;
  border-radius: 4px;
}
.message.info { background: #e3f2fd; color: #1565c0; }
.message.success { background: #e8f5e9; color: #2e7d32; }
.message.error { background: #ffebee; color: #c62828; }
</style>

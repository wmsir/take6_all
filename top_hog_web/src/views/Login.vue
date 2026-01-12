<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!-- 主内容 -->
    <div class="login-content">
      <!-- 品牌标题 -->
      <div class="brand-header">
        <h1>🎮 牛头王在线</h1>
        <p class="subtitle">Take 6! - 经典策略卡牌游戏</p>
      </div>

      <!-- 认证卡片 -->
      <div class="auth-card">
        <!-- 标签切换 -->
        <div class="tabs">
          <button 
            :class="{ active: activeTab === 'signin' }" 
            @click="switchTab('signin')"
          >
            <span class="tab-icon">🔑</span>
            <span>登录</span>
          </button>
          <button 
            :class="{ active: activeTab === 'signup' }" 
            @click="switchTab('signup')"
          >
            <span class="tab-icon">✨</span>
            <span>注册</span>
          </button>
          <div class="tab-indicator" :class="{ 'tab-signup': activeTab === 'signup' }"></div>
        </div>

        <!-- 登录表单 -->
        <transition name="slide-fade" mode="out-in">
          <div v-if="activeTab === 'signin'" key="signin" class="form-section">
            <div class="form-header">
              <h2>欢迎回来</h2>
              <p>登录您的账户继续游戏</p>
            </div>

            <div class="form-body">
              <div class="input-group">
                <label>
                  <span class="label-icon">👤</span>
                  <span>用户名</span>
                </label>
                <input 
                  v-model="signinForm.username" 
                  type="text" 
                  placeholder="请输入用户名"
                  @keyup.enter="signin"
                />
              </div>

              <div class="input-group">
                <label>
                  <span class="label-icon">🔒</span>
                  <span>密码</span>
                </label>
                <input 
                  v-model="signinForm.password" 
                  type="password" 
                  placeholder="请输入密码"
                  @keyup.enter="signin"
                />
              </div>

              <button 
                @click="signin" 
                :disabled="loading"
                class="btn-primary btn-block"
              >
                <span v-if="!loading">🚀 立即登录</span>
                <span v-else class="loading-text">
                  <span class="spinner"></span>
                  登录中...
                </span>
              </button>
            </div>
          </div>

          <!-- 注册表单 -->
          <div v-else key="signup" class="form-section">
            <div class="form-header">
              <h2>创建账户</h2>
              <p>加入我们,开始您的游戏之旅</p>
            </div>

            <div class="form-body">
              <div class="input-group">
                <label>
                  <span class="label-icon">👤</span>
                  <span>用户名</span>
                </label>
                <input 
                  v-model="signupForm.username" 
                  type="text" 
                  placeholder="请输入用户名"
                  maxlength="20"
                />
              </div>

              <div class="input-group">
                <label>
                  <span class="label-icon">📧</span>
                  <span>邮箱</span>
                </label>
                <input 
                  v-model="signupForm.email" 
                  type="email" 
                  placeholder="请输入邮箱地址"
                />
              </div>

              <div class="input-group">
                <label>
                  <span class="label-icon">🔢</span>
                  <span>验证码</span>
                </label>
                <div class="code-row">
                  <input 
                    v-model="signupForm.emailVerificationCode" 
                    type="text" 
                    placeholder="请输入验证码"
                    maxlength="6"
                  />
                  <button 
                    @click="getVerificationCode" 
                    :disabled="codeLoading || codeTimer > 0"
                    class="btn-code"
                  >
                    <span v-if="codeTimer > 0">{{ codeTimer }}s</span>
                    <span v-else-if="codeLoading">发送中...</span>
                    <span v-else>获取验证码</span>
                  </button>
                </div>
              </div>

              <div class="input-group">
                <label>
                  <span class="label-icon">🔒</span>
                  <span>密码</span>
                </label>
                <input 
                  v-model="signupForm.password" 
                  type="password" 
                  placeholder="请输入密码(至少6位)"
                  @keyup.enter="signup"
                />
              </div>

              <button 
                @click="signup" 
                :disabled="loading"
                class="btn-primary btn-block"
              >
                <span v-if="!loading">✨ 立即注册</span>
                <span v-else class="loading-text">
                  <span class="spinner"></span>
                  注册中...
                </span>
              </button>
            </div>
          </div>
        </transition>

        <!-- 消息提示 -->
        <transition name="message-fade">
          <div v-if="message" :class="['message', messageType]">
            <span class="message-icon">
              {{ messageType === 'success' ? '✅' : messageType === 'error' ? '❌' : 'ℹ️' }}
            </span>
            <span>{{ message }}</span>
          </div>
        </transition>
      </div>

      <!-- 页脚信息 -->
      <div class="footer-info">
        <p>© 2026 完美在线 - 享受策略的乐趣</p>
      </div>
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
  setTimeout(() => {
    message.value = '';
  }, 5000);
};

const switchTab = (tab) => {
  activeTab.value = tab;
  message.value = '';
};

const signin = async () => {
  if (!signinForm.username || !signinForm.password) {
    showMessage('请输入用户名和密码', 'error');
    return;
  }
  loading.value = true;
  try {
    const response = await api.post('/auth/signin', signinForm);
    if (response.data.code === 200) {
      const data = response.data.data;
      localStorage.setItem('jwt_token', data.accessToken);
      localStorage.setItem('user_info', JSON.stringify({
        id: data.id,
        username: data.username,
        nickname: data.nickname,
        email: data.email,
        vipStatus: data.vipStatus
      }));
      showMessage('登录成功!', 'success');
      setTimeout(() => {
        router.push('/lobby');
      }, 500);
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
  // 简单的邮箱格式验证
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(signupForm.email)) {
    showMessage('请输入有效的邮箱地址', 'error');
    return;
  }
  codeLoading.value = true;
  try {
    const response = await api.post('/auth/request-verification-code', { email: signupForm.email });
    if (response.data.code === 200) {
      showMessage('验证码已发送到您的邮箱', 'success');
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
  if (signupForm.password.length < 6) {
    showMessage('密码至少需要6位', 'error');
    return;
  }
  loading.value = true;
  try {
    const response = await api.post('/auth/signup', signupForm);
    if (response.data.code === 200 || response.status === 201) {
      showMessage('注册成功,即将跳转到登录页面', 'success');
      setTimeout(() => {
        activeTab.value = 'signin';
        signinForm.username = signupForm.username;
        signinForm.password = signupForm.password;
        message.value = '';
      }, 1500);
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
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: var(--primary-gradient);
  opacity: 0.1;
  animation: float 20s infinite ease-in-out;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.circle-2 {
  width: 400px;
  height: 400px;
  bottom: -150px;
  right: -150px;
  animation-delay: 5s;
}

.circle-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 10%;
  animation-delay: 10s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.1);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.9);
  }
}

/* 主内容 */
.login-content {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 480px;
  animation: fadeIn 0.8s ease;
}

/* 品牌标题 */
.brand-header {
  text-align: center;
  margin-bottom: 2rem;
  animation: slideIn 0.6s ease;
}

.brand-header h1 {
  font-size: 2.5em;
  margin: 0 0 0.5rem 0;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  color: var(--text-secondary);
  font-size: 1.1em;
  margin: 0;
}

/* 认证卡片 */
.auth-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  padding: 2rem;
  box-shadow: var(--shadow-lg);
  animation: fadeIn 0.8s ease 0.2s backwards;
}

/* 标签切换 */
.tabs {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  position: relative;
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-lg);
  padding: 0.5rem;
}

.tabs button {
  flex: 1;
  background: none;
  border: none;
  padding: 0.75rem 1rem;
  color: var(--text-secondary);
  font-size: 1em;
  font-weight: 600;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.tabs button.active {
  color: var(--text-primary);
}

.tab-icon {
  font-size: 1.2em;
}

.tab-indicator {
  position: absolute;
  left: 0.5rem;
  top: 0.5rem;
  width: calc(50% - 0.5rem);
  height: calc(100% - 1rem);
  background: var(--primary-gradient);
  border-radius: var(--radius-md);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
  box-shadow: var(--shadow-md);
}

.tab-indicator.tab-signup {
  transform: translateX(calc(100% + 0.5rem));
}

/* 表单区域 */
.form-section {
  animation: fadeIn 0.4s ease;
}

.form-header {
  text-align: center;
  margin-bottom: 2rem;
}

.form-header h2 {
  font-size: 1.8em;
  margin: 0 0 0.5rem 0;
  color: var(--text-primary);
}

.form-header p {
  color: var(--text-secondary);
  margin: 0;
  font-size: 0.95em;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* 输入组 */
.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.input-group label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9em;
  font-weight: 600;
  color: var(--text-secondary);
}

.label-icon {
  font-size: 1.1em;
}

.input-group input {
  width: 100%;
  padding: 0.875rem 1rem;
  background: rgba(255, 255, 255, 0.05);
  border: 2px solid var(--glass-border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 1em;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.input-group input:focus {
  background: rgba(255, 255, 255, 0.1);
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}

.input-group input::placeholder {
  color: var(--text-muted);
}

/* 验证码行 */
.code-row {
  display: flex;
  gap: 0.75rem;
}

.code-row input {
  flex: 1;
}

.btn-code {
  padding: 0.875rem 1.25rem;
  background: var(--success-gradient);
  white-space: nowrap;
  min-width: 120px;
}

.btn-code:disabled {
  background: rgba(255, 255, 255, 0.1);
}

/* 按钮 */
.btn-block {
  width: 100%;
  margin-top: 0.5rem;
}

.loading-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 消息提示 */
.message {
  margin-top: 1.5rem;
  padding: 1rem;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.95em;
  animation: messageSlide 0.3s ease;
}

.message-icon {
  font-size: 1.2em;
}

.message.info {
  background: rgba(79, 172, 254, 0.2);
  border: 1px solid rgba(79, 172, 254, 0.3);
  color: #4facfe;
}

.message.success {
  background: rgba(0, 242, 254, 0.2);
  border: 1px solid rgba(0, 242, 254, 0.3);
  color: #00f2fe;
}

.message.error {
  background: rgba(245, 87, 108, 0.2);
  border: 1px solid rgba(245, 87, 108, 0.3);
  color: #f5576c;
}

@keyframes messageSlide {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 页脚 */
.footer-info {
  text-align: center;
  margin-top: 2rem;
  color: var(--text-muted);
  font-size: 0.9em;
  animation: fadeIn 1s ease 0.4s backwards;
}

.footer-info p {
  margin: 0;
}

/* 过渡动画 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.message-fade-enter-active,
.message-fade-leave-active {
  transition: all 0.3s ease;
}

.message-fade-enter-from,
.message-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-container {
    padding: 1rem;
  }

  .brand-header h1 {
    font-size: 2em;
  }

  .auth-card {
    padding: 1.5rem;
  }

  .form-header h2 {
    font-size: 1.5em;
  }

  .code-row {
    flex-direction: column;
  }

  .btn-code {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .brand-header h1 {
    font-size: 1.8em;
  }

  .subtitle {
    font-size: 1em;
  }

  .auth-card {
    padding: 1.25rem;
  }

  .tabs {
    gap: 0.5rem;
    padding: 0.4rem;
  }

  .tabs button {
    padding: 0.6rem 0.75rem;
    font-size: 0.9em;
  }

  .tab-icon {
    font-size: 1em;
  }
}
</style>

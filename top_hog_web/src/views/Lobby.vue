<template>
  <div class="lobby-container">
    <!-- 顶部导航栏 -->
    <header class="lobby-header">
      <div class="header-content">
        <div class="logo-section">
          <h1>🎮 游戏大厅</h1>
        </div>
        <div class="user-section">
          <div class="user-info-card">
            <div class="user-avatar">{{ getUserInitial() }}</div>
            <div class="user-details">
              <span class="user-name">{{ userInfo.nickname || userInfo.username }}</span>
              <span class="user-id">ID: {{ userInfo.id }}</span>
            </div>
          </div>
          <button @click="logout" class="logout-btn">
            <span>退出登录</span>
          </button>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="lobby-main">
      <div class="lobby-content">
        <!-- 左侧区域 -->
        <aside class="sidebar">
          <!-- 快速操作卡片 -->
          <div class="glass-card quick-actions">
            <h2>⚡ 快速开始</h2>
            <div class="action-form">
              <div class="form-group">
                <label>房间名称</label>
                <input 
                  v-model="newRoomName" 
                  placeholder="输入房间名称" 
                  maxlength="20"
                />
              </div>
              <div class="form-group">
                <label>游戏类型</label>
                <select v-model="selectedGameType" class="game-select">
                  <option v-for="game in games" :key="game.code" :value="game.code">
                    {{ game.displayName }}
                  </option>
                </select>
              </div>
              <button 
                @click="createRoom" 
                :disabled="loading || !newRoomName.trim()" 
                class="btn-primary btn-block"
              >
                <span v-if="!loading">🎯 创建房间</span>
                <span v-else>创建中...</span>
              </button>
              <button 
                @click="createPveRoom" 
                :disabled="loading" 
                class="btn-secondary btn-block"
              >
                <span v-if="!loading">🤖 人机对战</span>
                <span v-else>创建中...</span>
              </button>
            </div>
          </div>

          <!-- 统计面板 -->
          <div class="glass-card stats-panel">
            <h2>📊 实时统计</h2>
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-icon">👥</div>
                <div class="stat-info">
                  <div class="stat-value">{{ stats.onlineUsers }}</div>
                  <div class="stat-label">在线玩家</div>
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-icon">🏠</div>
                <div class="stat-info">
                  <div class="stat-value">{{ rooms.length }}</div>
                  <div class="stat-label">活跃房间</div>
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-icon">🎲</div>
                <div class="stat-info">
                  <div class="stat-value">{{ stats.todayGames }}</div>
                  <div class="stat-label">今日对局</div>
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-icon">🏆</div>
                <div class="stat-info">
                  <div class="stat-value">{{ stats.winRate }}%</div>
                  <div class="stat-label">胜率</div>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <!-- 右侧房间列表 -->
        <section class="room-section">
          <div class="section-header">
            <h2>🎪 房间列表</h2>
            <button @click="fetchRooms" class="btn-refresh" :disabled="loadingRooms">
              <span v-if="!loadingRooms">🔄 刷新</span>
              <span v-else>刷新中...</span>
            </button>
          </div>

          <!-- 加载状态 -->
          <div v-if="loadingRooms" class="loading-state">
            <div class="loading-spinner"></div>
            <p>加载房间列表中...</p>
          </div>

          <!-- 房间网格 -->
          <div v-else-if="rooms.length > 0" class="room-grid">
            <div 
              v-for="room in rooms" 
              :key="room.roomId" 
              class="room-card"
              :class="{ 'room-full': isRoomFull(room), 'room-playing': room.gameState === 'PLAYING' }"
            >
              <div class="room-header">
                <h3 class="room-name">{{ room.roomName }}</h3>
                <span class="room-status" :class="getStatusClass(room.gameState)">
                  {{ translateStatus(room.gameState) }}
                </span>
              </div>
              
              <div class="room-info">
                <div class="info-row">
                  <span class="info-label">房间ID</span>
                  <span class="info-value">{{ room.roomId }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">游戏类型</span>
                  <span class="info-value">{{ getGameTypeName(room.gameType) }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">玩家人数</span>
                  <span class="info-value players-count">
                    <span class="current-players">{{ Object.keys(room.players || {}).length }}</span>
                    <span class="separator">/</span>
                    <span class="max-players">{{ room.maxPlayers }}</span>
                  </span>
                </div>
              </div>

              <button 
                @click="joinRoom(room.roomId)" 
                :disabled="isRoomFull(room)"
                class="btn-join"
                :class="{ 'btn-disabled': isRoomFull(room) }"
              >
                <span v-if="!isRoomFull(room)">🚀 加入房间</span>
                <span v-else>房间已满</span>
              </button>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-state">
            <div class="empty-icon">🎮</div>
            <h3>暂无活跃房间</h3>
            <p>快来创建第一个房间,开始游戏吧!</p>
            <button @click="focusRoomInput" class="btn-primary">
              立即创建
            </button>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import api from '../services/api';

const router = useRouter();
const userInfo = reactive(JSON.parse(localStorage.getItem('user_info') || '{}'));
const rooms = ref([]);
const loading = ref(false);
const loadingRooms = ref(false);
const newRoomName = ref('');
const games = ref([]);
const selectedGameType = ref('top_hog');

// 模拟统计数据
const stats = reactive({
  onlineUsers: 128,
  todayGames: 456,
  winRate: 68
});

// 获取用户名首字母
const getUserInitial = () => {
  const name = userInfo.nickname || userInfo.username || 'U';
  return name.charAt(0).toUpperCase();
};

// 状态翻译
const translateStatus = (status) => {
  const map = {
    'WAITING': '等待中',
    'PLAYING': '游戏中',
    'GAME_OVER': '已结束',
    'WAITING_FOR_PLAYER_CHOICE': '等待选择'
  };
  return map[status] || status;
};

// 获取状态样式类
const getStatusClass = (status) => {
  const classMap = {
    'WAITING': 'status-waiting',
    'PLAYING': 'status-playing',
    'GAME_OVER': 'status-finished',
    'WAITING_FOR_PLAYER_CHOICE': 'status-waiting'
  };
  return classMap[status] || '';
};

// 获取游戏类型名称
const getGameTypeName = (gameType) => {
  const game = games.value.find(g => g.code === gameType);
  return game ? game.displayName : gameType;
};

// 判断房间是否已满
const isRoomFull = (room) => {
  const count = Object.keys(room.players || {}).length;
  return count >= (room.maxPlayers || 10);
};

// 退出登录
const logout = () => {
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('user_info');
  router.push('/login');
};

// 获取房间列表
const fetchRooms = async () => {
  loadingRooms.value = true;
  try {
    const response = await api.post('/room/list', { page: 1, pageSize: 50 });
    if (response.data.code === 200) {
      rooms.value = response.data.data.list;
    }
  } catch (error) {
    console.error("Fetch rooms failed", error);
  } finally {
    loadingRooms.value = false;
  }
};

// 获取游戏列表
const fetchGames = async () => {
  try {
    const response = await api.get('/game-config/enabled');
    if (response.data.code === 200) {
      games.value = response.data.data;
      if (games.value.length > 0) {
        selectedGameType.value = games.value[0].code;
      }
    }
  } catch (error) {
    console.error("Fetch games failed", error);
  }
};

// 创建房间
const createRoom = async () => {
  if (!newRoomName.value.trim()) return;
  loading.value = true;
  try {
    const response = await api.post('/room/create', { 
      roomName: newRoomName.value,
      gameType: selectedGameType.value 
    });
    if (response.data.code === 200) {
      const room = response.data.data;
      joinRoom(room.roomId);
    } else {
      alert(response.data.message);
    }
  } catch (error) {
    alert(error.message);
  } finally {
    loading.value = false;
  }
};

// 创建PvE房间
const createPveRoom = async () => {
  loading.value = true;
  try {
    const response = await api.post('/room/create-pve', { botCount: 5 });
    if (response.data.code === 200) {
      const room = response.data.data;
      joinRoom(room.roomId);
    } else {
      alert(response.data.message);
    }
  } catch (error) {
    alert(error.message);
  } finally {
    loading.value = false;
  }
};

// 加入房间
const joinRoom = async (roomId) => {
  try {
    const response = await api.post('/room/join', { roomId });
    if (response.data.code === 200) {
      router.push(`/game/${roomId}`);
    } else {
      alert(response.data.message);
    }
  } catch (error) {
    alert(error.response?.data?.message || '加入房间失败');
  }
};

// 聚焦到房间名输入框
const focusRoomInput = () => {
  const input = document.querySelector('input[placeholder="输入房间名称"]');
  if (input) {
    input.focus();
    input.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }
};

onMounted(() => {
  fetchRooms();
  fetchGames();
  
  // 定时刷新房间列表
  const intervalId = setInterval(fetchRooms, 10000);
  
  // 组件卸载时清除定时器
  return () => clearInterval(intervalId);
});
</script>

<style scoped>
.lobby-container {
  min-height: 100vh;
  padding-bottom: 2rem;
}

/* 顶部导航栏 */
.lobby-header {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--glass-border);
  padding: 1.5rem 0;
  position: sticky;
  top: 0;
  z-index: 100;
  animation: slideIn 0.5s ease;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;
}

.logo-section h1 {
  font-size: 1.8em;
  margin: 0;
}

.user-section {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.user-info-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.5rem 1rem;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.2em;
  color: white;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.user-name {
  font-weight: 600;
  color: var(--text-primary);
}

.user-id {
  font-size: 0.85em;
  color: var(--text-muted);
}

.logout-btn {
  background: var(--secondary-gradient);
  padding: 0.6em 1.2em;
  font-size: 0.95em;
}

/* 主内容区 */
.lobby-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

.lobby-content {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 2rem;
  align-items: start;
}

/* 玻璃态卡片 */
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  box-shadow: var(--shadow-lg);
  animation: fadeIn 0.6s ease;
}

.glass-card h2 {
  font-size: 1.3em;
  margin-bottom: 1.5rem;
}

/* 侧边栏 */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  position: sticky;
  top: 120px;
}

/* 快速操作 */
.quick-actions {
  animation-delay: 0.1s;
}

.action-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-size: 0.9em;
  font-weight: 600;
  color: var(--text-secondary);
}

.btn-block {
  width: 100%;
}

.btn-primary {
  background: var(--primary-gradient);
}

.btn-secondary {
  background: var(--warning-gradient);
}

.btn-refresh {
  background: var(--success-gradient);
  padding: 0.6em 1.2em;
  font-size: 0.95em;
}

/* 统计面板 */
.stats-panel {
  animation-delay: 0.2s;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-md);
  transition: all 0.3s ease;
}

.stat-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.stat-icon {
  font-size: 2em;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.stat-value {
  font-size: 1.5em;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.stat-label {
  font-size: 0.8em;
  color: var(--text-muted);
}

/* 房间区域 */
.room-section {
  animation: fadeIn 0.6s ease 0.3s backwards;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-header h2 {
  margin: 0;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1rem;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid rgba(255, 255, 255, 0.1);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 房间网格 */
.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

/* 房间卡片 */
.room-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeIn 0.5s ease;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.room-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg), var(--shadow-glow);
  border-color: rgba(102, 126, 234, 0.5);
}

.room-card.room-full {
  opacity: 0.6;
}

.room-card.room-playing {
  border-color: rgba(245, 87, 108, 0.5);
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.room-name {
  font-size: 1.2em;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  flex: 1;
  word-break: break-word;
}

.room-status {
  padding: 0.3em 0.8em;
  border-radius: var(--radius-sm);
  font-size: 0.85em;
  font-weight: 600;
  white-space: nowrap;
}

.status-waiting {
  background: rgba(79, 172, 254, 0.2);
  color: #4facfe;
}

.status-playing {
  background: rgba(245, 87, 108, 0.2);
  color: #f5576c;
}

.status-finished {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-muted);
}

.room-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-md);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 0.9em;
  color: var(--text-muted);
}

.info-value {
  font-weight: 600;
  color: var(--text-primary);
}

.players-count {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.current-players {
  color: #4facfe;
  font-weight: 700;
}

.separator {
  color: var(--text-muted);
}

.max-players {
  color: var(--text-secondary);
}

.btn-join {
  width: 100%;
  background: var(--primary-gradient);
  margin-top: auto;
}

.btn-join.btn-disabled {
  background: rgba(255, 255, 255, 0.1);
  cursor: not-allowed;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 2px dashed var(--glass-border);
  border-radius: var(--radius-lg);
  gap: 1rem;
}

.empty-icon {
  font-size: 4em;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 1.5em;
  margin: 0;
}

.empty-state p {
  color: var(--text-secondary);
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .lobby-content {
    grid-template-columns: 1fr;
  }
  
  .sidebar {
    position: static;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 1.5rem;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 1rem;
  }
  
  .user-section {
    width: 100%;
    justify-content: space-between;
  }
  
  .lobby-main {
    padding: 1rem;
  }
  
  .sidebar {
    grid-template-columns: 1fr;
  }
  
  .room-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .lobby-header {
    padding: 1rem 0;
  }
  
  .header-content {
    padding: 0 1rem;
  }
  
  .logo-section h1 {
    font-size: 1.5em;
  }
  
  .user-info-card {
    padding: 0.4rem 0.8rem;
  }
  
  .user-avatar {
    width: 35px;
    height: 35px;
    font-size: 1em;
  }
}
</style>

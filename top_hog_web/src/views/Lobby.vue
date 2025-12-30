<template>
  <div class="container">
    <div class="header">
        <h1>游戏大厅</h1>
        <div class="user-info">
            <span>欢迎, {{ userInfo.nickname || userInfo.username }} (ID: {{ userInfo.id }})</span>
            <button @click="logout" class="logout-btn">退出</button>
        </div>
    </div>

    <div class="content">
        <div class="create-room-section section">
            <h2>创建房间</h2>
            <div class="input-group">
                <input v-model="newRoomName" placeholder="房间名称" />
                <button @click="createRoom" :disabled="loading">创建</button>
            </div>
            <div class="input-group">
                 <button @click="createPveRoom" :disabled="loading" style="background-color: #f39c12;">创建人机对战 (PvE)</button>
            </div>
        </div>

        <div class="room-list-section section">
            <div class="list-header">
                <h2>房间列表</h2>
                <button @click="fetchRooms" class="refresh-btn">刷新</button>
            </div>

            <div v-if="loadingRooms" class="loading">加载中...</div>
            <ul v-else-if="rooms.length > 0" class="room-list">
                <li v-for="room in rooms" :key="room.roomId" class="room-item">
                    <div class="room-info">
                        <strong>{{ room.roomName }}</strong>
                        <span class="room-id">ID: {{ room.roomId }}</span>
                        <span class="room-status">{{ translateStatus(room.gameState) }}</span>
                    </div>
                    <div class="room-actions">
                        <span>{{ Object.keys(room.players || {}).length }}/{{ room.maxPlayers }} 人</span>
                        <button @click="joinRoom(room.roomId)" :disabled="isRoomFull(room)">加入</button>
                    </div>
                </li>
            </ul>
            <div v-else class="empty-list">暂无房间，快去创建一个吧！</div>
        </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import api from '../services/api';

const router = useRouter();
const userInfo = reactive(JSON.parse(localStorage.getItem('user_info') || '{}'));
const rooms = ref([]);
const loading = ref(false);
const loadingRooms = ref(false);
const newRoomName = ref('');

const translateStatus = (status) => {
    const map = {
        'WAITING': '等待中',
        'PLAYING': '游戏中',
        'GAME_OVER': '已结束',
        'WAITING_FOR_PLAYER_CHOICE': '等待选择'
    };
    return map[status] || status;
};

const isRoomFull = (room) => {
    const count = Object.keys(room.players || {}).length;
    return count >= (room.maxPlayers || 10);
};

const logout = () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_info');
    router.push('/login');
};

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

const createRoom = async () => {
    if (!newRoomName.value.trim()) return;
    loading.value = true;
    try {
        const response = await api.post('/room/create', { roomName: newRoomName.value });
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
}

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

onMounted(() => {
    fetchRooms();
});
</script>

<style scoped>
.container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
    width: 100%;
    box-sizing: border-box;
}
.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
}
.user-info {
    display: flex;
    gap: 15px;
    align-items: center;
}
.logout-btn {
    background-color: #e74c3c;
    padding: 5px 10px;
}
.logout-btn:hover {
    background-color: #c0392b;
}
.section {
    background: white;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.05);
    margin-bottom: 20px;
}
.input-group {
    display: flex;
    gap: 10px;
}
.input-group input {
    flex: 1;
    margin-bottom: 0;
}
.list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
}
.refresh-btn {
    background-color: #2ecc71;
}
.room-list {
    list-style: none;
    padding: 0;
}
.room-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px;
    border-bottom: 1px solid #eee;
    transition: background 0.2s;
}
.room-item:hover {
    background-color: #f9f9f9;
}
.room-info {
    display: flex;
    flex-direction: column;
}
.room-id {
    font-size: 0.8em;
    color: #999;
}
.room-status {
    font-size: 0.9em;
    color: #3498db;
}
.room-actions {
    display: flex;
    align-items: center;
    gap: 15px;
}
.empty-list {
    text-align: center;
    color: #999;
    padding: 30px;
}
</style>

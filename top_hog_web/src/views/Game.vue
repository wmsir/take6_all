<template>
  <div class="game-container">
    <div class="game-header">
        <h1>牛头王 (Take 6!) - {{ roomInfo.roomName }}</h1>
        <div class="header-controls">
             <button class="icon-button" @click="toggleMusic" :title="isMusicPlaying ? '关闭音乐' : '开启音乐'">
                {{ isMusicPlaying ? '🔊' : '🔇' }}
             </button>
             <div class="player-count-badge">
                👥 {{ playerCount }}/{{ maxPlayers }}
             </div>
        </div>
    </div>

    <!-- Audio (Hidden) -->
    <audio ref="bgmAudio" loop src="/background.mp3"></audio>
    <audio ref="sfxAudio" src="/placeholder.mp3"></audio>

    <!-- Game Over Results Overlay -->
    <Transition name="fade">
        <div v-if="showVictoryOverlay" class="victory-overlay" @click.self="closeVictoryOverlay">
            <div class="results-card">
                <!-- Header -->
                <div class="results-header">
                    <div class="results-icon">🏆</div>
                    <h2>游戏结束</h2>
                    <p class="results-subtitle">{{ victoryMessage }}</p>
                </div>

                <!-- Rankings Table -->
                <div class="rankings-section">
                    <h3>📊 战绩排名</h3>
                    <div class="rankings-table">
                        <div 
                            v-for="(player, index) in rankedPlayers" 
                            :key="player.sessionId"
                            class="ranking-row"
                            :class="{ 
                                'rank-1': index === 0, 
                                'rank-2': index === 1, 
                                'rank-3': index === 2,
                                'is-me': isMe(player)
                            }"
                        >
                            <div class="rank-badge">
                                <span v-if="index === 0" class="medal">🥇</span>
                                <span v-else-if="index === 1" class="medal">🥈</span>
                                <span v-else-if="index === 2" class="medal">🥉</span>
                                <span v-else class="rank-number">#{{ index + 1 }}</span>
                            </div>
                            <div class="player-info">
                                <div class="player-name-row">
                                    <span class="player-name">
                                        <span v-if="player.isHost">👑</span>
                                        {{ player.displayName }}
                                    </span>
                                    <span v-if="isMe(player)" class="you-badge">你</span>
                                </div>
                                <div class="player-stats">
                                    <span v-if="player.isBot" class="bot-badge">🤖 机器人</span>
                                    <span v-if="player.isTrustee" class="trustee-badge">托管</span>
                                </div>
                            </div>
                            <div class="player-score">
                                <span class="score-value">{{ player.score || 0 }}</span>
                                <span class="score-label">分</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Actions -->
                <div class="results-actions">
                    <button class="btn btn-secondary" @click="closeVictoryOverlay">
                        关闭
                    </button>
                    <button 
                        v-if="gameState === 'GAME_OVER'" 
                        class="btn btn-success" 
                        @click="requestNewGame"
                    >
                        🎮 再来一局
                    </button>
                    <button class="btn btn-primary" @click="returnToLobby">
                        返回大厅
                    </button>
                </div>
            </div>
        </div>
    </Transition>

    <div class="game-layout">
        <!-- Main Game Area: Dynamically Loaded -->
        <component 
            :is="currentGameComponent"
            ref="gameBoardRef"
            :gameState="gameState"
            :roomState="roomState"
            :mySessionId="mySessionId"
            :userInfo="userInfo"
            :wsSend="send"
            @leaveRoom="onLeaveRoom"
            @addBot="onAddBot"
        />

        <!-- Sidebar -->
        <div class="game-sidebar">
            <div class="sidebar-panel player-panel">
                <h3>玩家列表</h3>
                <ul class="player-list-items">
                    <li v-for="player in playerList" :key="player.sessionId" class="player-item" :class="{ 'is-me': isMe(player) }">
                        <div class="player-info-row">
                            <span class="player-name">
                                <span v-if="player.isHost">👑</span>
                                {{ player.displayName }}
                            </span>
                            <span class="player-score">🐂 {{ player.score || 0 }}</span>
                        </div>
                        <div class="player-status-row">
                             <span class="status-tag" :class="getStatusClass(player)">
                                 {{ getStatusText(player) }}
                             </span>
                             <span class="card-count">🎴 {{ player.hand ? player.hand.length : '?' }}</span>
                        </div>
                    </li>
                </ul>
            </div>

            <div class="sidebar-panel chat-panel">
                <h3>聊天</h3>
                <div class="chat-window" ref="chatBox">
                    <div v-for="(msg, i) in chatMessages" :key="i" class="chat-msg">
                        <span class="chat-sender">{{ msg.senderName }}:</span>
                        <span class="chat-text">{{ msg.text }}</span>
                    </div>
                </div>
                <div class="chat-controls">
                    <select v-model="quickChat" @change="handleQuickChat" class="quick-chat-select">
                        <option value="">快捷语...</option>
                        <option v-for="txt in quickChatOptions" :key="txt" :value="txt">{{ txt }}</option>
                    </select>
                    <div class="input-group">
                        <input v-model="chatInput" @keypress.enter="sendChat" placeholder="说点什么..." />
                        <button @click="sendChat" class="btn-small">发送</button>
                    </div>
                </div>
            </div>

            <div class="sidebar-panel log-panel">
                 <h3>日志</h3>
                 <div class="log-window" ref="logBox">
                      <div v-for="(log, i) in logs" :key="i" class="log-entry">{{ log }}</div>
                 </div>
            </div>
        </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch, shallowRef } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '../services/api';
import TopHogBoard from '../components/game/TopHogBoard.vue';

const route = useRoute();
const router = useRouter();
const roomId = ref(route.params.roomId);
const userInfo = reactive(JSON.parse(localStorage.getItem('user_info') || '{}'));

// Refs
const bgmAudio = ref(null);
const chatBox = ref(null);
const logBox = ref(null);
const gameBoardRef = ref(null); // Ref to the dynamic child component

// State
const ws = ref(null);
const isConnected = ref(false);
const roomInfo = reactive({ roomName: '', roomId: '', gameType: 'top_hog' });
const gameState = ref('WAITING');
const roomState = ref({}); // Store full room state to pass to child
const players = ref({});
const mySessionId = ref(null);
const isMusicPlaying = ref(false);
const chatMessages = ref([]);
const logs = ref([]);
const quickChat = ref('');
const chatInput = ref('');
const errorMessage = ref('');
const joiningFeedback = ref('');

// Victory Overlay
const showVictoryOverlay = ref(false);
const victoryMessage = ref('');
let victoryOverlayManuallyClosed = ref(false);

// Constants
const quickChatOptions = [
    "又是我吃牛头的一天。",
    "牛头王不是我，真的！",
    "我已经囤了半副牛头牌了……",
    "这局我就是送温暖的慈善家。",
    "你们都太坏了，联手坑我是不是？",
    "牛头太多，已阵亡。",
    "我宣布：本局牛头王自动当选！",
    "我怀疑你在苟，但我没有证据。",
    "你这牌打得我措手不及！",
    "小心那行，快满了！",
    "哎呀你也在这儿埋伏？",
    "你是不是偷看了我的牌？",
    "这轮咱们都别乱出啊～",
    "你又抢我吃的顺序！",
    "赢啦！牛头都不想跟我回家～",
    "惨败……我是不是该退役了？",
    "虽然输了，但过程很欢乐！",
    "牛头王易主，江湖再见！",
    "这分差，下次还得继续努力～"
];

// Computed
const playerList = computed(() => Object.values(players.value));
const playerCount = computed(() => playerList.value.length);
const maxPlayers = computed(() => roomInfo.maxPlayers || 10);

// Ranked players sorted by score (ascending for this game - lower is better)
const rankedPlayers = computed(() => {
    return [...playerList.value].sort((a, b) => {
        const scoreA = a.score || 0;
        const scoreB = b.score || 0;
        return scoreA - scoreB; // Lower score wins in this game
    });
});

const currentGameComponent = computed(() => {
    // Should map roomInfo.gameType to component
    // Default to TopHogBoard
    if (roomInfo.gameType === 'example_game') {
        // return ExampleGameBoard;
        return TopHogBoard; // Fallback for now
    }
    return TopHogBoard;
});

// Methods
const addLog = (msg) => {
    const time = new Date().toLocaleTimeString();
    logs.value.push(`[${time}] ${msg}`);
    nextTick(() => {
        if (logBox.value) logBox.value.scrollTop = logBox.value.scrollHeight;
    });
};

const toggleMusic = () => {
    if (!bgmAudio.value) return;
    if (isMusicPlaying.value) {
        bgmAudio.value.pause();
        isMusicPlaying.value = false;
    } else {
        bgmAudio.value.play().catch(e => {
            console.warn("BGM error", e);
            alert("无法播放背景音乐");
        });
        isMusicPlaying.value = true;
    }
};

const connect = () => {
    if (!userInfo.id) {
        alert("未登录");
        router.push('/login');
        return;
    }
    const wsUrl = `ws://${window.location.hostname}:8088/ws-game?userIdentifier=${userInfo.id}`;
    addLog(`Connecting...`);

    ws.value = new WebSocket(wsUrl);

    ws.value.onopen = () => {
        addLog("Connected");
        isConnected.value = true;
        send({ type: 'joinRoom', roomId: roomId.value });
    };

    ws.value.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            handleMessage(data);
        } catch (e) {
            console.error(e);
        }
    };

    ws.value.onclose = () => {
        addLog("Disconnected");
        isConnected.value = false;
    };
};

const send = (msg) => {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
        ws.value.send(JSON.stringify(msg));
    }
};

const handleMessage = (data) => {
    if (data.type === 'connectionAcknowledged') {
        mySessionId.value = data.sessionId;
        if (data.vipStatus !== undefined) {
             userInfo.vipStatus = data.vipStatus;
             localStorage.setItem('user_info', JSON.stringify(userInfo));
        }
    } else if (data.type === 'gameStateUpdate') {
        updateGameState(data.roomState);
        if (data.message) addLog(data.message);
    } else if (data.type === 'rejoinSuccess') {
        addLog("Rejoined");
        if (data.roomState) updateGameState(data.roomState);
    } else if (data.type === 'chatMessage') {
        const senderId = data.sender;
        let name = "Unknown";
        if (players.value) {
             const p = Object.values(players.value).find(p => String(p.userId) === String(senderId));
             if (p) name = p.displayName;
             else if (String(senderId) === String(userInfo.id)) name = userInfo.nickname || userInfo.username;
        }
        chatMessages.value.push({ senderName: name, text: data.text });
        nextTick(() => {
            if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight;
        });
    } else if (data.type === 'error') {
        errorMessage.value = data.message;
        setTimeout(() => errorMessage.value = '', 5000);
    } else if (data.type === 'leftRoomSuccess') {
        router.push('/lobby');
    } else if (data.type === 'roomClosed') {
        alert(data.message || "房间已关闭");
        router.push('/lobby');
    } else {
        // Delegate other game-specific messages (like 'needSelectRow', 'playTipResponse') to child component
        if (gameBoardRef.value && gameBoardRef.value.handleGameEvent) {
            gameBoardRef.value.handleGameEvent(data);
        }
    }
};

const updateGameState = (state) => {
    console.log('=== updateGameState 被调用 ===', {
        currentState: gameState.value,
        newState: state.gameState,
        message: state.message
    });
    
    roomState.value = state; // Update full state for child
    roomInfo.roomName = state.roomName;
    roomInfo.maxPlayers = state.maxPlayers;
    roomInfo.gameType = state.gameType || 'top_hog';
    
    // 检查状态变化(在更新gameState之前)
    const previousState = gameState.value;
    const isNowGameOver = state.gameState === 'GAME_OVER';
    const isRoundOver = state.gameState === 'ROUND_OVER';
    
    console.log('状态检查:', {
        previousState,
        isNowGameOver,
        isRoundOver,
        shouldShow: isNowGameOver || isRoundOver
    });
    
    // 更新游戏状态
    gameState.value = state.gameState;
    players.value = state.players || {};

    // 如果状态发生变化,重置战绩确认标记
    if (previousState !== state.gameState) {
        victoryOverlayManuallyClosed.value = false;
        sessionStorage.removeItem(`victory_ack_${roomId.value}`);
        console.log('状态已变化,重置确认标记');
    }

    // 如果游戏结束或一轮结束,显示战绩面板
    if (isNowGameOver || isRoundOver) {
         const winnerName = state.winnerDisplayName || "某人";
         const msg = state.message || (isNowGameOver ? "游戏结束" : "本轮结束");
         
         if (isNowGameOver) {
             victoryMessage.value = msg.includes("获胜") ? msg : `游戏结束! ${winnerName} 获胜!`;
         } else {
             victoryMessage.value = msg;
         }
         
         // 每次状态变化时都显示战绩面板
         showVictoryOverlay.value = true;
         
         console.log('✅ 显示战绩面板:', {
             state: state.gameState,
             message: victoryMessage.value,
             playersCount: Object.keys(players.value).length,
             showVictoryOverlay: showVictoryOverlay.value
         });
    } else {
        showVictoryOverlay.value = false;
        console.log('❌ 隐藏战绩面板 (状态不是ROUND_OVER或GAME_OVER)');
    }
};

const handleQuickChat = () => {
    if (quickChat.value) {
        chatInput.value = quickChat.value;
    }
};

const sendChat = () => {
    if (!chatInput.value.trim()) return;
    send({ type: 'chat', roomId: roomId.value, data: { text: chatInput.value.trim() } });
    chatInput.value = '';
    quickChat.value = '';
};

// Actions exposed to child via event or just child calling props.wsSend directly
const onLeaveRoom = () => {
    const me = Object.values(players.value).find(p => p.sessionId === mySessionId.value);
    const isHost = me && me.isHost;
    const msg = isHost ? "你是房主，离开将导致房间销毁。确定要离开吗？" : "确定要离开房间吗？";

    if (confirm(msg)) {
        send({ type: 'leaveRoom', roomId: roomId.value });
    }
};

const onAddBot = async () => {
     try {
        const response = await api.post('/room/add-bots', { roomId: roomId.value, botCount: 1 });
        if (response.data.code !== 200) alert(response.data.message);
    } catch (e) {
        alert(e.response?.data?.message || '添加机器人失败');
    }
}

const closeVictoryOverlay = () => {
    showVictoryOverlay.value = false;
    victoryOverlayManuallyClosed.value = true;
    sessionStorage.setItem(`victory_ack_${roomId.value}`, 'true');
};

const returnToLobby = () => {
    if (ws.value) {
        send({ type: 'leaveRoom', roomId: roomId.value });
    }
    router.push('/lobby');
};

const requestNewGame = () => {
    if (ws.value) {
        send({ type: 'requestNewGame', roomId: roomId.value });
        closeVictoryOverlay();
        addLog('已请求再来一局');
    }
};

// Utils for sidebar
const isMe = (player) => {
    return (player.sessionId === mySessionId.value);
};

const getStatusClass = (player) => {
    if (player.isTrustee) return 'status-trustee';
    if (gameState.value === 'WAITING') return player.isReady ? 'status-ready' : 'status-not-ready';
    if (gameState.value === 'GAME_OVER') return player.requestedNewGame ? 'status-again' : 'status-ended';
    
    // 游戏进行中,检查是否已出牌
    if (gameState.value === 'PLAYING' || gameState.value === 'PROCESSING_TURN') {
        const hasPlayed = roomState.value.playedCardsThisTurn && 
                         roomState.value.playedCardsThisTurn[player.sessionId];
        return hasPlayed ? 'status-played' : 'status-playing';
    }
    
    return '';
};

const getStatusText = (player) => {
    if (player.isTrustee) return '托管中';
    if (gameState.value === 'WAITING') return player.isReady ? '已准备' : '未准备';
    if (gameState.value === 'GAME_OVER') return player.requestedNewGame ? '想再来' : '结束';
    
    // 游戏进行中,显示出牌状态
    if (gameState.value === 'PLAYING' || gameState.value === 'PROCESSING_TURN') {
        const hasPlayed = roomState.value.playedCardsThisTurn && 
                         roomState.value.playedCardsThisTurn[player.sessionId];
        return hasPlayed ? '已出牌' : '出牌中';
    }
    
    return '游戏中';
};

onMounted(() => {
    connect();
});

onUnmounted(() => {
    if (ws.value) ws.value.close();
});

watch(
  () => route.params.roomId,
  (newRoomId, oldRoomId) => {
    if (newRoomId && newRoomId !== oldRoomId) {
      roomId.value = newRoomId;
      if (ws.value) {
        ws.value.close();
        ws.value = null;
        isConnected.value = false;
      }
      chatMessages.value = [];
      logs.value = [];
      players.value = {};
      roomState.value = {};
      gameState.value = 'WAITING';
      nextTick(() => {
        connect();
      });
    }
  }
);
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700&display=swap');

.game-container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 20px;
    background-color: #f3f4f6;
    min-height: 100vh;
    font-family: 'Roboto', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    color: #374151;
}

/* Header */
.game-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    background: white;
    padding: 15px 25px;
    border-radius: 12px;
    box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}
.game-header h1 {
    font-size: 1.5rem;
    margin: 0;
    color: #111827;
}
.header-controls {
    display: flex;
    align-items: center;
    gap: 15px;
}
.icon-button {
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    transition: transform 0.2s;
}
.icon-button:hover { transform: scale(1.1); }
.player-count-badge {
    background-color: #e5e7eb;
    padding: 5px 12px;
    border-radius: 20px;
    font-weight: 500;
}

/* Layout */
.game-layout {
    display: flex;
    gap: 25px;
}
.game-main {
    flex: 3;
    display: flex;
    flex-direction: column;
    gap: 20px;
}
.game-sidebar {
    flex: 1;
    min-width: 300px;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

/* Buttons */
.btn {
    padding: 10px 20px;
    border: none;
    border-radius: 8px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
}
.btn:hover:not(:disabled) { filter: brightness(1.1); transform: translateY(-1px); }
.btn:active:not(:disabled) { transform: translateY(0); }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-primary { background-color: #4f46e5; color: white; }
.btn-secondary { background-color: #6b7280; color: white; }
.btn-success { background-color: #10b981; color: white; }
.btn-danger { background-color: #ef4444; color: white; }
.btn-warning { background-color: #f59e0b; color: black; }
.btn-info { background-color: #3b82f6; color: white; }
.btn-outline { background: transparent; border: 1px solid #9ca3af; color: #4b5563; }
.btn-outline-warning { background: white; border: 1px solid #f59e0b; color: #d97706; }
.btn-action { background-color: #4f46e5; color: white; font-size: 1.1rem; padding: 12px 30px; box-shadow: 0 4px 6px rgba(79, 70, 229, 0.3); }

.btn-small { padding: 5px 10px; font-size: 0.85rem; background-color: #4f46e5; color: white; }

/* Control Bar */
.control-bar {
    display: flex;
    justify-content: space-between;
    background: white;
    padding: 15px;
    border-radius: 12px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}
.left-controls, .right-controls { display: flex; gap: 10px; }

/* Board */
.board-area {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 20px;
    background-color: #e0e7ff;
    border-radius: 16px;
    min-height: 300px;
    border: 2px solid #c7d2fe;
}
.board-row {
    display: flex;
    align-items: center;
    background-color: rgba(255, 255, 255, 0.6);
    padding: 10px;
    border-radius: 10px;
    position: relative;
    transition: background 0.3s;
}
.board-row.row-selectable { background-color: #fff3cd; border: 2px dashed #f59e0b; }
.row-header {
    width: 80px;
    display: flex;
    flex-direction: column;
    align-items: center;
    font-weight: bold;
    color: #4b5563;
    flex-shrink: 0;
}
.row-cards {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    flex-grow: 1;
    padding-left: 10px;
    border-left: 2px solid #e5e7eb;
}
.empty-slot { color: #9ca3af; font-style: italic; display: flex; align-items: center; height: 90px; }

.row-overlay {
    position: absolute;
    right: 10px;
    top: 50%;
    transform: translateY(-50%);
    z-index: 10;
}

/* Cards */
.game-card {
    width: 60px;
    height: 90px;
    background-color: white;
    border-radius: 8px;
    border: 1px solid #d1d5db;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    padding: 5px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    user-select: none;
    font-weight: bold;
    position: relative;
    transition: transform 0.2s, box-shadow 0.2s;
}
.game-card .card-top { align-self: flex-start; font-size: 0.9rem; }
.game-card .card-center { color: #dc2626; font-size: 1.2rem; }
.game-card .card-bottom { align-self: flex-end; font-size: 0.8rem; color: #dc2626; }

.hand-card { cursor: pointer; height: 100px; width: 68px; }
.hand-card:hover { transform: translateY(-10px); box-shadow: 0 5px 15px rgba(0,0,0,0.15); z-index: 5; }
.hand-card.selected { border: 2px solid #4f46e5; transform: translateY(-15px); box-shadow: 0 8px 20px rgba(79, 70, 229, 0.25); z-index: 10; }

/* Hand Area */
.hand-area {
    background: white;
    padding: 20px;
    border-radius: 16px;
    box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}
.hand-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
}
.player-badge {
    background-color: #f3f4f6;
    padding: 5px 12px;
    border-radius: 20px;
    font-weight: 600;
}
.player-badge.is-trustee { background-color: #f3e8ff; color: #9333ea; border: 1px solid #d8b4fe; }

.hand-cards {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    justify-content: center;
    min-height: 110px;
}
.no-cards { color: #9ca3af; margin-top: 30px; }

.action-bar {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin-top: 20px;
    align-items: center;
}
.tip-bubble {
    background-color: #fffbeb;
    color: #b45309;
    padding: 10px;
    border-radius: 8px;
    margin-top: 15px;
    text-align: center;
    border: 1px solid #fcd34d;
}

/* Sidebar Panels */
.sidebar-panel {
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
    overflow: hidden;
    display: flex;
    flex-direction: column;
}
.sidebar-panel h3 {
    margin: 0;
    padding: 15px;
    background-color: #f9fafb;
    border-bottom: 1px solid #e5e7eb;
    font-size: 1.1rem;
}

/* Player List */
.player-list-items {
    list-style: none;
    padding: 0;
    margin: 0;
    max-height: 300px;
    overflow-y: auto;
}
.player-item {
    padding: 12px 15px;
    border-bottom: 1px solid #f3f4f6;
}
.player-item.is-me { background-color: #eff6ff; }
.player-info-row { display: flex; justify-content: space-between; font-weight: 500; margin-bottom: 5px; }
.player-status-row { display: flex; justify-content: space-between; font-size: 0.85rem; color: #6b7280; }
.status-tag { padding: 2px 6px; border-radius: 4px; font-size: 0.75rem; }
.status-ready { background-color: #d1fae5; color: #065f46; }
.status-not-ready { background-color: #fee2e2; color: #991b1b; }
.status-trustee { background-color: #f3e8ff; color: #6b21a8; }
.status-playing { background-color: #fef3c7; color: #d97706; font-weight: 600; }
.status-played { background-color: #d1fae5; color: #065f46; font-weight: 600; }
.status-again { background-color: #dbeafe; color: #1e40af; }
.status-ended { background-color: #e5e7eb; color: #6b7280; }

/* Chat */
.chat-window {
    height: 300px;
    overflow-y: auto;
    padding: 15px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}
.chat-msg { font-size: 0.9rem; }
.chat-sender { font-weight: bold; margin-right: 5px; color: #4b5563; }
.chat-controls { padding: 15px; border-top: 1px solid #e5e7eb; background-color: #f9fafb; display: flex; flex-direction: column; gap: 8px; }
.quick-chat-select, .input-group input {
    width: 100%;
    padding: 8px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    outline: none;
}
.input-group { display: flex; gap: 8px; }
.input-group input { flex: 1; }

/* Logs */
.log-window {
    height: 150px;
    overflow-y: auto;
    padding: 10px;
    font-size: 0.8rem;
    color: #6b7280;
    font-family: monospace;
}
.log-entry { margin-bottom: 4px; }

/* Alerts */
.alert { padding: 10px; border-radius: 6px; margin-bottom: 15px; }
.alert-error { background-color: #fee2e2; color: #991b1b; }
.alert-info { background-color: #dbeafe; color: #1e40af; }

/* Choice Panel */
.choice-panel {
    background-color: #fff7ed;
    border: 2px solid #f97316;
    padding: 15px;
    border-radius: 8px;
    margin-bottom: 20px;
    text-align: center;
}
.card-badge { font-weight: bold; color: #c2410c; }
.timer-text { color: #c2410c; font-weight: bold; margin-top: 5px; }

/* Game Over Results */
.victory-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.85);
    backdrop-filter: blur(8px);
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
}

.results-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 3px;
    border-radius: 24px;
    max-width: 600px;
    width: 100%;
    max-height: 90vh;
    overflow-y: auto;
    animation: slideUp 0.4s ease;
}

@keyframes slideUp {
    from {
        opacity: 0;
        transform: translateY(30px) scale(0.95);
    }
    to {
        opacity: 1;
        transform: translateY(0) scale(1);
    }
}

.results-card > * {
    background: white;
    border-radius: 22px;
}

/* Results Header */
.results-header {
    text-align: center;
    padding: 2rem 2rem 1.5rem;
    background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
    border-radius: 22px 22px 0 0;
}

.results-icon {
    font-size: 4rem;
    margin-bottom: 1rem;
    animation: bounce 0.6s ease;
}

@keyframes bounce {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-20px); }
}

.results-header h2 {
    margin: 0 0 0.5rem 0;
    font-size: 2rem;
    color: #111827;
}

.results-subtitle {
    margin: 0;
    font-size: 1.1rem;
    color: #6b7280;
}

/* Rankings Section */
.rankings-section {
    padding: 1.5rem 2rem;
}

.rankings-section h3 {
    margin: 0 0 1.5rem 0;
    font-size: 1.3rem;
    color: #111827;
    text-align: center;
}

.rankings-table {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

/* Ranking Row */
.ranking-row {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1rem;
    background: #f9fafb;
    border-radius: 12px;
    border: 2px solid transparent;
    transition: all 0.3s ease;
    animation: fadeInRow 0.4s ease backwards;
}

.ranking-row:nth-child(1) { animation-delay: 0.1s; }
.ranking-row:nth-child(2) { animation-delay: 0.15s; }
.ranking-row:nth-child(3) { animation-delay: 0.2s; }
.ranking-row:nth-child(4) { animation-delay: 0.25s; }
.ranking-row:nth-child(5) { animation-delay: 0.3s; }

@keyframes fadeInRow {
    from {
        opacity: 0;
        transform: translateX(-20px);
    }
    to {
        opacity: 1;
        transform: translateX(0);
    }
}

.ranking-row.rank-1 {
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
    border-color: #fbbf24;
    box-shadow: 0 4px 12px rgba(251, 191, 36, 0.3);
}

.ranking-row.rank-2 {
    background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
    border-color: #9ca3af;
}

.ranking-row.rank-3 {
    background: linear-gradient(135deg, #fed7aa 0%, #fdba74 100%);
    border-color: #fb923c;
}

.ranking-row.is-me {
    border-color: #4f46e5;
    box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.2);
}

/* Rank Badge */
.rank-badge {
    width: 50px;
    height: 50px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.medal {
    font-size: 2.5rem;
    animation: rotate 0.6s ease;
}

@keyframes rotate {
    from { transform: rotate(-180deg) scale(0); }
    to { transform: rotate(0) scale(1); }
}

.rank-number {
    font-size: 1.5rem;
    font-weight: 700;
    color: #6b7280;
}

/* Player Info */
.player-info {
    flex: 1;
    min-width: 0;
}

.player-name-row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.25rem;
}

.player-name {
    font-size: 1.1rem;
    font-weight: 600;
    color: #111827;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.you-badge {
    background: #4f46e5;
    color: white;
    padding: 0.15rem 0.5rem;
    border-radius: 4px;
    font-size: 0.75rem;
    font-weight: 600;
}

.player-stats {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.bot-badge,
.trustee-badge {
    font-size: 0.75rem;
    padding: 0.15rem 0.5rem;
    border-radius: 4px;
    font-weight: 500;
}

.bot-badge {
    background: #dbeafe;
    color: #1e40af;
}

.trustee-badge {
    background: #f3e8ff;
    color: #6b21a8;
}

/* Player Score */
.player-score {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    flex-shrink: 0;
}

.score-value {
    font-size: 1.8rem;
    font-weight: 700;
    color: #111827;
    line-height: 1;
}

.score-label {
    font-size: 0.85rem;
    color: #6b7280;
    margin-top: 0.25rem;
}

/* Results Actions */
.results-actions {
    display: flex;
    gap: 1rem;
    padding: 1.5rem 2rem 2rem;
    justify-content: center;
}

.results-actions .btn {
    flex: 1;
    max-width: 200px;
    padding: 0.875rem 1.5rem;
    font-size: 1rem;
}

/* Fade Transition */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

/* Responsive */
@media (max-width: 900px) {
    .game-layout { flex-direction: column; }
    .game-sidebar { width: 100%; }
}
</style>

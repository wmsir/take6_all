<template>
  <div class="game-container">
    <h1>牛头王 (Take 6!) - 房间: {{ roomInfo.roomName }}</h1>

    <div class="music-control">
         <button @click="toggleMusic">{{ isMusicPlaying ? '🎵 关闭背景音乐' : '🎵 开启背景音乐' }}</button>
         <!-- Hidden audio elements -->
         <audio ref="bgmAudio" loop src="/background.mp3"></audio>
         <audio ref="sfxAudio" src="/placeholder.mp3"></audio>
    </div>

    <!-- Victory Overlay -->
    <div v-if="showVictoryOverlay" class="victory-overlay">
        <div class="victory-message-content">
            <div class="victory-message">{{ victoryMessage }}</div>
            <button class="close-victory-button" @click="closeVictoryOverlay">关闭</button>
        </div>
    </div>

    <!-- Game Area -->
    <div class="game-layout">
        <div class="game-main-content">
             <!-- Controls -->
            <div class="game-controls">
                <div class="player-count-info">房间玩家: {{ playerCount }}/{{ maxPlayers }}</div>
                <button v-if="gameState === 'WAITING'"
                        :class="['ready-button', { 'is-ready': isMyPlayerReady }]"
                        @click="toggleReady"
                        :disabled="isMyPlayerBot">
                    {{ isMyPlayerReady ? '取消准备' : '准备' }}
                </button>
                <button v-if="isMyPlayerHost && gameState === 'WAITING'" @click="addBot">添加机器人</button>
                <button v-if="canStartGame" @click="startGame">开始游戏</button>
                <button @click="toggleAutoPlay"
                        :class="['ready-button', { 'is-ready': isMyPlayerBot }]"
                        v-if="myPlayer">
                    {{ isMyPlayerBot ? '取消托管' : '开启托管' }}
                </button>
                <button @click="leaveRoom">离开房间</button>
                <button v-if="gameState === 'GAME_OVER'"
                        @click="playAgain"
                        :disabled="hasRequestedNewGame">
                    {{ hasRequestedNewGame ? '已请求新局' : '再来一局' }}
                </button>
            </div>

            <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
            <div v-if="joiningFeedback" class="joining-feedback">{{ joiningFeedback }}</div>

             <!-- Choice Area -->
            <Transition name="popup">
                <div v-if="showChoiceArea" class="choice-overlay-modal">
                     <div class="player-choice-area section">
                        <h3>请选择您要收取的牌列</h3>
                        <p>您打出的牌: <strong>{{ cardLeadingToChoice ? `${cardLeadingToChoice.number} (🐂${cardLeadingToChoice.bullheads})` : '' }}</strong></p>
                        <p>请在 <strong style="color: red;">{{ choiceTimer }}</strong> 秒内点击对应牌列前的“选择此行”按钮：</p>
                    </div>
                </div>
            </Transition>

            <!-- Game Rows -->
            <h3>桌面牌列:</h3>
            <div class="game-rows">
                <div v-for="(row, index) in gameRows" :key="index" class="game-row">
                    <!-- Choice Button -->
                    <div v-if="showChoiceButtons && choiceOptions.some(opt => opt.rowIndex === index)" class="row-choice-control">
                        <span class="row-bullhead-info">(🐂 {{ getChoiceBullheads(index) }})</span>
                        <button class="select-row-button" @click="chooseRow(index)">选择此行</button>
                    </div>

                    <div class="game-row-content">
                        <strong>行 {{ index + 1 }}: </strong>
                        <div class="cards-container">
                            <div v-for="card in row.cards" :key="card.number" class="card">
                                <span class="number">{{ card.number }}</span>
                                <span class="bullheads">🐂 {{ card.bullheads }}</span>
                            </div>
                        </div>
                        <span v-if="!row.cards || row.cards.length === 0">(空)</span>
                    </div>
                </div>
            </div>

            <!-- Hand -->
            <h3>我的手牌 ({{ myHand.length }} 张):</h3>
            <div class="player-hand">
                <div v-if="myHand.length === 0" style="width: 100%; text-align: center;">(无手牌)</div>
                <div v-else
                     v-for="card in sortedHand"
                     :key="card.number"
                     :class="['card', 'clickable', { 'highlighted': selectedCard && selectedCard.number === card.number }]"
                     @click="selectCard(card)">
                    <span class="number">{{ card.number }}</span>
                    <span class="bullheads">🐂 {{ card.bullheads }}</span>
                </div>
            </div>

            <div class="play-card-action">
                <button @click="playCard" :disabled="!canPlayCard">出牌</button>
                <button @click="getTip" :disabled="!canGetTip" class="tip-btn">💡 获取提示</button>
            </div>
            <div v-if="tipMessage" class="tip-message-area">{{ tipMessage }}</div>

        </div>

        <div class="game-sidebar">
             <!-- Player List -->
            <div class="section player-list-container">
                <h3>房间内玩家:</h3>
                <ul class="player-list">
                    <li v-for="player in playerList" :key="player.sessionId" :style="{ fontWeight: isMe(player) ? 'bold' : 'normal' }">
                        <span class="player-details">
                            <span v-if="player.isHost" title="房主">🏠</span>
                            <strong>{{ player.displayName }}</strong>
                            <span v-if="player.isHost" style="color: #f39c12; font-size: 0.8em; margin-left: 5px;">(房主)</span>
                            - 牛头: {{ player.score || 0 }}, 手牌: {{ player.hand ? player.hand.length : '?' }}
                        </span>
                        <span class="player-status" :style="{ color: getStatusColor(player) }">
                            {{ getStatusText(player) }}
                        </span>
                    </li>
                </ul>
            </div>

            <!-- Chat -->
            <div class="section chat-area">
                <h3>房间聊天</h3>
                <div class="chat-messages" ref="chatBox">
                    <div v-for="(msg, i) in chatMessages" :key="i">
                        <strong>{{ msg.senderName }}:</strong> {{ msg.text }}
                    </div>
                </div>
                <select v-model="quickChat" @change="handleQuickChat">
                    <option value="">-- 选择快捷语句 --</option>
                    <option v-for="txt in quickChatOptions" :key="txt" :value="txt">{{ txt }}</option>
                </select>
                <div class="chat-input-container">
                    <input v-model="chatInput" @keypress.enter="sendChat" placeholder="输入聊天内容..." />
                    <button @click="sendChat">发送</button>
                </div>
            </div>

            <!-- Server Log -->
             <div class="section log-area">
                <h3>服务器日志</h3>
                <div class="server-messages" ref="logBox">
                     <div v-for="(log, i) in logs" :key="i" class="message-entry">{{ log }}</div>
                </div>
             </div>
        </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '../services/api';

const route = useRoute();
const router = useRouter();
const roomId = route.params.roomId;
const userInfo = reactive(JSON.parse(localStorage.getItem('user_info') || '{}'));

// Refs
const bgmAudio = ref(null);
const sfxAudio = ref(null);
const chatBox = ref(null);
const logBox = ref(null);

// State
const ws = ref(null);
const isConnected = ref(false);
const roomInfo = reactive({ roomName: '', roomId: '' });
const gameState = ref('WAITING'); // WAITING, PLAYING, GAME_OVER, WAITING_FOR_PLAYER_CHOICE, PROCESSING_TURN
const gameRows = ref([]);
const players = ref({});
const mySessionId = ref(null);
const myHand = ref([]);
const selectedCard = ref(null);
const isMusicPlaying = ref(false);
const chatMessages = ref([]);
const logs = ref([]);
const quickChat = ref('');
const chatInput = ref('');
const errorMessage = ref('');
const joiningFeedback = ref('');
const tipMessage = ref('');
const isWaitingForTurnProcessing = ref(false);

// Choice State
const showChoiceArea = ref(false);
const choiceOptions = ref([]);
const cardLeadingToChoice = ref(null);
const choiceTimer = ref(30);
let choiceInterval = null;

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
const sortedHand = computed(() => {
    return [...myHand.value].sort((a, b) => a.number - b.number);
});

const playerList = computed(() => Object.values(players.value));
const playerCount = computed(() => playerList.value.length);
const maxPlayers = computed(() => roomInfo.maxPlayers || 10);

const myPlayer = computed(() => {
    if (!players.value) return null;
    return Object.values(players.value).find(p => {
        // Match by sessionId if available, otherwise by userId or displayName as fallback
        if (mySessionId.value && p.sessionId === mySessionId.value) return true;
        if (userInfo.id && p.userId && String(p.userId) === String(userInfo.id)) return true;
        return false;
    });
});

const isMe = (player) => {
    return myPlayer.value && (player.sessionId === myPlayer.value.sessionId);
};

const isMyPlayerHost = computed(() => myPlayer.value?.isHost);
const isMyPlayerReady = computed(() => myPlayer.value?.isReady);
const isMyPlayerBot = computed(() => myPlayer.value?.is托管);
const hasRequestedNewGame = computed(() => myPlayer.value?.requestedNewGame);

const canStartGame = computed(() => {
    if (gameState.value !== 'WAITING') return false;
    if (!myPlayer.value || myPlayer.value.is托管) return false;
    // Check if at least 2 humans are ready
    const humans = playerList.value.filter(p => !p.is托管);
    const readyHumans = humans.filter(p => p.isReady);
    return humans.length >= 2 && readyHumans.length === humans.length;
});

const canPlayCard = computed(() => {
    return gameState.value === 'PLAYING' &&
           !isWaitingForTurnProcessing.value &&
           myPlayer.value &&
           !myPlayer.value.is托管 &&
           selectedCard.value;
});

const canGetTip = computed(() => {
    return gameState.value === 'PLAYING' &&
           !isWaitingForTurnProcessing.value &&
           myPlayer.value &&
           !myPlayer.value.is托管;
});

const showChoiceButtons = computed(() => {
    return gameState.value === 'WAITING_FOR_PLAYER_CHOICE' &&
           roomInfo.playerChoosingRowSessionId === mySessionId.value;
});

// Methods

const addLog = (msg) => {
    const time = new Date().toLocaleTimeString();
    logs.value.push(`[${time}] ${msg}`);
    nextTick(() => {
        if (logBox.value) logBox.value.scrollTop = logBox.value.scrollHeight;
    });
};

const playSound = (type = 'action') => {
    if (sfxAudio.value) {
        sfxAudio.value.currentTime = 0;
        sfxAudio.value.play().catch(e => console.warn("SFX error", e));
    }
};

const toggleMusic = () => {
    if (!bgmAudio.value) return;
    if (isMusicPlaying.value) {
        bgmAudio.value.pause();
        isMusicPlaying.value = false;
    } else {
        bgmAudio.value.play().catch(e => {
            console.warn("BGM error", e);
            alert("无法播放背景音乐，请检查文件或浏览器设置。");
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
    // Note: The backend expects 'userIdentifier' to be the numeric User ID
    const wsUrl = `ws://${window.location.hostname}:8088/ws-game?userIdentifier=${userInfo.id}`;
    addLog(`Connecting to ${wsUrl}`);

    ws.value = new WebSocket(wsUrl);

    ws.value.onopen = () => {
        addLog("WebSocket Open");
        isConnected.value = true;
        // Join Room automatically
        send({ type: 'joinRoom', roomId });
    };

    ws.value.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            handleMessage(data);
        } catch (e) {
            console.error(e);
            addLog("Error parsing message: " + event.data);
        }
    };

    ws.value.onclose = () => {
        addLog("WebSocket Closed");
        isConnected.value = false;
        // Maybe handle reconnect
    };

    ws.value.onerror = (e) => {
        console.error(e);
        addLog("WebSocket Error");
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
        addLog(`Session ID: ${data.sessionId}`);
        if (data.vipStatus !== undefined) {
             userInfo.vipStatus = data.vipStatus;
             localStorage.setItem('user_info', JSON.stringify(userInfo));
        }
    } else if (data.type === 'gameStateUpdate') {
        updateGameState(data.roomState);
        if (data.message) addLog(data.message);
    } else if (data.type === 'rejoinSuccess') {
        addLog("Rejoined successfully");
        if (data.roomState) updateGameState(data.roomState);
    } else if (data.type === 'chatMessage') {
        // Need to resolve sender name
        const senderId = data.sender;
        // Try to find player name
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
    } else if (data.type === 'playTipResponse') {
        if (data.success && data.tip) {
            const tip = data.tip;
            tipMessage.value = `💡 AI建议: 打出 ${tip.suggestedCardNumber} (预计牛头: ${tip.estimatedBullheads}). ${tip.reason}`;
            // Auto select
            const card = myHand.value.find(c => c.number === tip.suggestedCardNumber);
            if (card) selectCard(card);
        } else {
            tipMessage.value = `💡 ${data.message}`;
        }
    } else if (data.type === 'promptToChooseRow') {
        if (data.playerChoosingRowSessionId === mySessionId.value) {
            choiceOptions.value = data.options;
            cardLeadingToChoice.value = data.cardPlayed;
            showChoiceArea.value = true;
            choiceTimer.value = (data.timeout || 30000) / 1000;
            if (choiceInterval) clearInterval(choiceInterval);
            choiceInterval = setInterval(() => {
                choiceTimer.value--;
                if (choiceTimer.value <= 0) clearInterval(choiceInterval);
            }, 1000);
        }
    } else if (data.type === 'error') {
        errorMessage.value = data.message;
        isWaitingForTurnProcessing.value = false;
        setTimeout(() => errorMessage.value = '', 5000);
    } else if (data.type === 'leftRoomSuccess') {
        router.push('/lobby');
    }
};

const updateGameState = (state) => {
    roomInfo.roomName = state.roomName;
    roomInfo.maxPlayers = state.maxPlayers;
    roomInfo.playerChoosingRowSessionId = state.playerChoosingRowSessionId;

    // Handle Game Over Overlay logic
    if (gameState.value === 'GAME_OVER' && state.gameState !== 'GAME_OVER') {
        victoryOverlayManuallyClosed.value = false;
    }

    gameState.value = state.gameState;
    gameRows.value = state.rows || [];
    players.value = state.players || {};

    // Update Hand
    if (myPlayer.value && myPlayer.value.hand) {
        // Only update hand if changed to avoid jumpy UI, or just update always since it's simple
        myHand.value = myPlayer.value.hand;
    } else {
        myHand.value = [];
    }

    // Reset processing flag if state changes (e.g. next turn)
    if (isWaitingForTurnProcessing.value && state.gameState === 'PLAYING') {
         // This is a bit simplistic, ideally we check turn number
         isWaitingForTurnProcessing.value = false;
         joiningFeedback.value = '';
    }

    if (state.gameState !== 'WAITING_FOR_PLAYER_CHOICE') {
        showChoiceArea.value = false;
        if (choiceInterval) clearInterval(choiceInterval);
    }

    // Victory
    if (state.gameState === 'GAME_OVER') {
         const winnerName = state.winnerDisplayName || "某人";
         const msg = state.message || "游戏结束";
         victoryMessage.value = msg.includes("获胜") ? msg : `游戏结束! ${winnerName} 获胜!`;
         if (!victoryOverlayManuallyClosed.value) {
             showVictoryOverlay.value = true;
         }
    } else {
        showVictoryOverlay.value = false;
    }
};

const toggleReady = () => {
    send({ type: 'playerReady', roomId });
};

const startGame = () => {
    send({ type: 'startGame', roomId });
};

const toggleAutoPlay = () => {
    send({ type: 'toggleAutoPlay', roomId });
};

const addBot = async () => {
    try {
        const response = await api.post('/room/add-bots', { roomId, botCount: 1 });
        if (response.data.code !== 200) {
            alert(response.data.message);
        }
    } catch (e) {
        alert(e.response?.data?.message || '添加机器人失败');
    }
};

const selectCard = (card) => {
    selectedCard.value = card;
    tipMessage.value = '';
};

const playCard = () => {
    if (!selectedCard.value) return;
    isWaitingForTurnProcessing.value = true;
    joiningFeedback.value = "正在出牌...";
    send({ type: 'playCard', roomId, data: { cardNumber: selectedCard.value.number } });
    selectedCard.value = null; // Clear selection
};

const getTip = () => {
    send({ type: 'requestPlayTip', roomId });
};

const chooseRow = (rowIndex) => {
    send({ type: 'selectRow', roomId, rowIndex: rowIndex }); // Backend supports 'selectRow' with direct 'rowIndex' or 'playerChoosesRow'
    showChoiceArea.value = false;
};

const getChoiceBullheads = (rowIndex) => {
    const opt = choiceOptions.value.find(o => o.rowIndex === rowIndex);
    return opt ? opt.bullheads : '?';
};

const handleQuickChat = () => {
    if (quickChat.value) {
        chatInput.value = quickChat.value;
    }
};

const sendChat = () => {
    if (!chatInput.value.trim()) return;
    send({ type: 'chat', roomId, data: { text: chatInput.value.trim() } });
    chatInput.value = '';
    quickChat.value = '';
};

const leaveRoom = () => {
    if (confirm("确定要离开房间吗？")) {
        send({ type: 'leaveRoom', roomId });
        // Client side navigation happens on 'leftRoomSuccess'
    }
};

const playAgain = () => {
    send({ type: 'requestNewGame', roomId });
};

const closeVictoryOverlay = () => {
    showVictoryOverlay.value = false;
    victoryOverlayManuallyClosed.value = true;
};

const getStatusColor = (player) => {
    if (player.is托管) return '#7f8c8d';
    if (gameState.value === 'WAITING') return player.isReady ? 'green' : 'orange';
    if (gameState.value === 'GAME_OVER') return player.requestedNewGame ? 'purple' : 'black';
    return 'black';
};

const getStatusText = (player) => {
    if (player.is托管) return '(托管中)';
    if (gameState.value === 'WAITING') return player.isReady ? '(已准备)' : '(未准备)';
    if (gameState.value === 'GAME_OVER') return player.requestedNewGame ? '(想再来一局)' : '(游戏结束)';
    return '';
};

onMounted(() => {
    connect();
});

onUnmounted(() => {
    if (ws.value) ws.value.close();
});
</script>

<style scoped>
.game-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
    background-color: #f0f2f5;
    min-height: 100vh;
}
.music-control {
    text-align: center;
    margin-bottom: 20px;
}
/* Layout */
.game-layout {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
}
.game-main-content {
    flex: 3;
    min-width: 450px;
    display: flex;
    flex-direction: column;
}
.game-sidebar {
    flex: 2;
    min-width: 300px;
    display: flex;
    flex-direction: column;
}

/* Controls */
.game-controls {
    margin-bottom: 15px;
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
}
.player-count-info {
    font-weight: bold;
    background-color: #e0e0e0;
    padding: 8px 12px;
    border-radius: 4px;
}
.ready-button { background-color: #ef5350; }
.ready-button.is-ready { background-color: #4caf50; }

/* Game Rows */
.game-rows {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 15px;
    padding: 10px;
    border: 1px solid #b0bec5;
    border-radius: 4px;
    background-color: #eceff1;
    min-height: 200px;
}
.game-row {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px;
    background-color: #cfd8dc;
    border-radius: 3px;
    min-height: 100px;
    position: relative;
}
.game-row-content {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    flex-grow: 1;
    overflow: visible; /* Allow animations to fly in from outside if needed */
}
.cards-container {
    display: flex;
    flex-wrap: nowrap;
    overflow-x: auto;
    align-items: center;
    flex-grow: 1;
    min-height: 85px; /* Ensure height for animation */
}

/* Animations */
.popup-enter-active,
.popup-leave-active {
  transition: all 0.5s ease;
}
.popup-enter-from,
.popup-leave-to {
  opacity: 0;
  transform: scale(0.5) translateY(-50px);
}

/* Card */
.card {
    width: 55px;
    height: 80px;
    border: 1px solid #546e7a;
    border-radius: 5px;
    background-color: white;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    font-weight: bold;
    box-shadow: 0 1px 3px rgba(0,0,0,0.2);
    margin: 2px;
    flex-shrink: 0;
    user-select: none;
}
.card .number { font-size: 20px; }
.card .bullheads { font-size: 11px; color: #d32f2f; }
.card.clickable { cursor: pointer; transition: transform 0.1s; }
.card.clickable:hover { transform: translateY(-3px); }
.card.highlighted { border: 2px solid #f57c00; box-shadow: 0 0 8px #f57c00; transform: scale(1.05); }

/* Hand */
.player-hand {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 10px;
    border: 1px solid #a5d6a7;
    border-radius: 4px;
    min-height: 95px;
    background-color: #e8f5e9;
    margin-bottom: 15px;
    justify-content: center;
}
.play-card-action {
    text-align: center;
    margin-top: 10px;
}
.tip-btn { background-color: #f39c12; margin-left: 10px; }
.tip-message-area {
    margin-top: 8px;
    padding: 8px;
    background-color: #e9ecef;
    border-left: 3px solid #f39c12;
    border-radius: 4px;
}

/* Sidebar */
.section {
    margin-bottom: 15px;
    padding: 15px;
    border: 1px solid #e0e0e0;
    border-radius: 6px;
    background-color: #f9f9f9;
}
.player-list {
    list-style: none;
    padding: 0;
    max-height: 250px;
    overflow-y: auto;
}
.player-list li {
    padding: 10px 8px;
    border-bottom: 1px solid #f0f0f0;
    display: flex;
    justify-content: space-between;
}

/* Chat */
.chat-area { display: flex; flex-direction: column; height: 350px; }
.chat-messages {
    flex-grow: 1;
    overflow-y: auto;
    border: 1px solid #ccc;
    padding: 10px;
    background: white;
    margin-bottom: 10px;
    text-align: left;
}
.chat-input-container { display: flex; gap: 8px; }
.chat-input-container input { flex: 1; margin-bottom: 0; }

/* Log */
.server-messages {
    height: 120px;
    overflow-y: auto;
    border: 1px solid #ccc;
    background: white;
    font-size: 0.8em;
    padding: 5px;
}

/* Choice */
.choice-overlay-modal {
    position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
    background-color: rgba(0,0,0,0.5);
    z-index: 900;
    display: flex;
    justify-content: center;
    align-items: center;
}
.player-choice-area {
    border: 2px solid #f57c00;
    background-color: #fffde7;
    padding: 30px;
    max-width: 500px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.3);
}
.row-choice-control {
    margin-right: 10px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
}
.select-row-button {
    background-color: #ffa000;
    padding: 4px 8px;
    font-size: 0.8em;
}

/* Overlay */
.victory-overlay {
    position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
    background-color: rgba(0,0,0,0.75);
    z-index: 1000;
    display: flex;
    justify-content: center;
    align-items: center;
}
.victory-message-content {
    background: rgba(0,0,0,0.8);
    padding: 40px;
    border-radius: 15px;
    text-align: center;
}
.victory-message {
    color: #ffd700;
    font-size: 2.5em;
    margin-bottom: 20px;
}
.close-victory-button {
    background-color: #e67e22;
    padding: 10px 30px;
    font-size: 1.2em;
}

.error-message {
    background-color: #ffebee;
    color: #c62828;
    padding: 10px;
    border-radius: 4px;
    margin-bottom: 10px;
}
.joining-feedback {
    background-color: #fff3e0;
    color: #ef6c00;
    padding: 10px;
    border-radius: 4px;
    margin-bottom: 10px;
}
</style>

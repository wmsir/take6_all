<template>
  <div class="game-layout">
    <!-- Main Game Area -->
    <div class="game-main">
         <!-- Game Status Bar (Optional, if game specific controls are needed) -->
        <div class="control-bar">
            <div class="left-controls">
                <button v-if="gameState === 'WAITING'" class="btn btn-outline" @click="leaveRoom">
                    离开
                </button>
                <button v-if="gameState === 'WAITING'"
                        :class="['btn', isMyPlayerReady ? 'btn-danger' : 'btn-success']"
                        @click="toggleReady"
                        :disabled="isMyPlayerBot">
                    {{ isMyPlayerReady ? '取消准备' : '准备' }}
                </button>
                <button v-if="isMyPlayerHost && gameState === 'WAITING'" class="btn btn-secondary" @click="addBot">
                    + 机器人
                </button>
                <button v-if="canStartGame" class="btn btn-primary" @click="startGame">
                    开始游戏
                </button>
            </div>

            <div class="right-controls">
                <!-- Trustee Button: Only visible when game is NOT waiting -->
                <button @click="toggleAutoPlay"
                        :class="['btn', isMyPlayerBot ? 'btn-danger' : 'btn-info', 'trustee-btn']"
                        v-if="myPlayer && gameState !== 'WAITING'">
                    <span class="btn-icon">{{ isMyPlayerBot ? '🤖' : '🎮' }}</span>
                    {{ isMyPlayerBot ? '取消托管' : '开启托管' }}
                </button>
                 <button v-if="gameState === 'GAME_OVER'"
                        class="btn btn-primary"
                        @click="playAgain"
                        :disabled="hasRequestedNewGame">
                    {{ hasRequestedNewGame ? '已请求' : '再来一局' }}
                </button>
                <button v-if="gameState !== 'WAITING'" class="btn btn-outline" @click="leaveRoom">离开</button>
            </div>
        </div>

        <!-- Choice Area -->
        <Transition name="slide-down">
            <div v-if="showChoiceArea" class="choice-panel">
                <h3>⚠️ 请选择要收取的牌列</h3>
                <div class="choice-info">
                    <p>您打出的牌: <span class="card-badge">{{ cardLeadingToChoice ? `${cardLeadingToChoice.number}` : '' }} <small>🐂{{ cardLeadingToChoice ? cardLeadingToChoice.bullheads : '' }}</small></span></p>
                    <p class="timer-text">剩余时间: {{ choiceTimer }}s</p>
                </div>
            </div>
        </Transition>

        <!-- Board (Rows) -->
        <div class="board-area">
            <div v-for="(row, index) in gameRows" :key="index" class="board-row" :class="{ 'row-selectable': showChoiceButtons && choiceOptions.some(opt => opt.rowIndex === index) }">
                <div class="row-header">
                    <span class="row-label">行 {{ index + 1 }}</span>
                    <span class="row-bullheads" v-if="row.cards && row.cards.length > 0">
                       🐂 {{ row.cards.reduce((acc, c) => acc + c.bullheads, 0) }}
                    </span>
                </div>

                <div class="row-cards">
                    <div v-for="card in row.cards" :key="card.number" class="game-card">
                        <div class="card-top">{{ card.number }}</div>
                        <div class="card-center">🐂</div>
                        <div class="card-bottom">{{ card.bullheads }}</div>
                    </div>
                    <div v-if="!row.cards || row.cards.length === 0" class="empty-slot">Empty</div>
                </div>

                <!-- Selection Overlay for Choice -->
                <div v-if="showChoiceButtons && choiceOptions.some(opt => opt.rowIndex === index)" class="row-overlay">
                     <button class="btn btn-warning select-row-btn" @click="chooseRow(index)">
                         选择此行 (+{{ getChoiceBullheads(index) }} 🐂)
                     </button>
                </div>
            </div>
        </div>

        <!-- Player Hand -->
        <div class="hand-area">
            <div class="hand-header">
                <h3>我的手牌</h3>
                 <div class="my-info">
                    <span class="player-badge" :class="{ 'is-trustee': isMyPlayerBot }">
                        {{ myPlayer ? myPlayer.displayName : 'Loading...' }}
                    </span>
                 </div>
            </div>

            <div class="hand-cards">
                 <div v-if="myHand.length === 0" class="no-cards">无手牌</div>
                 <div v-else
                      v-for="card in sortedHand"
                      :key="card.number"
                      :class="['game-card', 'hand-card', { 'selected': selectedCard && selectedCard.number === card.number }]"
                      @click="selectCard(card)">
                     <div class="card-top">{{ card.number }}</div>
                     <div class="card-center">🐂</div>
                     <div class="card-bottom">{{ card.bullheads }}</div>
                 </div>
            </div>

            <div class="action-bar">
                <button class="btn btn-action" @click="playCard" :disabled="!canPlayCard">
                    出牌
                </button>
                <button class="btn btn-outline-warning" @click="getTip" :disabled="!canGetTip">
                    💡 提示
                </button>
            </div>
            <div v-if="tipMessage" class="tip-bubble">{{ tipMessage }}</div>
        </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue';

const props = defineProps({
  gameState: String,
  roomState: Object,
  mySessionId: String,
  userInfo: Object,
  wsSend: Function
});

const emit = defineEmits(['leaveRoom']);

const gameRows = computed(() => props.roomState.rows || []);
const players = computed(() => props.roomState.players || {});
const playerList = computed(() => Object.values(players.value));

const myPlayer = computed(() => {
    if (!players.value) return null;
    return Object.values(players.value).find(p => {
        if (props.mySessionId && p.sessionId === props.mySessionId) return true;
        if (props.userInfo.id && p.userId && String(p.userId) === String(props.userInfo.id)) return true;
        return false;
    });
});
const myHand = computed(() => myPlayer.value?.hand || []);
const sortedHand = computed(() => [...myHand.value].sort((a, b) => a.number - b.number));
const isMyPlayerHost = computed(() => myPlayer.value?.isHost);
const isMyPlayerReady = computed(() => myPlayer.value?.isReady);
const isMyPlayerBot = computed(() => myPlayer.value?.isTrustee);
const hasRequestedNewGame = computed(() => myPlayer.value?.requestedNewGame);

const selectedCard = ref(null);
const tipMessage = ref('');
const isWaitingForTurnProcessing = ref(false);

const canStartGame = computed(() => {
    if (props.gameState !== 'WAITING') return false;
    if (!myPlayer.value || myPlayer.value.isTrustee) return false;
    const humans = playerList.value.filter(p => !p.isTrustee);
    const readyHumans = humans.filter(p => p.isReady);
    return humans.length >= 2 && readyHumans.length === humans.length;
});

const canPlayCard = computed(() => {
    return props.gameState === 'PLAYING' &&
           !isWaitingForTurnProcessing.value &&
           myPlayer.value &&
           !myPlayer.value.isTrustee &&
           selectedCard.value;
});

const canGetTip = computed(() => {
    return props.gameState === 'PLAYING' &&
           !isWaitingForTurnProcessing.value &&
           myPlayer.value &&
           !myPlayer.value.isTrustee;
});


// State specific to Top Hog
const showChoiceArea = ref(false);
const choiceOptions = ref([]);
const cardLeadingToChoice = ref(null);
const choiceTimer = ref(30);
let choiceInterval = null;

// Watch for special Top Hog events in roomState or messages?
// The parent passes full roomState.
// We might need a way to receive "events" from parent.
// Or we check props.roomState changes.
// The original code handled 'needSelectRow' event.
// Ideally, the parent should pass "lastMessage" or emit event to us.
// For now, let's expose a method or watch a prop if we pass message events down.
// Simpler: Parent handles WebSocket messages and if it's board specific, calls a method on this ref?
// Or we pass the "latestGameEvent" prop.

// Let's rely on props.roomState for state, but for 'needSelectRow' which is an event, we need a mechanism.
// Let's add an expose method handleGameEvent(data)

const handleGameEvent = (data) => {
    if (data.type === 'playTipResponse') {
        if (data.success && data.tip) {
            const tip = data.tip;
            tipMessage.value = `AI建议: ${tip.suggestedCardNumber} (预计牛头: ${tip.estimatedBullheads}). ${tip.reason}`;
            const card = myHand.value.find(c => c.number === tip.suggestedCardNumber);
            if (card) selectCard(card);
        } else {
            tipMessage.value = `${data.message}`;
        }
    } else if (data.type === 'needSelectRow') {
        const payload = data.data;
        choiceOptions.value = payload.options;
        cardLeadingToChoice.value = { number: payload.cardNumber, bullheads: '?' };
        showChoiceArea.value = true;
        choiceTimer.value = (payload.timeout || 30000) / 1000;
        if (choiceInterval) clearInterval(choiceInterval);
        choiceInterval = setInterval(() => {
            choiceTimer.value--;
            if (choiceTimer.value <= 0) clearInterval(choiceInterval);
        }, 1000);
    }
};

defineExpose({ handleGameEvent });

// Logic
const toggleReady = () => props.wsSend({ type: 'playerReady', roomId: props.roomState.roomId });
const startGame = () => props.wsSend({ type: 'startGame', roomId: props.roomState.roomId });
const toggleAutoPlay = () => props.wsSend({ type: 'toggleAutoPlay', roomId: props.roomState.roomId });
const addBot = () => {
    // This was an API call in original, simpler to emit or keep as API call if we import api
    // Let's use props.wsSend if backend supported it, or import api here.
    // Original used API. Let's keep using API for consistency or move to parent.
    // To minimize dependencies here, let's emit 'addBot'.
    emit('addBot');
};
const leaveRoom = () => emit('leaveRoom');
const playAgain = () => props.wsSend({ type: 'requestNewGame', roomId: props.roomState.roomId });

const selectCard = (card) => {
    selectedCard.value = card;
    tipMessage.value = '';
};

const playCard = () => {
    if (!selectedCard.value) return;
    isWaitingForTurnProcessing.value = true;
    props.wsSend({ type: 'playCard', roomId: props.roomState.roomId, data: { cardNumber: selectedCard.value.number } });
    selectedCard.value = null;
};

const getTip = () => props.wsSend({ type: 'requestPlayTip', roomId: props.roomState.roomId });

const chooseRow = (rowIndex) => {
    props.wsSend({ type: 'selectRow', roomId: props.roomState.roomId, rowIndex: rowIndex });
    showChoiceArea.value = false;
};

const getChoiceBullheads = (rowIndex) => {
    const opt = choiceOptions.value.find(o => o.rowIndex === rowIndex);
    return opt ? opt.bullheads : '?';
};

const showChoiceButtons = computed(() => showChoiceArea.value);

// Watchers
watch(() => props.gameState, (newState) => {
     if (newState !== 'WAITING_FOR_PLAYER_CHOICE') {
        showChoiceArea.value = false;
        if (choiceInterval) clearInterval(choiceInterval);
    }
    if (isWaitingForTurnProcessing.value && newState === 'PLAYING') {
         isWaitingForTurnProcessing.value = false;
    }
});

</script>

<style scoped>
/* Copy relevant styles from Game.vue */
.game-layout { display: flex; gap: 25px; flex: 1; }
.game-main { flex: 1; display: flex; flex-direction: column; gap: 20px; }
.control-bar { display: flex; justify-content: space-between; background: white; padding: 15px; border-radius: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.left-controls, .right-controls { display: flex; gap: 10px; }

.btn { padding: 10px 20px; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: inline-flex; align-items: center; justify-content: center; gap: 8px; }
.btn:hover:not(:disabled) { filter: brightness(1.1); transform: translateY(-1px); }
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

/* Board */
.board-area { display: flex; flex-direction: column; gap: 12px; padding: 20px; background-color: #e0e7ff; border-radius: 16px; min-height: 300px; border: 2px solid #c7d2fe; }
.board-row { display: flex; align-items: center; background-color: rgba(255, 255, 255, 0.6); padding: 10px; border-radius: 10px; position: relative; transition: background 0.3s; }
.board-row.row-selectable { background-color: #fff3cd; border: 2px dashed #f59e0b; }
.row-header { width: 80px; display: flex; flex-direction: column; align-items: center; font-weight: bold; color: #4b5563; flex-shrink: 0; }
.row-cards { display: flex; gap: 10px; flex-wrap: wrap; flex-grow: 1; padding-left: 10px; border-left: 2px solid #e5e7eb; }
.empty-slot { color: #9ca3af; font-style: italic; display: flex; align-items: center; height: 90px; }
.row-overlay { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); z-index: 10; }

/* Cards */
.game-card { width: 60px; height: 90px; background-color: white; border-radius: 8px; border: 1px solid #d1d5db; display: flex; flex-direction: column; align-items: center; justify-content: space-between; padding: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); user-select: none; font-weight: bold; position: relative; transition: transform 0.2s, box-shadow 0.2s; }
.game-card .card-top { align-self: flex-start; font-size: 0.9rem; }
.game-card .card-center { color: #dc2626; font-size: 1.2rem; }
.game-card .card-bottom { align-self: flex-end; font-size: 0.8rem; color: #dc2626; }

.hand-card { cursor: pointer; height: 100px; width: 68px; }
.hand-card:hover { transform: translateY(-10px); box-shadow: 0 5px 15px rgba(0,0,0,0.15); z-index: 5; }
.hand-card.selected { border: 2px solid #4f46e5; transform: translateY(-15px); box-shadow: 0 8px 20px rgba(79, 70, 229, 0.25); z-index: 10; }

/* Hand Area */
.hand-area { background: white; padding: 20px; border-radius: 16px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
.hand-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.hand-cards { display: flex; gap: 10px; padding: 20px 0; overflow-x: auto; min-height: 140px; }
.action-bar { display: flex; gap: 15px; justify-content: center; margin-top: 15px; }
.tip-bubble { background-color: #fff3cd; color: #856404; padding: 10px; border-radius: 8px; margin-top: 10px; text-align: center; border: 1px solid #ffeeba; }

/* Choice Panel */
.choice-panel { background-color: #fff3cd; color: #856404; padding: 15px; border-radius: 8px; margin-bottom: 15px; border: 1px solid #ffeeba; text-align: center; }
.card-badge { display: inline-block; background: white; padding: 2px 6px; border-radius: 4px; border: 1px solid #ccc; font-weight: bold; }
</style>

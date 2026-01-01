const app = getApp();
const gameApi = require('../../api/gameApi.js');

let socketOpen = false;
let socketMsgQueue = [];
let socketConnecting = false;

Page({
  data: {
    roomId: null,
    roomInfo: {},
    players: [],
    playerHand: [],
    selectedCard: null,
    canPlay: false,
    isProcessing: false,
    chatMessages: [],
    chatInput: '',
    lastMessageId: '',
    wsConnected: false,
    // 新增字段
    currentRound: 1,
    currentTurn: 1,
    myScore: 0,
    otherScores: '',
    countdown: 30,
    rows: [],
    revealedCards: [],
    sortedCards: '',
    handCount: 0,
    countdownTimer: null,
    // 出牌动画相关
    playingCards: [],  // 正在播放动画的卡牌 [{cardNumber, bullheads, playerName}]
    animatingCards: [], // 当前动画中的卡牌
    // 选择收牌行相关
    showSelectRowModal: false,  // 显示选择收牌行弹窗
    selectRowCountdown: 30,     // 选择收牌行倒计时
    selectRowTimer: null,       // 选择收牌行定时器
    selectedRowIndex: null,     // 已选择的行索引
    // 玩家网格布局相关
    allPlayersPlayed: false,    // 所有玩家是否都出完牌
    emptySlots: [],             // 空位数组（用于填充到10个位置）
    // 托管相关
    isHosting: false            // 是否开启托管
  },
  // 节流：避免 onLoad/onShow/wsOpen 多处触发导致 getGameState 连续请求
  _fetchingGameState: false,
  _lastFetchGameStateAt: 0,
  _playTimeoutTimer: null,

  onLoad(options) {
    console.log('游戏页面 onLoad，options:', options);
    
    // 从多个来源获取 roomId（优先级：options > 全局数据 > 存储）
    let roomId = options.roomId;
    let gameState = null;
    
    if (!roomId) {
      const globalRoom = app.globalData.currentRoom;
      if (globalRoom) {
        roomId = globalRoom.roomId || globalRoom.id;
        // 如果有保存的游戏状态，先使用它
        gameState = app.globalData.gameState || globalRoom;
      }
    }
    
    if (!roomId) {
      const storedRoom = wx.getStorageSync('currentRoom');
      if (storedRoom) {
        roomId = storedRoom.roomId || storedRoom.id;
      }
    }
    
    // 尝试从存储中获取游戏状态
    if (!gameState) {
      gameState = wx.getStorageSync('gameState');
    }
    
    if (roomId) {
      console.log('找到房间ID:', roomId);
      this.setData({ roomId: roomId });
      
      // 如果有保存的游戏状态，先使用它渲染页面（避免空白）
      if (gameState && gameState.gameState === 'PLAYING') {
        console.log('使用保存的游戏状态渲染页面');
        this.handleRoomUpdate(gameState);
        // 即使有保存的状态，也获取一次最新状态（确保数据是最新的）
        console.log('虽然有保存的状态，但仍获取最新状态');
        setTimeout(() => {
          this.fetchGameState(roomId);
        }, 300);
      } else {
        // 如果没有保存的状态，立即尝试获取一次（不等待WebSocket）
        console.log('没有保存的游戏状态，立即尝试获取');
        setTimeout(() => {
          this.fetchGameState(roomId);
        }, 200);
      }
      
      // 不在 onLoad 主动连接 WebSocket，统一在 onShow 处理，避免重复连接导致事件丢失/数据不同步
    } else {
      console.warn('未找到房间ID，将在onShow中重试');
      // 不立即提示错误，等待onShow时再处理
    }
  },

  onShow() {
    // 每次显示页面时，检查是否需要重新连接 WebSocket
    let roomId = this.data.roomId;
    console.log('[GAME] onShow state:', {
      roomId,
      wsConnected: this.data.wsConnected,
      socketOpen,
      socketConnecting
    });
    
    // 如果没有roomId，尝试从全局数据或存储中获取
    if (!roomId) {
      const globalRoom = app.globalData.currentRoom || wx.getStorageSync('currentRoom');
      if (globalRoom) {
        roomId = globalRoom.roomId || globalRoom.id;
        if (roomId) {
          console.log('onShow中找到房间ID:', roomId);
          this.setData({ roomId: roomId });
        }
      }
    }
    
    if (roomId) {
      // 如果WebSocket未连接，则连接
      if (!this.data.wsConnected && !socketOpen && !socketConnecting) {
        console.log('onShow中连接WebSocket，roomId:', roomId);
        this.connectWebSocket(roomId);
      } else if (this.data.wsConnected || socketOpen) {
        // 如果已经连接，也尝试获取一次游戏状态（确保数据是最新的）
        console.log('onShow中WebSocket已连接，主动获取游戏状态，roomId:', roomId);
        this.fetchGameState(roomId);
      } else {
        // 如果正在连接中，也尝试获取一次（不等待连接成功）
        console.log('onShow中WebSocket正在连接，先尝试获取游戏状态，roomId:', roomId);
        this.fetchGameState(roomId);
      }
    } else {
      // 如果没有roomId，提示用户并跳转回房间页面
      console.warn('游戏页面：未找到房间ID');
      wx.showToast({
        title: '未找到房间信息',
        icon: 'none',
        duration: 2000
      });
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/room/room'
        });
      }, 2000);
    }
  },

  onUnload() {
    // 清理所有定时器
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer);
    }
    if (this.data.selectRowTimer) {
      clearInterval(this.data.selectRowTimer);
    }
    if (this._cardAnimationTimer) {
      clearTimeout(this._cardAnimationTimer);
    }
    // 注意：小程序不支持 wx.offSocketOpen 等API，只需要关闭socket即可
    if (socketOpen) {
      wx.closeSocket();
    }
    socketOpen = false;
    socketMsgQueue = [];
  },

  connectWebSocket(roomId) {
    if (!roomId) {
      console.error('connectWebSocket: roomId为空');
      return;
    }
    
    // 防重复连接：onLoad/onShow/热重载可能触发多次
    if (socketConnecting) {
      console.log('connectWebSocket: 正在连接中，跳过重复调用');
      return;
    }
    socketConnecting = true;

    // 如果已经连接，先关闭
    if (socketOpen) {
      console.log('关闭现有WebSocket连接');
      wx.closeSocket();
      socketOpen = false;
      this.setData({ wsConnected: false });
    }
    
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    if (!userInfo) {
      console.error('connectWebSocket: 用户信息为空');
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      });
      return;
    }
    
    // 使用用户ID作为userIdentifier
    const userIdentifier = userInfo.id || userInfo.username || userInfo.nickname;
    if (!userIdentifier) {
      console.error('connectWebSocket: userIdentifier为空');
      return;
    }
    
    const wsUrl = `${app.globalData.wsUrl}?roomId=${roomId}&userIdentifier=${encodeURIComponent(userIdentifier)}`;
    console.log('连接WebSocket:', wsUrl);
    
    // 先尝试获取一次游戏状态（不等待WebSocket）
    console.log('========== 连接WebSocket前，先尝试获取游戏状态 ==========');
    console.log('roomId:', roomId);
    console.log('this.fetchGameState:', typeof this.fetchGameState);
    if (typeof this.fetchGameState === 'function') {
      console.log('准备调用 this.fetchGameState');
      this.fetchGameState(roomId);
    } else {
      console.error('this.fetchGameState 不是函数！');
    }
    
    wx.connectSocket({ url: wsUrl });

    // 注意：wx.onSocketOpen 可能会被多次调用，但每次连接只会触发一次
    wx.onSocketOpen(() => {
      console.log('========== WebSocket连接成功 ==========');
      console.log('roomId:', roomId);
      socketOpen = true;
      socketConnecting = false;
      this.setData({ wsConnected: true });
      
      // 发送加入房间消息
      this.sendSocketMsg({ type: 'joinRoom', roomId, userIdentifier });
      
      // 发送队列中的消息
      while (socketMsgQueue.length > 0) {
        wx.sendSocketMessage({ data: socketMsgQueue.shift() });
      }
      
      // 连接成功后，再次获取当前游戏状态（确保数据是最新的）
      console.log('WebSocket连接成功，准备调用 fetchGameState，roomId:', roomId);
      setTimeout(() => {
        this.fetchGameState(roomId);
      }, 500); // 延迟500ms，确保joinRoom消息已发送并服务器已响应
    });

    wx.onSocketMessage((res) => {
      let msg;
      try {
        msg = JSON.parse(res.data);
      } catch (e) {
        console.error('消息解析失败:', res.data);
        return;
      }
      if (msg.type === 'gameStateUpdate' || msg.type === 'roomStateUpdate') {
        // 兼容两种消息类型
        const roomState = msg.roomState || msg.data;
        this.handleRoomUpdate(roomState);
      } else if (msg.type === 'chat') {
        this.handleChatMessage(msg.data || msg);
      } else if (msg.type === 'error') {
        // 服务端错误时，避免前端一直"出牌中"
        this.setData({ isProcessing: false });
        if (this._playTimeoutTimer) {
          clearTimeout(this._playTimeoutTimer);
          this._playTimeoutTimer = null;
        }
        wx.showToast({ title: msg.data || msg.message || '发生错误', icon: 'none' });
      } else if (msg.type === 'selectRow' || msg.type === 'needSelectRow') {
        // 服务器通知需要选择收牌行
        console.log('[GAME] 收到选择收牌行通知:', msg);
        this.showSelectRowDialog();
      } else if (msg.type === 'rowSelected') {
        // 服务器确认收牌行已选择
        console.log('[GAME] 收牌行已选择:', msg);
        wx.showToast({
          title: '收牌成功',
          icon: 'success'
        });
      } else if (msg.type === 'cardsPlaced' || msg.type === 'roundSettlement') {
        // 服务器通知卡牌已放置到场牌，触发移动动画
        console.log('[GAME] 收到卡牌放置通知，开始移动动画');
        this.startCardMoveAnimation();
      }
    });

    wx.onSocketClose(() => {
      socketOpen = false;
      socketConnecting = false;
      this.setData({ wsConnected: false });
    });

    wx.onSocketError((err) => {
      socketOpen = false;
      socketConnecting = false;
      this.setData({ wsConnected: false });
      wx.showToast({ title: 'WebSocket连接失败', icon: 'none' });
    });
  },

  sendSocketMsg(data) {
    const msg = typeof data === 'string' ? data : JSON.stringify(data);
    if (socketOpen) {
      wx.sendSocketMessage({ data: msg });
    } else {
      socketMsgQueue.push(msg);
    }
  },

  /**
   * 主动获取游戏状态（用于断线重连或首次加载）
   */
  fetchGameState(roomId) {
    if (!roomId) {
      console.warn('fetchGameState: roomId为空');
      return;
    }

    const now = Date.now();
    if (this._fetchingGameState) {
      console.log('fetchGameState: 上一次请求尚未结束，跳过');
      return;
    }
    if (now - (this._lastFetchGameStateAt || 0) < 800) {
      console.log('fetchGameState: 调用过于频繁，跳过');
      return;
    }
    this._fetchingGameState = true;
    this._lastFetchGameStateAt = now;
    
    console.log('========== 开始调用 fetchGameState ==========');
    console.log('roomId:', roomId);
    console.log('gameApi:', gameApi);
    console.log('gameApi.getGameState:', gameApi.getGameState);
    
    if (!gameApi || !gameApi.getGameState) {
      console.error('gameApi.getGameState 不存在！');
      return;
    }
    
    console.log('调用 gameApi.getGameState，roomId:', roomId);
    gameApi.getGameState(roomId)
      .then((gameState) => {
        console.log('========== 获取到游戏状态 ==========');
        console.log('gameState:', gameState);
        if (gameState) {
          // 格式化数据并更新页面
          console.log('准备更新页面，gameState:', gameState);
          this.handleRoomUpdate(gameState);
        } else {
          console.warn('获取到的游戏状态为空');
        }
      })
      .catch((err) => {
        console.error('========== 获取游戏状态失败 ==========');
        console.error('错误信息:', err);
        console.error('错误堆栈:', err.stack);
        // 如果API调用失败，等待WebSocket推送
        wx.showToast({
          title: '获取游戏状态失败，等待服务器推送',
          icon: 'none',
          duration: 2000
        });
      })
      .finally(() => {
        this._fetchingGameState = false;
      });
  },

  /**
   * 获取一个可靠的 roomId（switchTab 无参数时兜底）
   */
  getEffectiveRoomId() {
    const fromData = this.data.roomId;
    const fromRoomInfo = this.data.roomInfo && (this.data.roomInfo.roomId || this.data.roomInfo.id);
    const globalRoom = app.globalData.currentRoom;
    const fromGlobal = globalRoom && (globalRoom.roomId || globalRoom.id);
    const storedRoom = wx.getStorageSync('currentRoom');
    const fromStorage = storedRoom && (storedRoom.roomId || storedRoom.id);
    return fromData || fromRoomInfo || fromGlobal || fromStorage || null;
  },

  handleRoomUpdate(roomState) {
    if (!roomState) return;
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const userId = userInfo.id; // 使用用户ID来匹配
    const players = Object.values(roomState.players || {});
    
    // 改进玩家匹配逻辑，支持多种ID格式
    const currentPlayer = players.find(p => {
      // 优先使用ID匹配（支持字符串和数字类型转换）
      if (p.id == userId || p.userId == userId || String(p.id) === String(userId) || String(p.userId) === String(userId)) {
        return true;
      }
      // 如果没有ID，使用sessionId匹配
      if (p.sessionId && userInfo.sessionId && p.sessionId === userInfo.sessionId) {
        return true;
      }
      // 最后使用昵称匹配（兼容旧数据）
      if ((!p.id && !p.userId) && (p.displayName === userInfo.nickname || p.nickname === userInfo.nickname)) {
        return true;
      }
      return false;
    });
    
    // 格式化玩家信息
    // 获取已出牌信息
    const playedCards = roomState.playedCardsThisTurn || {};
    
    // 判断所有玩家是否都出完牌
    const allPlayersPlayed = players.length > 0 && players.every(p => p.hasPlayed);
    
    console.log('[GAME] 所有玩家出牌状态:', {
      totalPlayers: players.length,
      playedCount: players.filter(p => p.hasPlayed).length,
      allPlayersPlayed: allPlayersPlayed
    });
    
    const updatedPlayers = players.map(player => {
      let status = 'thinking';
      let statusText = '思考中';
      let playedCard = null;
      
      // 查找该玩家已出的牌
      const playerId = player.id || player.userId || player.sessionId;
      if (playedCards[playerId]) {
        playedCard = {
          number: playedCards[playerId].cardNumber || playedCards[playerId].number,
          bullheads: playedCards[playerId].bullheads || 0
        };
      }
      
      if (player.hasPlayed) {
        status = 'played';
        statusText = '已出牌';
      } else if (player.isCurrentTurn) {
        status = 'thinking';
        statusText = '思考中';
      }
      if (player.id === userId || player.userId === userId || (player.displayName === userInfo.nickname && !player.id)) {
        if (this.data.selectedCard) {
          status = 'locked';
          statusText = '已锁定';
        }
      }
      return {
        ...player,
        displayName: player.displayName || player.nickname,
        avatarUrl: player.avatarUrl || '/images/default-avatar.png',
        status: status,
        statusText: statusText,
        playedCard: playedCard,
        hasPlayed: player.hasPlayed,
        isAnimating: false,
        animationStyle: ''
      };
    });
    
    // 计算空位（总共10个位置 - 当前玩家数）
    const emptySlots = Array.from({ length: Math.max(0, 10 - updatedPlayers.length) }, (_, i) => ({ index: i }));

    // 调试日志：检查当前玩家和手牌
    console.log('[GAME] handleRoomUpdate - 当前玩家信息:', {
      currentPlayer: currentPlayer ? {
        id: currentPlayer.id,
        userId: currentPlayer.userId,
        nickname: currentPlayer.nickname,
        hasPlayed: currentPlayer.hasPlayed,
        hand: currentPlayer.hand,
        handCount: currentPlayer.hand ? currentPlayer.hand.length : 0
      } : null,
      userId: userId
    });
    
    // 本游戏为"同时出牌"机制：每回合所有玩家同时选牌并确认
    // 因此不能用 isCurrentTurn 控制出牌权限；改为"未出牌且未托管即可出牌"
    const canPlay = roomState.gameState === 'PLAYING'
      && !(currentPlayer && currentPlayer.hasPlayed)
      && !(currentPlayer && currentPlayer.is托管);
    const playerHand = (currentPlayer && currentPlayer.hand || []).sort((a, b) => a.number - b.number);
    
    console.log('[GAME] 手牌处理结果:', {
      playerHand: playerHand,
      handCount: playerHand.length,
      canPlay: canPlay
    });
    
    // 格式化场牌（每行最多6张，第5张高亮）
    const rows = (roomState.rows || []).map((row, index) => {
      const cards = (row.cards || []).slice(0, 6); // 最多只显示6张
      const totalBullheads = cards.reduce((sum, card) => sum + (card.bullheads || 0), 0);
      const isDanger = cards.length >= 5;
      return {
        cards: cards.map((card, cardIndex) => ({
          ...card,
          isHead: cardIndex === 0,
          isFifth: cardIndex === 4, // 第5张（索引4）
          isDanger: isDanger && cardIndex === cards.length - 1
        })),
        totalBullheads: totalBullheads,
        isDanger: isDanger
      };
    });
    
    console.log('[GAME] 场牌处理结果:', {
      serverRows: roomState.rows,
      formattedRows: rows,
      rowsCount: rows.length
    });

    // 格式化其他玩家分数（排除当前玩家）
    const otherScores = updatedPlayers
      .filter(p => {
        const pId = p.id || p.userId;
        const pSessionId = p.sessionId;
        // 排除当前玩家
        if (pId == userId || String(pId) === String(userId)) return false;
        if (pSessionId && userInfo.sessionId && pSessionId === userInfo.sessionId) return false;
        if ((!p.id && !p.userId) && (p.displayName === userInfo.nickname || p.nickname === userInfo.nickname)) return false;
        return true;
      })
      .map(p => `${p.displayName || p.nickname}${p.score || 0}`)
      .join('·');

    // 格式化已亮牌（不再触发中心弹窗动画）
    const revealedCards = Object.values(playedCards);
    const sortedCards = revealedCards.length > 0 
      ? revealedCards.map(c => c.cardNumber || c.number).sort((a, b) => a - b).join(', ')
      : '';

    // 手牌保护：如果服务器返回的手牌为空，但本地还有手牌，保留本地手牌（防止出牌过程中闪烁）
    const shouldUpdateHand = playerHand.length > 0 || this.data.playerHand.length === 0;
    const finalPlayerHand = shouldUpdateHand ? playerHand : this.data.playerHand;
    const finalHandCount = shouldUpdateHand ? playerHand.length : this.data.handCount;
    
    // 场牌保护：如果服务器返回的场牌为空，但本地还有场牌，保留本地场牌
    const shouldUpdateRows = rows.length > 0 || this.data.rows.length === 0;
    const finalRows = shouldUpdateRows ? rows : this.data.rows;
    
    console.log('[GAME] 数据更新策略:', {
      serverHand: playerHand.length,
      localHand: this.data.playerHand.length,
      shouldUpdateHand: shouldUpdateHand,
      finalHand: finalPlayerHand.length,
      serverRows: rows.length,
      localRows: this.data.rows.length,
      shouldUpdateRows: shouldUpdateRows,
      finalRows: finalRows.length
    });
    
    this.setData({
      roomInfo: roomState,
      // 确保 roomId 不会丢（后续 WS/出牌依赖）
      roomId: roomState.roomId || this.data.roomId,
      players: updatedPlayers,
      playerHand: finalPlayerHand,
      handCount: finalHandCount,
      canPlay: canPlay,
      isProcessing: false,
      currentRound: roomState.currentRound || 1,
      currentTurn: roomState.currentTurnNumber || 1,
      myScore: currentPlayer ? (currentPlayer.score || 0) : 0,
      otherScores: otherScores,
      rows: finalRows,
      revealedCards: revealedCards,
      sortedCards: sortedCards,
      allPlayersPlayed: allPlayersPlayed,
      emptySlots: emptySlots
    });
    
    // 如果所有玩家都出完牌，触发翻牌动画（延迟500ms）
    if (allPlayersPlayed && !this.data.allPlayersPlayed) {
      console.log('[GAME] 所有玩家出牌完成，准备翻牌动画');
      setTimeout(() => {
        this.startCardFlipAnimation();
      }, 500);
    }

    if (this._playTimeoutTimer) {
      clearTimeout(this._playTimeoutTimer);
      this._playTimeoutTimer = null;
    }

    // 启动倒计时
    if (roomState.gameState === 'PLAYING' && roomState.countdown) {
      this.startCountdown(roomState.countdown);
    }

    if (roomState.gameState === 'GAME_OVER' || roomState.gameState === 'FINISHED') {
      console.log('[GAME] 游戏结束，准备跳转到结算页面');
      
      // 保存游戏结果到全局
      const gameResult = {
        roomId: this.data.roomId,
        roomName: roomState.roomName || '未命名房间',
        currentRound: roomState.currentRound || 1,
        maxRounds: roomState.maxRounds || 10,
        remainingRounds: (roomState.maxRounds || 10) - (roomState.currentRound || 1),
        targetScore: roomState.targetScore || 66,
        players: roomState.players || {},
        rankings: updatedPlayers,
        isGameOver: true
      };
      
      app.globalData.gameResult = gameResult;
      wx.setStorageSync('gameResult', gameResult);
      
      console.log('[GAME] 已保存游戏结果:', gameResult);
      
      // 延迟跳转，确保数据已保存
      setTimeout(() => {
        wx.redirectTo({
          url: `/pages/result/result?roomId=${this.data.roomId}&isGameOver=true`
        });
      }, 300);
    } else if (roomState.gameState === 'ROUND_END') {
      // 单局结束（非游戏彻底结束）
      console.log('[GAME] 单局结束，准备跳转到结算页面');
      
      const gameResult = {
        roomId: this.data.roomId,
        roomName: roomState.roomName || '未命名房间',
        currentRound: roomState.currentRound || 1,
        maxRounds: roomState.maxRounds || 10,
        remainingRounds: (roomState.maxRounds || 10) - (roomState.currentRound || 1),
        targetScore: roomState.targetScore || 66,
        players: roomState.players || {},
        rankings: updatedPlayers,
        isGameOver: false
      };
      
      app.globalData.gameResult = gameResult;
      wx.setStorageSync('gameResult', gameResult);
      
      setTimeout(() => {
        wx.redirectTo({
          url: `/pages/result/result?roomId=${this.data.roomId}&isGameOver=false`
        });
      }, 300);
    }
  },

  /**
   * 启动倒计时
   */
  startCountdown(seconds) {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer);
    }
    this.setData({ countdown: seconds });
    this.data.countdownTimer = setInterval(() => {
      const countdown = this.data.countdown - 1;
      if (countdown <= 0) {
        clearInterval(this.data.countdownTimer);
        this.setData({ countdown: 0 });
      } else {
        this.setData({ countdown: countdown });
      }
    }, 1000);
  },

  handleChatMessage(message) {
    const messages = [...this.data.chatMessages, message];
    this.setData({
      chatMessages: messages,
      lastMessageId: `msg-${message.id}`
    });
  },

  selectCard(e) {
    const cardNumber = e.currentTarget.dataset.cardNumber;
    this.setData({ selectedCard: cardNumber });
  },

  /**
   * 取消选择
   */
  handleCancelSelect() {
    this.setData({ selectedCard: null });
  },

  /**
   * 切换托管状态
   */
  handleToggleHosting() {
    const newHostingState = !this.data.isHosting;
    this.setData({ isHosting: newHostingState });
    
    // 通过WebSocket通知服务器托管状态变化
    const roomId = this.getEffectiveRoomId();
    if (roomId && (this.data.wsConnected || socketOpen)) {
      const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
      const userIdentifier = userInfo && (userInfo.id || userInfo.username || userInfo.nickname);
      
      this.sendSocketMsg({
        type: 'toggleHosting',
        roomId: String(roomId),
        isHosting: newHostingState,
        userIdentifier
      });
      
      wx.showToast({
        title: newHostingState ? '已开启托管' : '已取消托管',
        icon: 'success',
        duration: 1500
      });
    } else {
      wx.showToast({
        title: '连接中，请稍后重试',
        icon: 'none'
      });
      // 回滚状态
      this.setData({ isHosting: !newHostingState });
    }
  },

  /**
   * 设置
   */
  handleSettings() {
    // 可以显示设置弹窗
    wx.showActionSheet({
      itemList: ['退出游戏'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.handleLeaveRoom();
        }
      }
    });
  },

  handlePlayCard() {
    if (!this.data.selectedCard || this.data.isProcessing) return;
    if (!this.data.canPlay) {
      wx.showToast({
        title: '当前回合不可出牌（可能已出牌或处于托管）',
        icon: 'none'
      });
      return;
    }

    const roomId = this.getEffectiveRoomId();
    if (!roomId) {
      wx.showToast({ title: '缺少 roomId，无法出牌', icon: 'none' });
      return;
    }

    // 若 WS 尚未连接，不进入“出牌中”
    if (!socketOpen || !this.data.wsConnected) {
      wx.showToast({ title: '连接中，请稍后重试', icon: 'none' });
      this.connectWebSocket(roomId);
      return;
    }

    this.setData({ isProcessing: true, roomId });
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const userIdentifier = userInfo && (userInfo.id || userInfo.username || userInfo.nickname);
    // 兼容后端两种解析方式：
    // 1) 读取顶层字段 roomId/cardNumber
    // 2) 读取 data 负载（部分服务端会要求 msg.data 存在）
    const payload = {
      type: 'playCard',
      roomId: String(roomId),
      // 兼容后端错误字段名（roomld / rootid），后端修复后也不影响
      roomld: String(roomId),
      rootid: String(roomId),
      cardNumber: this.data.selectedCard,
      userIdentifier,
      data: {
        roomId: String(roomId),
        roomld: String(roomId),
        rootid: String(roomId),
        cardNumber: this.data.selectedCard,
        userIdentifier
      }
    };

    console.log('[PLAY] sendSocketMsg payload:', payload);
    this.sendSocketMsg(payload);

    // 超时兜底：服务端没推送也不至于永远“出牌中”
    if (this._playTimeoutTimer) clearTimeout(this._playTimeoutTimer);
    this._playTimeoutTimer = setTimeout(() => {
      if (this.data.isProcessing) {
        this.setData({ isProcessing: false });
        wx.showToast({ title: '出牌超时，请重试', icon: 'none' });
      }
    }, 6000);
  },

  handleReady() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const userIdentifier = userInfo && (userInfo.id || userInfo.username || userInfo.nickname);
    this.sendSocketMsg({ type: 'playerReady', roomId: this.data.roomId, userIdentifier });
  },

  handleStartGame() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const userIdentifier = userInfo && (userInfo.id || userInfo.username || userInfo.nickname);
    this.sendSocketMsg({ type: 'startGame', roomId: this.data.roomId, userIdentifier });
  },

  handleLeaveRoom() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const userIdentifier = userInfo && (userInfo.id || userInfo.username || userInfo.nickname);
    wx.showModal({
      title: '提示',
      content: '确定要离开游戏吗？',
      success: (res) => {
        if (res.confirm) {
          this.sendSocketMsg({ type: 'leaveRoom', roomId: this.data.roomId, userIdentifier });
          wx.closeSocket();
          wx.navigateBack();
        }
      }
    });
  },

  onChatInput(e) {
    this.setData({ chatInput: e.detail.value });
  },

  sendChatMessage() {
    const { chatInput } = this.data;
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const userIdentifier = userInfo && (userInfo.id || userInfo.username || userInfo.nickname);
    if (!chatInput.trim()) return;
    this.sendSocketMsg({
      type: 'chat',
      roomId: this.data.roomId,
      data: {
        text: chatInput.trim(),
        sender: userIdentifier
      },
      userIdentifier
    });
    this.setData({ chatInput: '' });
  },

  /**
   * 翻牌动画：所有玩家出完牌后，显示正面（无需翻转动画，直接显示）
   */
  startCardFlipAnimation() {
    console.log('[GAME] 开始显示卡牌正面');
    // 直接更新状态，WXML会根据 allPlayersPlayed 自动显示正面
    // 无需额外处理，已在 WXML 中通过 wx:if="{{allPlayersPlayed}}" 实现
  },

  /**
   * 卡牌移动动画：从玩家位置移动到场牌区域
   * 这个函数会在服务器推送 'cardsPlaced' 消息时调用
   */
  startCardMoveAnimation() {
    console.log('[GAME] 开始卡牌移动动画');
    
    const players = this.data.players;
    const updatedPlayers = players.map(player => {
      if (player.playedCard) {
        return {
          ...player,
          isAnimating: true
        };
      }
      return player;
    });
    
    this.setData({
      players: updatedPlayers
    });
    
    // 2秒后清除动画状态并清空卡牌
    setTimeout(() => {
      const finalPlayers = this.data.players.map(player => ({
        ...player,
        isAnimating: false,
        playedCard: null
      }));
      
      this.setData({
        players: finalPlayers,
        allPlayersPlayed: false
      });
      
      console.log('[GAME] 卡牌移动动画完成，已清空玩家卡牌');
    }, 2000);
  },

  /**
   * 出牌动画：按数字从小到大依次显示
   * @param {Array} cards - 已出的牌 [{cardNumber, bullheads, playerName}]
   */
  playCardsAnimation(cards) {
    if (!cards || cards.length === 0) return;
    
    // 按数字从小到大排序
    const sortedCards = cards.map(c => ({
      cardNumber: c.cardNumber || c.number,
      bullheads: c.bullheads || 0,
      playerName: c.playerName || c.displayName || '玩家'
    })).sort((a, b) => a.cardNumber - b.cardNumber);
    
    console.log('[GAME] 开始播放出牌动画，排序后:', sortedCards);
    
    // 清空当前动画
    this.setData({
      playingCards: [],
      animatingCards: []
    });
    
    // 依次显示每张牌，间隔500ms
    let currentIndex = 0;
    const showNextCard = () => {
      if (currentIndex >= sortedCards.length) {
        console.log('[GAME] 出牌动画播放完成');
        // 动画播放完成后，保留3秒再清空
        setTimeout(() => {
          this.setData({
            playingCards: [],
            animatingCards: []
          });
        }, 3000);
        return;
      }
      
      const card = sortedCards[currentIndex];
      const newPlayingCards = [...this.data.playingCards, card];
      const newAnimatingCards = [...this.data.animatingCards, card.cardNumber];
      
      console.log('[GAME] 显示第', currentIndex + 1, '张牌:', card);
      
      this.setData({
        playingCards: newPlayingCards,
        animatingCards: newAnimatingCards
      });
      
      // 500ms后移除动画class，再显示下一张
      setTimeout(() => {
        this.setData({
          animatingCards: this.data.animatingCards.filter(n => n !== card.cardNumber)
        });
      }, 500);
      
      currentIndex++;
      this._cardAnimationTimer = setTimeout(showNextCard, 600);
    };
    
    showNextCard();
  },

  /**
   * 选择收牌行
   * @param {Event} e - 事件对象
   */
  handleSelectRow(e) {
    const rowIndex = e.currentTarget.dataset.rowIndex;
    this.setData({
      selectedRowIndex: rowIndex
    });
  },

  /**
   * 确认选择收牌行
   */
  handleConfirmSelectRow() {
    if (this.data.selectedRowIndex === null) {
      wx.showToast({
        title: '请选择一行',
        icon: 'none'
      });
      return;
    }
    
    console.log('[GAME] 确认选择收牌行:', this.data.selectedRowIndex);
    
    // 清除倒计时
    if (this.data.selectRowTimer) {
      clearInterval(this.data.selectRowTimer);
    }
    
    // 发送选择消息到服务器
    this.sendSocketMsg({
      type: 'selectRow',
      roomId: this.data.roomId,
      rowIndex: this.data.selectedRowIndex
    });
    
    // 关闭弹窗
    this.setData({
      showSelectRowModal: false,
      selectRowCountdown: 30,
      selectedRowIndex: null
    });
  },

  /**
   * 取消选择收牌行
   */
  handleCancelSelectRow() {
    this.setData({
      showSelectRowModal: false,
      selectedRowIndex: null
    });
  },

  /**
   * 显示选择收牌行弹窗
   */
  showSelectRowDialog() {
    console.log('[GAME] 显示选择收牌行弹窗');
    
    this.setData({
      showSelectRowModal: true,
      selectRowCountdown: 30,
      selectedRowIndex: null
    });
    
    // 开始倒计时
    if (this.data.selectRowTimer) {
      clearInterval(this.data.selectRowTimer);
    }
    
    this.data.selectRowTimer = setInterval(() => {
      const newCountdown = this.data.selectRowCountdown - 1;
      this.setData({
        selectRowCountdown: newCountdown
      });
      
      if (newCountdown <= 0) {
        // 倒计时结束，自动选择猪头数最少的行
        this.autoSelectMinBullheadsRow();
      }
    }, 1000);
  },

  /**
   * 自动选择猪头数最少的行
   */
  autoSelectMinBullheadsRow() {
    console.log('[GAME] 倒计时结束，自动选择猪头数最少的行');
    
    // 清除倒计时
    if (this.data.selectRowTimer) {
      clearInterval(this.data.selectRowTimer);
    }
    
    // 找出猪头数最少的行
    const rows = this.data.rows;
    if (rows.length === 0) {
      console.error('[GAME] 没有可选择的行');
      return;
    }
    
    let minBullheads = Infinity;
    let minRowIndex = 0;
    
    rows.forEach((row, index) => {
      if (row.totalBullheads < minBullheads) {
        minBullheads = row.totalBullheads;
        minRowIndex = index;
      }
    });
    
    console.log('[GAME] 自动选择第', minRowIndex + 1, '行，猪头数:', minBullheads);
    
    // 发送选择消息到服务器
    this.sendSocketMsg({
      type: 'selectRow',
      roomId: this.data.roomId,
      rowIndex: minRowIndex
    });
    
    // 关闭弹窗
    this.setData({
      showSelectRowModal: false,
      selectRowCountdown: 30,
      selectedRowIndex: null
    });
    
    wx.showToast({
      title: `自动选择第${minRowIndex + 1}行`,
      icon: 'none'
    });
  }
}); 
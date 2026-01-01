/**
 * 房间页面
 */
const roomApi = require('../../api/roomApi.js');
const app = getApp();

let socketOpen = false;
let socketMsgQueue = [];

Page({
  data: {
    roomId: null,
    roomInfo: {},
    players: [],
    playerSlots: [],
    activePlayers: 0,
    maxPlayers: 4,
    hostName: '',
    isHost: false,
    isMeReady: false,
    canReady: true,
    canStartGame: false,
    readyCount: 0,
    wsConnected: false,
    currentUserId: null,
    ruleText: '',
    botCountRange: [],
    chatMessages: [],
    inputValue: '',
    statusBarHeight: 20 // Default, will be updated in onLoad
  },

  onLoad(options) {
    // Get Status Bar Height for custom header
    const sysInfo = wx.getSystemInfoSync();
    this.setData({
      statusBarHeight: sysInfo.statusBarHeight
    });

    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    if (!userInfo) {
      wx.reLaunch({
        url: '/pages/login/login'
      });
      return;
    }

    this.setData({
      currentUserId: userInfo.id
    });

    // Handle direct launch with roomId (e.g. from share)
    if (options && options.roomId) {
      this.setData({ roomId: options.roomId });
      this.connectWebSocket(options.roomId);
    }

    // Note: onShow will handle other initialization scenarios (switchTab)
  },

  onShow() {
    this.checkRoomState();
  },

  onHide() {
    // Optional: Decide whether to close socket on hide.
    // Usually for game rooms, we keep it open unless user explicitly leaves.
  },

  onUnload() {
    wx.closeSocket();
    socketOpen = false;
    socketMsgQueue = [];
  },

  /**
   * Check Global Data for Room State (from Create or Join)
   */
  checkRoomState() {
    const globalRoom = app.globalData.currentRoom || wx.getStorageSync('currentRoom');
    const currentRoomId = this.data.roomId;

    // Case 1: Just created/joined a room (Global data exists)
    if (globalRoom) {
      // If we are already connected to this room, do nothing
      if (currentRoomId && currentRoomId == globalRoom.roomId) {
        return;
      }

      // Load new room
      const roomId = globalRoom.roomId || globalRoom.id;
      if (roomId) {
        this.setData({ roomId: roomId });

        // Initialize UI with preloaded data immediately
        this.renderPreloadedRoom(globalRoom);

        // Connect Socket
        this.connectWebSocket(roomId);

        // Clear global data to prevent reloading it repeatedly if we navigate away and back
        // However, for TabBar, we might want to keep it?
        // Better: We rely on WebSocket state or local data state.
        // Clearing it ensures we don't overwrite if we join a NEW room later.
        app.globalData.currentRoom = null;
        wx.removeStorageSync('currentRoom');
      }
    }
    // Case 2: No global data, but we have a roomId in data (from previous session in this tab)
    else if (currentRoomId) {
      if (!this.data.wsConnected && !socketOpen) {
         this.connectWebSocket(currentRoomId);
      }
    }
    // Case 3: No room at all
    else {
      // User clicked "Room" tab but isn't in a room.
      // We can show an empty state in WXML.
    }
  },

  /**
   * Render UI from initial room object
   */
  renderPreloadedRoom(roomData) {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');

    // Construct initial player list if missing
    let players = [];
    if (roomData.players && Array.isArray(roomData.players) && roomData.players.length > 0) {
      players = roomData.players;
    } else {
      // Synthesize the current user as a player (likely host if created, or guest if joined)
      // Note: Backend 'join' response usually includes player list. 'create' might not.
      players = [{
        id: userInfo.id,
        nickname: userInfo.nickname,
        avatarUrl: userInfo.avatarUrl,
        isHost: true, // Assumption for creation. For join, this might be wrong temporarily until WS update.
        isReady: false,
        displayName: userInfo.nickname
      }];
    }

    const roomState = {
      roomName: roomData.roomName || `${userInfo.nickname}的房间`,
      maxPlayers: roomData.maxPlayers || 6,
      maxRounds: roomData.maxRounds || 3,
      targetScore: roomData.targetScore || 66,
      currentRound: 1,
      gameState: 'WAITING',
      players: players
    };

    // Reuse the update logic
    this.handleRoomUpdate({ roomState });
  },

  /**
   * 连接 WebSocket
   */
  connectWebSocket(roomId) {
    if (socketOpen) {
       wx.closeSocket();
    }

    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    
    // 验证用户信息和用户ID
    if (!userInfo || !userInfo.id) {
      console.error('connectWebSocket: 用户信息或用户ID缺失', userInfo);
      wx.showToast({
        title: '用户信息缺失，请重新登录',
        icon: 'none',
        duration: 2000
      });
      setTimeout(() => {
        wx.reLaunch({
          url: '/pages/login/login'
        });
      }, 2000);
      return;
    }
    
    // 使用用户ID作为userIdentifier（必须是数字）
    const userIdentifier = userInfo.id;
    const wsUrl = `${app.globalData.wsUrl}?roomId=${roomId}&userIdentifier=${userIdentifier}`;

    wx.connectSocket({ url: wsUrl });

    wx.onSocketOpen(() => {
      console.log('WebSocket连接已打开');
      socketOpen = true;
      this.setData({ wsConnected: true });

      // 发送加入房间消息
      const joinMsg = { type: 'joinRoom', roomId, userIdentifier };
      this.sendSocketMsg(joinMsg);

      // 发送队列中的消息
      while (socketMsgQueue.length > 0) {
        wx.sendSocketMessage({ data: socketMsgQueue.shift() });
      }
    });

    wx.onSocketMessage((res) => {
      console.log('收到WebSocket消息:', res.data);
      let msg;
      try {
        msg = JSON.parse(res.data);
      } catch (e) {
        console.error('消息解析失败:', res.data);
        return;
      }

      if (msg.type === 'gameStateUpdate') {
        this.handleRoomUpdate(msg);
      } else if (msg.type === 'chat' || msg.type === 'chatMessage') {
        this.handleNewChatMessage(msg);
      } else if (msg.type === 'error') {
        wx.showToast({ title: msg.message || msg.data || '发生错误', icon: 'none' });
      }
    });

    wx.onSocketClose(() => {
      console.log('WebSocket连接已关闭');
      socketOpen = false;
      this.setData({ wsConnected: false });
    });

    wx.onSocketError((err) => {
      console.error('WebSocket连接错误:', err);
      socketOpen = false;
      this.setData({ wsConnected: false });
      // Don't show toast immediately to avoid spam on generic network issues,
      // but here it's fine for debugging.
    });
  },

  /**
   * 发送 WebSocket 消息
   */
  sendSocketMsg(data) {
    const msg = typeof data === 'string' ? data : JSON.stringify(data);
    if (socketOpen) {
      wx.sendSocketMessage({ data: msg });
    } else {
      socketMsgQueue.push(msg);
    }
  },

  /**
   * 处理房间状态更新
   */
  handleRoomUpdate(msg) {
    const roomState = msg.roomState;
    if (!roomState) {
      return;
    }

    const playersObject = roomState.players || {};
    const players = Object.values(playersObject);
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const userId = userInfo.id; // 使用用户ID来匹配

    // 找到房主和当前玩家
    // 如果后端没有返回isHost，则第一个玩家视为房主
    let hostPlayer = players.find(p => p.isHost === true);
    if (!hostPlayer && players.length > 0) {
      // 如果没有明确的房主标记，第一个玩家视为房主
      hostPlayer = players[0];
      // 标记第一个玩家为房主（用于前端显示）
      if (players[0]) {
        players[0].isHost = true;
        hostPlayer = players[0];
      }
    }
    
    const currentPlayer = players.find(p => {
      // 优先使用ID匹配（支持字符串和数字类型转换）
      if (p.id == userId || p.userId == userId || String(p.id) === String(userId) || String(p.userId) === String(userId)) {
        return true;
      }
      // 如果没有ID，使用sessionId匹配（如果后端返回了sessionId）
      if (p.sessionId && userInfo.sessionId && p.sessionId === userInfo.sessionId) {
        return true;
      }
      // 最后使用昵称匹配（兼容旧数据）
      if ((!p.id && !p.userId) && (p.displayName === userInfo.nickname || p.nickname === userInfo.nickname)) {
        return true;
      }
      return false;
    });
    
    // 调试日志：检查 currentPlayer 是否找到
    if (!currentPlayer) {
      console.warn('未找到当前玩家:', {
        userId: userId,
        players: players.map(p => ({
          id: p.id,
          userId: p.userId,
          sessionId: p.sessionId,
          nickname: p.nickname,
          displayName: p.displayName
        }))
      });
    }

    // 判断是否为房主：直接比较当前玩家和房主是否为同一人
    // 优先使用后端返回的 isHost 字段，如果没有则判断是否为第一个玩家
    let isHost = false;
    if (currentPlayer) {
      // 方法1：如果当前玩家明确标记为房主
      if (currentPlayer.isHost === true) {
        isHost = true;
      }
      // 方法2：如果当前玩家是房主（通过比较ID或其他标识）
      else if (hostPlayer) {
        // 比较多种可能的标识符
        const currentId = currentPlayer.id || currentPlayer.userId || currentPlayer.sessionId;
        const hostId = hostPlayer.id || hostPlayer.userId || hostPlayer.sessionId;
        if (currentId && hostId && currentId === hostId) {
          isHost = true;
        }
        // 或者直接比较对象引用（如果是同一个对象）
        else if (currentPlayer === hostPlayer) {
          isHost = true;
        }
      }
      // 方法3：如果后端没有返回 isHost，且当前玩家是第一个玩家，则视为房主
      else if (players.length > 0 && players[0] === currentPlayer && currentPlayer.isHost !== false) {
        isHost = true;
      }
    }
    
    // 调试日志（生产环境可删除）
    console.log('isHost判断:', {
      currentPlayer: currentPlayer ? {
        id: currentPlayer.id,
        userId: currentPlayer.userId,
        sessionId: currentPlayer.sessionId,
        isHost: currentPlayer.isHost
      } : null,
      hostPlayer: hostPlayer ? {
        id: hostPlayer.id,
        userId: hostPlayer.userId,
        sessionId: hostPlayer.sessionId,
        isHost: hostPlayer.isHost
      } : null,
      isHost: isHost
    });
    
    // 房主默认已准备（如果后端没有返回 isReady，则默认为 true）
    // 其他玩家需要明确设置 isReady 为 true 才算已准备
    let isMeReady = false;
    if (currentPlayer) {
      if (currentPlayer.isReady !== undefined) {
        isMeReady = currentPlayer.isReady;
      } else {
        // 如果后端没有返回准备状态，房主默认为已准备，其他玩家默认为未准备
        isMeReady = isHost ? true : false;
      }
    }
    
    // 计算准备状态：房主默认已准备，其他玩家需要明确 isReady === true
    // 先标记房主（如果后端没有返回）
    const playersWithHost = players.map((p, index) => {
      if (p.isHost === true) return p;
      if (index === 0 && p.isHost !== false) {
        return { ...p, isHost: true };
      }
      return p;
    });
    
    const readyCount = playersWithHost.filter(p => {
      if (p.isReady === true) return true;
      if (p.isReady === undefined && p.isHost === true) return true; // 房主默认已准备
      return false;
    }).length;

    // Game can start if at least 2 players and ALL current players are ready
    // 房主默认已准备，机器人默认已准备，其他玩家需要明确 isReady === true
    const canStartGame = playersWithHost.length >= 2 && playersWithHost.every(p => {
      if (p.isReady === true) return true;
      if (p.isReady === undefined && (p.isHost === true || p.isRobot === true)) return true; // 房主和机器人默认已准备
      return false;
    });

    // 构建玩家槽位
    const maxPlayers = roomState.maxPlayers || 4;
    const playerSlots = [];
    for (let i = 0; i < maxPlayers; i++) {
      if (i < players.length) {
        const player = players[i];
        const isMe = (player.id === userId || player.userId === userId || (player.displayName === userInfo.nickname && !player.id));
        // 判断是否为房主：如果后端返回了isHost，使用后端数据；否则第一个玩家视为房主
        const playerIsHost = player.isHost === true || (i === 0 && player.isHost !== false && players.length > 0);
        // 房主默认已准备（如果后端没有返回isReady）
        const playerIsReady = player.isReady !== undefined ? player.isReady : (playerIsHost ? true : false);
        
        playerSlots.push({
          player: {
            ...player,
            displayName: player.displayName || player.nickname,
            avatarUrl: player.avatarUrl || '/images/default-avatar.png',
            isHost: playerIsHost, // 确保isHost字段存在
            isReady: playerIsReady // 确保isReady字段存在
          },
          isMe: isMe,
          isHost: playerIsHost
        });
      } else {
        playerSlots.push({ player: null });
      }
    }

    // Build Rule Text
    const maxRounds = roomState.maxRounds || 3;
    const targetScore = roomState.targetScore || 66;
    const ruleText = `${maxPlayers}人 · 最多 ${maxRounds} 局 · 超过 ${targetScore} 分游戏结束`;

    // Calculate bot count range (转换为字符串数组，供picker使用)
    const availableSlots = maxPlayers - players.length;
    const botCountRange = [];
    for (let i = 1; i <= availableSlots; i++) {
      botCountRange.push(String(i)); // 转换为字符串
    }

    this.setData({
      roomInfo: {
        roomName: roomState.roomName || '好友局',
        maxPlayers: maxPlayers,
        maxRounds: maxRounds,
        targetScore: targetScore,
        currentRound: roomState.currentRound || 1
      },
      players: players,
      playerSlots: playerSlots,
      activePlayers: players.length,
      maxPlayers: maxPlayers,
      hostName: hostPlayer ? (hostPlayer.displayName || hostPlayer.nickname) : '未知',
      isHost: isHost,
      isMeReady: isMeReady,
      canReady: roomState.gameState === 'WAITING',
      canStartGame: canStartGame,
      readyCount: readyCount,
      ruleText: ruleText,
      botCountRange: botCountRange
    });

    // 如果游戏状态变为 PLAYING，跳转到游戏页面
    if (roomState.gameState === 'PLAYING') {
      // 保存房间ID和游戏状态到全局数据和存储，因为switchTab无法传递参数
      // 优先使用roomState中的roomId，其次使用this.data.roomId
      const roomId = roomState.roomId || this.data.roomId;
      
      console.log('游戏开始，准备跳转，roomId:', roomId, 'roomState.roomId:', roomState.roomId, 'this.data.roomId:', this.data.roomId);
      
      if (roomId) {
        // 保存完整的游戏状态，以便游戏页面加载时能立即显示
        const gameStateData = {
          roomId: roomId,
          id: roomId,
          ...roomState,
          // 保留原有的房间信息
          ...app.globalData.currentRoom
        };
        
        // 保存到全局数据
        app.globalData.currentRoom = gameStateData;
        app.globalData.gameState = roomState; // 单独保存游戏状态
        
        // 保存到存储
        wx.setStorageSync('currentRoom', gameStateData);
        wx.setStorageSync('gameState', roomState);
        
        console.log('已保存房间信息和游戏状态到全局数据和存储');
        
        // 更新本地roomId（如果还没有设置）
        if (!this.data.roomId) {
          this.setData({ roomId: roomId });
        }
        
        // 切到对局页前，主动关闭房间页的 WebSocket
        // 否则对局页再 connectSocket 可能会遇到“底层连接已存在但 onSocketOpen 不触发/消息被旧页面消费”的问题
        try {
          wx.closeSocket();
        } catch (e) {
          console.warn('关闭房间页socket失败(可忽略):', e);
        }
        socketOpen = false;
        socketMsgQueue = [];
        this.setData({ wsConnected: false });

        setTimeout(() => {
          wx.switchTab({
            url: '/pages/game/game',
            success: () => {
              console.log('游戏开始，已跳转到游戏页面，roomId:', roomId);
            },
            fail: (err) => {
              console.error('跳转到游戏页面失败:', err);
              wx.showToast({
                title: '跳转失败，请手动切换到对局页面',
                icon: 'none',
                duration: 2000
              });
            }
          });
        }, 500); // 延迟500ms，确保状态更新完成
      } else {
        console.error('游戏开始但未找到roomId，无法跳转');
        wx.showToast({
          title: '房间ID缺失，无法跳转',
          icon: 'none',
          duration: 2000
        });
      }
    }
  },

  /**
   * 添加机器人 (Picker Change)
   */
  onBotCountChange(e) {
    console.log('onBotCountChange triggered:', e.detail);
    

    const index = parseInt(e.detail.value);
    const botCountRange = this.data.botCountRange || [];
    
    console.log('botCountRange:', botCountRange, 'index:', index);
    
    if (isNaN(index) || index < 0) {
      console.error('无效的选择索引:', index);
      return;
    }
    
    if (index >= botCountRange.length) {
      console.error('索引超出范围:', index, '范围:', botCountRange.length);
      return;
    }
    
    const countStr = botCountRange[index];
    const count = parseInt(countStr);
    
    console.log('选择的机器人数量:', count);
    
    if (count && count > 0) {
      this.confirmAddBots(count);
    } else {
      console.error('无效的机器人数量:', count);
      wx.showToast({
        title: '请选择有效的数量',
        icon: 'none'
      });
    }
  },

  /**
   * 确认添加机器人
   */
  confirmAddBots(count) {
    if (!count || count <= 0) {
      wx.showToast({
        title: '请选择有效的数量',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({ title: '添加中...' });

    roomApi.addBots(this.data.roomId, count)
      .then((responseData) => {
        wx.hideLoading();
        wx.showToast({ 
          title: `已添加 ${count} 个AI`, 
          icon: 'success',
          duration: 2000
        });

        // 使用返回的数据更新玩家列表
        if (responseData && responseData.players && Array.isArray(responseData.players)) {
          console.log('收到添加机器人后的玩家列表:', responseData.players);
          
          // 将玩家数组转换为对象格式（以sessionId为key）
          const playersObject = {};
          responseData.players.forEach(player => {
            const key = player.sessionId || player.id || `player_${Date.now()}_${Math.random()}`;
            const isRobot = player.sessionId && player.sessionId.startsWith('BOT_');
            
            playersObject[key] = {
              ...player,
              id: player.userId || player.sessionId || player.id,
              userId: player.userId,
              sessionId: player.sessionId,
              nickname: player.displayName || player.nickname,
              displayName: player.displayName || player.nickname,
              avatarUrl: player.avatarUrl || '/images/default-avatar.png',
              isRobot: isRobot,
              // 机器人默认已准备，其他玩家使用返回的isReady值
              isReady: player.isReady !== undefined ? player.isReady : (isRobot ? true : false),
              // 保留原有的房主信息（第一个玩家或明确标记为房主的玩家）
              isHost: player.isHost !== undefined ? player.isHost : false
            };
          });

          // 确保第一个玩家是房主（如果没有明确的房主标记）
          const playersArray = Object.values(playersObject);
          if (playersArray.length > 0) {
            const hasHost = playersArray.some(p => p.isHost === true);
            if (!hasHost && playersArray[0]) {
              playersArray[0].isHost = true;
              const firstPlayerKey = Object.keys(playersObject)[0];
              playersObject[firstPlayerKey].isHost = true;
            }
          }

          // 构建roomState对象并更新界面
          const currentRoomInfo = this.data.roomInfo || {};
          const currentPlayers = this.data.players || [];
          const roomState = {
            roomId: this.data.roomId,
            roomName: currentRoomInfo.roomName || this.data.roomName || '好友局',
            gameState: 'WAITING',
            maxPlayers: this.data.maxPlayers || 6,
            maxRounds: currentRoomInfo.maxRounds || this.data.maxRounds || 3,
            targetScore: currentRoomInfo.targetScore || this.data.targetScore || 66,
            currentRound: currentRoomInfo.currentRound || this.data.currentRound || 1,
            currentTurnNumber: 0,
            players: playersObject
          };

          console.log('更新后的roomState:', roomState);

          // 调用handleRoomUpdate更新界面
          this.handleRoomUpdate({ roomState, type: 'gameStateUpdate' });
        } else {
          // 如果返回数据格式不对，等待WebSocket更新
          console.log('返回数据格式不正确，等待WebSocket更新玩家列表...', responseData);
        }
      })
      .catch((err) => {
        wx.hideLoading();
        console.error('添加机器人失败:', err);
        wx.showToast({
          title: err.message || '添加失败，请重试',
          icon: 'none',
          duration: 2000
        });
      });
  },

  /**
   * Simulate adding bots to the local state
   */
  simulateBotsAdded(count) {
    const players = [...this.data.players];
    const existingCount = players.length;

    for (let i = 0; i < count; i++) {
      const botId = `bot_${Date.now()}_${i}`;
      players.push({
        id: botId,
        nickname: `电脑 ${existingCount + i + 1}`,
        avatarUrl: '/images/default-avatar.png',
        isRobot: true,
        isReady: true, // Bots are usually auto-ready
        displayName: `电脑 ${existingCount + i + 1}`
      });
    }

    // Reuse the main update logic with the new simulated list
    const roomState = {
        roomName: this.data.roomInfo.roomName,
        maxPlayers: this.data.maxPlayers,
        maxRounds: this.data.roomInfo.maxRounds,
        targetScore: this.data.roomInfo.targetScore,
        currentRound: this.data.roomInfo.currentRound,
        gameState: this.data.canReady ? 'WAITING' : 'PLAYING',
        players: players
    };

    this.handleRoomUpdate({ roomState });
  },

  /**
   * 准备/取消准备（房主可以切换准备状态）
   */
  handleReady() {
    const { isMeReady, isHost } = this.data;
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const userIdentifier = userInfo.id;
    
    // 发送切换准备状态的消息
    this.sendSocketMsg({
      type: 'playerReady',
      roomId: this.data.roomId,
      userIdentifier,
      isReady: !isMeReady // 切换状态
    });
    
    // 乐观更新本地状态（如果后端没有立即响应）
    this.setData({
      isMeReady: !isMeReady
    });
  },

  /**
   * 开始游戏
   */
  handleStartGame() {
    // 调试信息：检查 isHost 状态
    console.log('handleStartGame - 当前状态:', {
      isHost: this.data.isHost,
      players: this.data.players.map(p => ({
        id: p.id,
        userId: p.userId,
        nickname: p.nickname,
        isHost: p.isHost
      })),
      currentUserId: this.data.currentUserId
    });
    
    if (!this.data.isHost) {
      // 尝试从 players 中重新查找当前用户是否为房主
      const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
      const userId = userInfo.id;
      const currentPlayer = this.data.players.find(p => 
        (p.id === userId || p.userId === userId) && p.isHost === true
      );
      
      if (!currentPlayer) {
        wx.showToast({
          title: '只有房主可以开始游戏',
          icon: 'none'
        });
        return;
      }
      
      // 如果找到了房主，更新 isHost 状态并继续
      console.log('发现当前用户是房主，更新状态');
      this.setData({ isHost: true });
    }

    const { players, canStartGame } = this.data;
    
    // 检查所有玩家是否都准备好了
    if (!canStartGame) {
      // 找出未准备的玩家
      const notReadyPlayers = players.filter(p => p.isReady === false || p.isReady === undefined);
      if (notReadyPlayers.length > 0) {
        const notReadyNames = notReadyPlayers
          .map(p => p.displayName || p.nickname || '未知玩家')
          .join('、');
        wx.showToast({
          title: `${notReadyNames} 还未准备`,
          icon: 'none',
          duration: 3000
        });
      } else {
        wx.showToast({
          title: '至少需要2名玩家才能开始游戏',
          icon: 'none'
        });
      }
      return;
    }

    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const userIdentifier = userInfo.id;
    this.sendSocketMsg({
      type: 'startGame',
      roomId: this.data.roomId,
      userIdentifier
    });
  },

  /**
   * 离开房间
   */
  handleLeaveRoom() {
    wx.showModal({
      title: '提示',
      content: '确定要离开房间吗？',
      success: (res) => {
        if (res.confirm) {
          const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
          const userIdentifier = userInfo.id;
          this.sendSocketMsg({
            type: 'leaveRoom',
            roomId: this.data.roomId,
            userIdentifier
          });
          wx.closeSocket();
          // Reset room state
          this.setData({ roomId: null, wsConnected: false });

          // Switch back to Lobby
          wx.switchTab({
            url: '/pages/lobby/lobby'
          });
        }
      }
    });
  },

  /**
   * 复制房间号
   */
  handleCopyRoomId() {
    wx.setClipboardData({
      data: this.data.roomId,
      success: () => {
        wx.showToast({
          title: '房间号已复制',
          icon: 'success'
        });
      }
    });
  },

  /**
   * 分享房间
   */
  handleShareRoom() {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },

  onShareAppMessage() {
    return {
      title: `邀请你加入房间 #${this.data.roomId}`,
      path: `/pages/room/room?roomId=${this.data.roomId}`,
      imageUrl: '/images/share-room.jpg'
    };
  },

  /**
   * Chat Input Handler
   */
  onInput(e) {
    this.setData({
      inputValue: e.detail.value
    });
  },

  /**
   * Send Chat Message
   */
  handleSendChat() {
    const content = this.data.inputValue.trim();
    if (!content) return;

    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    if (!userInfo || !userInfo.id) {
      wx.showToast({
        title: '用户信息失效，请重新登录',
        icon: 'none'
      });
      return;
    }
    const userIdentifier = userInfo.id;

    // Send via WebSocket
    this.sendSocketMsg({
      type: 'chat',
      roomId: this.data.roomId,
      userIdentifier: userIdentifier,
      data: {
        text: content
      }
    });
 
    // Clear input
    this.setData({
      inputValue: ''
    });
  },

  /**
   * Handle incoming chat message
   */
  handleNewChatMessage(msg) {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const myUserId = userInfo.id;

    // Normalize fields
    // msg.sender (from chatMessage) or msg.userIdentifier (old chat)
    const senderId = msg.sender || msg.userIdentifier || (msg.data && msg.data.sender);
    // msg.text (from chatMessage) or msg.content (old chat)
    const content = msg.text || msg.content || (msg.data && msg.data.text);

    if (!content) return;

    // Try to find sender name
    let senderName = 'Unknown';
    if (senderId) {
        // Check players list
        const player = this.data.players.find(p =>
            String(p.id) === String(senderId) ||
            String(p.userId) === String(senderId)
        );
        if (player) {
            senderName = player.displayName || player.nickname;
        } else if (String(senderId) === String(myUserId)) {
            senderName = userInfo.nickname || '我';
        } else {
             senderName = `玩家 ${senderId}`;
        }
    }

    const isMe = (String(senderId) === String(myUserId));

    const newMsg = {
      sender: senderName,
      content: content,
      isMe: isMe
    };

    const messages = [...this.data.chatMessages, newMsg];
    this.setData({
      chatMessages: messages
    });
  }
});

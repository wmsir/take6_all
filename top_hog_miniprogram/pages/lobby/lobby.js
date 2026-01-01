/**
 * 大厅页面
 */
const roomApi = require('../../api/roomApi.js');
const app = getApp();

Page({
  data: {
    userInfo: null,
    totalScore: 0,
    roomCode: '',
    roomList: [],
    loading: false
  },

  onLoad(options) {
    // 检查URL参数中的token
    if (options && options.token) {
      const token = options.token;
      app.globalData.token = token;
      wx.setStorageSync('token', token);
    }

    // 获取用户信息
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    if (!userInfo) {
      // 未登录，跳转到登录页
      wx.reLaunch({
        url: '/pages/login/login'
      });
      return;
    }

    this.setData({ userInfo });
    // Note: loadRoomList() removed from onLoad to avoid duplicate call with onShow
  },

  onShow() {
    // 每次显示时刷新房间列表
    this.loadRoomList();
  },

  /**
   * 加载房间列表
   */
  loadRoomList() {
    this.setData({ loading: true });

    roomApi.getRoomList({})
      .then((data) => {
        const roomList = (data.list || []).map(item => ({
          roomId: item.roomId,
          roomName: item.roomName || '未命名房间',
          currentPlayers: item.currentPlayers || 0,
          maxPlayers: item.maxPlayers || 4,
          gameConfig: this.formatGameConfig(item),
          status: item.gameState === 'PLAYING' ? 'playing' : 'waiting',
          statusText: item.gameState === 'PLAYING' ? '进行中' : '等待中'
        }));

        this.setData({ roomList });
      })
      .catch((error) => {
        console.error('加载房间列表失败:', error);
        // 使用模拟数据
        this.setData({
          roomList: [
            {
              roomId: 'A9K3',
              roomName: '好友局 · 小王的房间',
              currentPlayers: 4,
              maxPlayers: 6,
              gameConfig: '最多 3 局 · 超过 66 分结束',
              status: 'playing',
              statusText: '进行中',
              needPassword: false
            },
            {
              roomId: 'B43F',
              roomName: '快速匹配 · 公开房',
              currentPlayers: 2,
              maxPlayers: 5,
              gameConfig: '按分数 · >66 结束',
              status: 'waiting',
              statusText: '等待中',
              needPassword: false
            },
            {
              roomId: 'P671',
              roomName: '好友私密房',
              currentPlayers: 3,
              maxPlayers: 4,
              gameConfig: '需要密码进入',
              status: 'waiting',
              statusText: '需密码',
              needPassword: true
            }
          ]
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  /**
   * 格式化游戏配置
   */
  formatGameConfig(item) {
    const configs = [];
    if (item.maxRounds) {
      configs.push(`最多 ${item.maxRounds} 局`);
    }
    if (item.targetScore) {
      configs.push(`超过 ${item.targetScore} 分结束`);
    }
    return configs.join(' · ') || '默认配置';
  },

  /**
   * 房间号输入
   */
  onRoomCodeInput(e) {
    this.setData({ roomCode: e.detail.value.toUpperCase() });
  },

  /**
   * 创建房间
   */
  handleCreateRoom() {
    wx.navigateTo({
      url: '/pages/room_create/room_create'
    });
  },

  /**
   * 显示加入房间弹窗
   */
  handleShowJoinModal() {
    // 可以显示一个弹窗让用户输入房间号
    // 这里简化处理，直接聚焦输入框
  },

  /**
   * 快速匹配房间
   */
  handleQuickMatch() {
    wx.showLoading({
      title: '匹配中...'
    });

    roomApi.quickMatch()
      .then((data) => {
        // 保存当前房间信息
        app.globalData.currentRoom = data;
        wx.setStorageSync('currentRoom', data);

        wx.hideLoading();
        wx.showToast({
          title: '匹配成功',
          icon: 'success'
        });

        // 跳转到房间页面 (TabBar page)
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/room/room'
          });
        }, 500);
      })
      .catch((error) => {
        wx.hideLoading();
        console.error('快速匹配失败:', error);
        
        // 如果没有可用房间，提示用户
        wx.showModal({
          title: '匹配失败',
          content: error.message || '当前没有可用的房间，是否创建新房间？',
          confirmText: '创建房间',
          cancelText: '取消',
          success: (res) => {
            if (res.confirm) {
              wx.navigateTo({
                url: '/pages/room_create/room_create'
              });
            }
          }
        });
      });
  },

  /**
   * 通过房间号加入
   */
  handleJoinByCode() {
    const { roomCode } = this.data;

    if (!roomCode || roomCode.length < 4) {
      wx.showToast({
        title: '请输入有效的房间号',
        icon: 'none'
      });
      return;
    }

    this.joinRoom(roomCode);
  },

  /**
   * 加入房间
   */
  handleJoinRoom(e) {
    const { roomId, needPassword, status } = e.currentTarget.dataset;

    if (status === 'playing') {
      // 观战逻辑
      this.spectateRoom(roomId);
      return;
    }

    if (needPassword) {
      // 输入密码逻辑
      this.showPasswordModal(roomId);
      return;
    }

    if (roomId) {
      this.joinRoom(roomId);
    }
  },

  /**
   * 观战房间
   */
  spectateRoom(roomId) {
    wx.showToast({
      title: '进入观战模式...',
      icon: 'none'
    });
    // Store specating info
    app.globalData.spectatingRoomId = roomId;

    setTimeout(() => {
       // Assuming game page is also a tabbar page based on app.json
       wx.switchTab({
        url: '/pages/game/game'
      });
    }, 500);
  },

  /**
   * 显示密码弹窗
   */
  showPasswordModal(roomId) {
    wx.showModal({
      title: '私密房间',
      placeholderText: '请输入房间密码',
      editable: true,
      success: (res) => {
        if (res.confirm) {
          const password = res.content;
          this.joinRoom(roomId, password);
        }
      }
    });
  },

  /**
   * 加入房间逻辑
   */
  joinRoom(roomId, password = '') {
    wx.showLoading({
      title: '加入中...'
    });

    roomApi.joinRoom(roomId, password)
      .then((data) => {
        // 保存当前房间信息
        app.globalData.currentRoom = data;
        wx.setStorageSync('currentRoom', data);

        // 设置清除聊天记录的标志
        // 当进入新房间时，需要清除上一个房间的聊天记录
        app.globalData.clearRoomChat = true;
        app.globalData.clearGameChat = true;

        wx.hideLoading();
        wx.showToast({
          title: '加入成功',
          icon: 'success'
        });

        // 跳转到房间页面 (TabBar page)
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/room/room'
          });
        }, 500);
      })
      .catch((error) => {
        wx.hideLoading();
        console.error('加入房间失败:', error);
      });
  },

  /**
   * 刷新房间列表
   */
  handleRefresh() {
    this.loadRoomList();
  }
});

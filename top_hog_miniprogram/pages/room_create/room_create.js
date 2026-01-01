const roomApi = require('../../api/roomApi.js');
const app = getApp();

Page({
  data: {
    roomName: '',
    maxPlayers: 6,
    maxPlayersIndex: 4, // index 4 corresponds to value 6 in range [2...10]
    maxPlayersRange: [2, 3, 4, 5, 6, 7, 8, 9, 10],

    endTypeIndex: 0,
    endTypeRange: [{name: '按分数结束', value: 'score'}, {name: '按局数结束', value: 'round'}],

    targetScore: 66,
    maxRounds: 10,

    isPrivate: false,
    password: '',

    loading: false
  },

  onLoad() {
    // 默认房间名
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    if (userInfo) {
      this.setData({
        roomName: `${userInfo.nickname}的房间`
      });
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [field]: e.detail.value
    });
  },

  onMaxPlayersChange(e) {
    const index = e.detail.value;
    this.setData({
      maxPlayersIndex: index,
      maxPlayers: this.data.maxPlayersRange[index]
    });
  },

  onEndTypeChange(e) {
    this.setData({
      endTypeIndex: e.detail.value
    });
  },

  onPrivateChange(e) {
    this.setData({
      isPrivate: e.detail.value
    });
  },

  handleCreate() {
    if (this.data.loading) return;

    const { roomName, maxPlayers, endTypeIndex, targetScore, maxRounds, isPrivate, password } = this.data;

    if (!roomName) {
      wx.showToast({ title: '请输入房间名称', icon: 'none' });
      return;
    }

    if (isPrivate && (!password || password.length !== 4)) {
      wx.showToast({ title: '请输入4位数字密码', icon: 'none' });
      return;
    }

    const params = {
      roomName,
      maxPlayers,
      isPrivate,
      password: isPrivate ? password : ''
    };

    if (endTypeIndex == 0) {
      params.targetScore = parseInt(targetScore) || 66;
    } else {
      params.maxRounds = parseInt(maxRounds) || 10;
    }

    this.setData({ loading: true });

    roomApi.createRoom(params)
      .then((data) => {
        // Robust ID extraction
        const roomId = data.roomId || data.id || data;

        if (!roomId) {
          console.error('API did not return a valid roomId', data);
          wx.showToast({ title: '创建失败: 无法获取房间号', icon: 'none' });
          return;
        }

        // Ensure roomId is injected into the data object if we only got an ID string
        if (typeof data !== 'object') {
          data = { roomId: roomId, ...params };
        } else if (!data.roomId) {
          data.roomId = roomId;
        }

        // 保存当前房间信息到全局，以便 SwitchTab 后页面获取
        app.globalData.currentRoom = data;
        wx.setStorageSync('currentRoom', data);

        // 设置清除聊天记录的标志
        // 当创建新房间时，需要清除上一个房间的聊天记录
        app.globalData.clearRoomChat = true;
        app.globalData.clearGameChat = true;

        wx.showToast({
          title: '创建成功',
          icon: 'success'
        });

        // 跳转到房间页面 (Room page is a TabBar page, must use switchTab)
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/room/room',
            success: () => {
              console.log('Switched to Room tab');
            },
            fail: (err) => {
              console.error('SwitchTab failed:', err);
              wx.showToast({ title: '跳转失败', icon: 'none' });
            }
          });
        }, 500);
      })
      .catch((error) => {
        console.error('创建房间失败:', error);
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  }
});

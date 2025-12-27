App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'http://localhost:8080', // 后端服务器地址
    wsUrl: 'ws://localhost:8080/ws-game', // WebSocket地址
    currentRoom: null,
    isConnected: false
  },

  onLaunch() {
    // 检查登录状态
    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
      // 验证token有效性
      this.validateToken(token);
    }
  },

  validateToken(token) {
    wx.request({
      url: `${this.globalData.baseUrl}/api/auth/validate`,
      method: 'POST',
      header: {
        'auth': token,
        'Content-Type': 'application/json'
      },
      data: {},
      success: (res) => {
        const { code, data } = res.data;
        if (code === 200) {
          this.globalData.userInfo = data;
        } else {
          // token无效，清除存储
          wx.removeStorageSync('token');
          this.globalData.token = null;
          this.globalData.userInfo = null;
          wx.reLaunch({
            url: '/pages/login/login'
          });
        }
      },
      fail: () => {
        wx.removeStorageSync('token');
        this.globalData.token = null;
        this.globalData.userInfo = null;
        wx.reLaunch({
          url: '/pages/login/login'
        });
      }
    });
  }
}); 
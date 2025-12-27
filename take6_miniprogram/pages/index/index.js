const app = getApp();

Page({
  data: {
    userInfo: null,
    rooms: [],
    newRoomName: '',
    loading: false,
    creating: false,
    page: 1,
    hasMore: true
  },

  onLoad() {
    // 检查登录状态
    if (!app.globalData.token) {
      wx.redirectTo({
        url: '/pages/login/login'
      });
      return;
    }

    this.setData({
      userInfo: app.globalData.userInfo
    });

    // 获取房间列表
    this.fetchRooms();
  },

  onShow() {
    // 每次显示页面时刷新房间列表
    this.fetchRooms();
  },

  onPullDownRefresh() {
    this.setData({
      page: 1,
      hasMore: true,
      rooms: []
    }, () => {
      this.fetchRooms(() => {
        wx.stopPullDownRefresh();
      });
    });
  },

  onScrollToLower() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMoreRooms();
    }
  },

  onRoomNameInput(e) {
    this.setData({
      newRoomName: e.detail.value
    });
  },

  fetchRooms(callback) {
    if (this.data.loading) return;

    this.setData({ loading: true });

    wx.request({
      url: `${app.globalData.baseUrl}/api/rooms`,
      method: 'GET',
      header: {
        'Authorization': app.globalData.token
      },
      success: (res) => {
        console.log('获取房间列表响应:', res);
        if (res.data.code === 1000) {
          this.setData({
            rooms: res.data.data,
            page: 1,
            hasMore: res.data.data.length === 10 // 假设每页10条数据
          });
        } else {
          wx.showToast({
            title: res.data.message || '获取房间列表失败',
            icon: 'none'
          });
        }
      },
      fail: (error) => {
        console.error('获取房间列表失败:', error);
        wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none'
        });
      },
      complete: () => {
        this.setData({ loading: false });
        if (typeof callback === 'function') callback();
      }
    });
  },

  loadMoreRooms() {
    if (this.data.loading || !this.data.hasMore) return;

    this.setData({ loading: true });

    wx.request({
      url: `${app.globalData.baseUrl}/api/rooms`,
      method: 'GET',
      data: {
        page: this.data.page + 1
      },
      header: {
        'Authorization': app.globalData.token
      },
      success: (res) => {
        if (res.data.code === 1000) {
          const newRooms = res.data.data;
          this.setData({
            rooms: [...this.data.rooms, ...newRooms],
            page: this.data.page + 1,
            hasMore: newRooms.length === 10
          });
        } else {
          wx.showToast({
            title: res.data.message || '加载更多失败',
            icon: 'none'
          });
        }
      },
      fail: (error) => {
        console.error('加载更多房间失败:', error);
        wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none'
        });
      },
      complete: () => {
        this.setData({ loading: false });
      }
    });
  },

  handleCreateRoom() {
    const { newRoomName } = this.data;
    if (!newRoomName.trim()) {
      wx.showToast({
        title: '请输入房间名称',
        icon: 'none'
      });
      return;
    }

    this.setData({ creating: true });

    wx.request({
      url: `${app.globalData.baseUrl}/api/rooms/create`,
      method: 'POST',
      data: {
        roomName: newRoomName.trim()
      },
      header: {
        'Authorization': app.globalData.token
      },
      success: (res) => {
        console.log('创建房间响应:', res);
        if (res.data.code === 1000) {
          wx.showToast({
            title: '创建成功',
            icon: 'success'
          });
          this.setData({ newRoomName: '' });
          // 跳转到房间页面
          wx.navigateTo({
            url: `/pages/room/room?roomId=${res.data.data.roomId}`
          });
        } else {
          wx.showToast({
            title: res.data.message || '创建失败',
            icon: 'none'
          });
        }
      },
      fail: (error) => {
        console.error('创建房间失败:', error);
        wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none'
        });
      },
      complete: () => {
        this.setData({ creating: false });
      }
    });
  },

  handleJoinRoom(e) {
    const roomId = e.currentTarget.dataset.roomId;
    wx.navigateTo({
      url: `/pages/room/room?roomId=${roomId}`
    });
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          // 清除登录状态
          wx.removeStorageSync('token');
          app.globalData.token = null;
          app.globalData.userInfo = null;
          app.globalData.stompClient = null;
          
          // 跳转到登录页
          wx.redirectTo({
            url: '/pages/login/login'
          });
        }
      }
    });
  }
}); 
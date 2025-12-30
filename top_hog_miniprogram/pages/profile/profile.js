/**
 * 我的页面
 */
const userApi = require('../../api/userApi.js');
const app = getApp();

Page({
  data: {
    userInfo: null,
    registerDays: 0,
    totalGames: 0,
    winRate: 0,
    avgScore: 0,
    maxStreak: 0,
    recentResults: [],
    winCount: 0,
    drawCount: 0,
    loseCount: 0,
    maxBullScore: 0,
    minBullScore: 0,
    commonOpponents: ''
  },

  onLoad() {
    this.loadUserInfo();
    // loadUserStats removed from onLoad to prevent duplicate calls
    // onShow handles data fetching
  },

  onShow() {
    // 每次显示时刷新数据
    this.loadUserInfo();
    this.loadUserStats();
  },

  /**
   * 加载用户信息
   */
  loadUserInfo() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    if (!userInfo) {
      wx.reLaunch({
        url: '/pages/login/login'
      });
      return;
    }

    // 计算注册天数
    const registerTime = userInfo.registerTime || Date.now();
    const registerDays = Math.floor((Date.now() - registerTime) / (1000 * 60 * 60 * 24));

    this.setData({
      userInfo: userInfo,
      registerDays: registerDays || 12
    });
  },

  /**
   * 加载用户战绩
   */
  loadUserStats() {
    userApi.getUserStats({})
      .then((data) => {
        this.setData({
          totalGames: data.totalGames || 0,
          winRate: data.winRate || 0,
          avgScore: data.avgScore || 0,
          maxStreak: data.maxStreak || 0,
          maxBullScore: data.maxBullScore || 0,
          minBullScore: data.minBullScore || 0,
          commonOpponents: data.commonOpponents || '小王 / 小李 / 小陈'
        });

        // 加载最近战绩
        this.loadRecentResults();
      })
      .catch((error) => {
        console.error('加载用户战绩失败:', error);
        // 使用模拟数据
        this.setData({
          totalGames: 23,
          winRate: 54,
          avgScore: 11.3,
          maxStreak: 4,
          maxBullScore: 18,
          minBullScore: 2,
          commonOpponents: '小王 / 小李 / 小陈'
        });
        this.loadRecentResults();
      });
  },

  /**
   * 加载最近战绩
   */
  loadRecentResults() {
    userApi.getHistoryList({ limit: 10 })
      .then((data) => {
        const results = (data.list || []).map(item => {
          if (item.score < item.avgScore) return 'win';
          if (item.score === item.avgScore) return 'draw';
          return 'lose';
        });

        const winCount = results.filter(r => r === 'win').length;
        const drawCount = results.filter(r => r === 'draw').length;
        const loseCount = results.filter(r => r === 'lose').length;

        this.setData({
          recentResults: results.length > 0 ? results : ['win', 'win', 'draw', 'lose', 'win', 'lose', 'win', 'draw', 'win', 'lose'],
          winCount: winCount || 5,
          drawCount: drawCount || 2,
          loseCount: loseCount || 3
        });
      })
      .catch((error) => {
        console.error('加载最近战绩失败:', error);
        // 使用模拟数据
        this.setData({
          recentResults: ['win', 'win', 'draw', 'lose', 'win', 'lose', 'win', 'draw', 'win', 'lose'],
          winCount: 5,
          drawCount: 2,
          loseCount: 3
        });
      });
  },

  /**
   * 编辑资料
   */
  handleEdit() {
    console.log('Navigating to profile edit page');
    wx.navigateTo({
      url: '/pages/profile_edit/profile_edit',
      fail: (err) => {
        console.error('Navigation failed:', err);
        wx.showToast({
          title: '无法打开编辑页',
          icon: 'none'
        });
      }
    });
  },

  /**
   * 查看全部
   */
  handleViewAll() {
    wx.showToast({
      title: '历史战绩功能开发中',
      icon: 'none'
    });
  },

  /**
   * 意见反馈
   */
  handleFeedback() {
    wx.showToast({
      title: '反馈功能开发中',
      icon: 'none'
    });
  },

  /**
   * 隐私与安全
   */
  handlePrivacy() {
    wx.showToast({
      title: '隐私设置功能开发中',
      icon: 'none'
    });
  },

  /**
   * 退出登录
   */
  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          // 清除登录信息
          app.globalData.token = null;
          app.globalData.userInfo = null;
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');

          wx.showToast({
            title: '已退出登录',
            icon: 'success'
          });

          // 跳转到登录页
          setTimeout(() => {
            wx.reLaunch({
              url: '/pages/login/login'
            });
          }, 1500);
        }
      }
    });
  }
});

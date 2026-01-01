/**
 * Onboarding / 登录页面
 */
const authApi = require('../../api/authApi.js');
const app = getApp();

Page({
  data: {
    userInfo: null,
    phone: '',
    code: '',
    codeCountdown: 0,
    cardProgress: 0,
    wsConnected: false,
    canEnter: false,
    loading: false
  },

  onLoad() {
    // 检查是否已登录
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    if (token && userInfo) {
      this.setData({ userInfo });
      // 已登录，直接进入大厅
      this.goToLobby();
      return;
    }
  },

  /**
   * 微信授权登录
   */
  handleWechatLogin() {
    this.setData({ loading: true });

    // 获取微信登录 code
    wx.login({
      success: (res) => {
        if (res.code) {
          // 调用后端接口
          authApi.wechatLogin(res.code)
            .then((data) => {
              // 保存用户信息和 token（保存所有返回的字段）
              const userInfo = {
                ...data,
                nickname: data.nickname || '谁是猪头王玩家',
                avatarUrl: data.avatarUrl || '/images/default-avatar.png',
                phone: data.phone || null,
                registerTime: data.registerTime || Date.now()
              };
              const token = data.token;

              app.globalData.token = token;
              app.globalData.userInfo = userInfo;
              wx.setStorageSync('token', token);
              wx.setStorageSync('userInfo', userInfo);

              this.setData({
                userInfo,
                token
              });

              wx.showToast({
                title: '登录成功',
                icon: 'success'
              });

              // 自动进入大厅
              setTimeout(() => {
                this.goToLobby();
              }, 1500);
            })
            .catch((error) => {
              console.error('微信登录失败:', error);
            })
            .finally(() => {
              this.setData({ loading: false });
            });
        } else {
          wx.showToast({
            title: '获取登录凭证失败',
            icon: 'none'
          });
          this.setData({ loading: false });
        }
      },
      fail: (error) => {
        console.error('微信登录失败:', error);
        wx.showToast({
          title: '登录失败，请重试',
          icon: 'none'
        });
        this.setData({ loading: false });
      }
    });
  },

  /**
   * 手机号输入
   */
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },

  /**
   * 验证码输入
   */
  onCodeInput(e) {
    this.setData({ code: e.detail.value });
  },

  /**
   * 获取验证码
   */
  handleGetCode() {
    const { phone } = this.data;

    if (!phone) {
      wx.showToast({
        title: '请输入手机号',
        icon: 'none'
      });
      return;
    }

    // 验证手机号格式
    const phoneRegex = /^1[3-9]\d{9}$/;
    if (!phoneRegex.test(phone)) {
      wx.showToast({
        title: '手机号格式不正确',
        icon: 'none'
      });
      return;
    }

    // 调用获取验证码接口
    authApi.getPhoneCode(phone)
      .then(() => {
        wx.showToast({
          title: '验证码已发送',
          icon: 'success'
        });

        // 开始倒计时
        this.setData({ codeCountdown: 60 });
        this.startCodeCountdown();
      })
      .catch((error) => {
        console.error('获取验证码失败:', error);
      });
  },

  /**
   * 验证码倒计时
   */
  startCodeCountdown() {
    const timer = setInterval(() => {
      const countdown = this.data.codeCountdown - 1;
      if (countdown <= 0) {
        clearInterval(timer);
        this.setData({ codeCountdown: 0 });
      } else {
        this.setData({ codeCountdown: countdown });
      }
    }, 1000);
  },

  /**
   * 绑定手机号
   */
  handleBindPhone() {
    const { userInfo, phone, code } = this.data;

    // 如果需要绑定手机号
    if (phone && code) {
      this.setData({ loading: true });

      authApi.bindPhone(phone, code)
        .then(() => {
          // 更新用户信息
          const updatedUserInfo = { ...userInfo, phone };
          app.globalData.userInfo = updatedUserInfo;
          wx.setStorageSync('userInfo', updatedUserInfo);
          this.setData({ userInfo: updatedUserInfo });

          wx.showToast({
            title: '绑定成功',
            icon: 'success'
          });

          this.goToLobby();
        })
        .catch((error) => {
          console.error('绑定手机号失败:', error);
        })
        .finally(() => {
          this.setData({ loading: false });
        });
    } else {
       wx.showToast({
        title: '请输入手机号和验证码',
        icon: 'none'
      });
    }
  },

  /**
   * 跳转到大厅
   */
  goToLobby() {
    const token = this.data.token || app.globalData.token || wx.getStorageSync('token');
    wx.reLaunch({
      url: `/pages/lobby/lobby?token=${token}`
    });
  }
});

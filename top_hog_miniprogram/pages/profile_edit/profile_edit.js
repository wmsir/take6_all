const userApi = require('../../api/userApi.js');
const app = getApp();

Page({
  data: {
    userInfo: {
      id: '',
      avatarUrl: '',
      nickname: '',
      phone: '',
      registerTime: null
    },
    loading: false
  },

  onLoad() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    if (userInfo) {
      // 格式化注册时间
      const registerTime = userInfo.registerTime || Date.now();
      const registerDate = new Date(registerTime);
      const formattedDate = `${registerDate.getFullYear()}-${String(registerDate.getMonth() + 1).padStart(2, '0')}-${String(registerDate.getDate()).padStart(2, '0')} ${String(registerDate.getHours()).padStart(2, '0')}:${String(registerDate.getMinutes()).padStart(2, '0')}:${String(registerDate.getSeconds()).padStart(2, '0')}`;
      
      this.setData({
        userInfo: {
          id: userInfo.id || '',
          avatarUrl: userInfo.avatarUrl || '',
          nickname: userInfo.nickname || '',
          phone: userInfo.phone || '',
          registerTime: registerTime,
          registerTimeFormatted: formattedDate
        }
      });
    }
  },

  /**
   * 选择头像
   */
  onChooseAvatar(e) {
    const { avatarUrl } = e.detail;
    this.setData({
      ['userInfo.avatarUrl']: avatarUrl
    });
  },

  /**
   * 昵称输入
   */
  onNicknameInput(e) {
    this.setData({
      ['userInfo.nickname']: e.detail.value
    });
  },

  /**
   * 昵称输入完成（兼容 input type="nickname"）
   */
  onNicknameChange(e) {
    this.setData({
      ['userInfo.nickname']: e.detail.value
    });
  },

  /**
   * 手机号输入
   */
  onPhoneInput(e) {
    this.setData({
      ['userInfo.phone']: e.detail.value
    });
  },

  /**
   * 保存资料
   */
  handleSave() {
    const { avatarUrl, nickname, phone } = this.data.userInfo;

    if (!nickname) {
      wx.showToast({
        title: '请输入昵称',
        icon: 'none'
      });
      return;
    }

    this.setData({ loading: true });

    // 如果是临时文件路径（以 http 或 wxfile 开头，且不是网络图片），需要先上传
    if (avatarUrl && (avatarUrl.startsWith('http://tmp') || avatarUrl.startsWith('wxfile://'))) {
      userApi.uploadAvatar(avatarUrl)
        .then((uploadedUrl) => {
          this.updateProfile(nickname, uploadedUrl, phone);
        })
        .catch((error) => {
          console.error('头像上传失败:', error);
          this.setData({ loading: false });
          wx.showToast({
            title: '头像上传失败',
            icon: 'none'
          });
        });
    } else {
      // 头像未修改或是网络图片
      this.updateProfile(nickname, avatarUrl, phone);
    }
  },

  /**
   * 更新个人信息
   */
  updateProfile(nickname, avatarUrl, phone) {
    userApi.updateUserInfo({ nickname, avatarUrl, phone })
      .then((data) => {
        // 更新本地存储，保留原有字段（id, registerTime等）
        const currentUserInfo = app.globalData.userInfo || {};
        const userInfo = { 
          ...currentUserInfo, 
          nickname, 
          avatarUrl, 
          phone 
        };
        if (data) {
          // 如果后端返回了新的用户信息，合并使用
          Object.assign(userInfo, data);
        }

        app.globalData.userInfo = userInfo;
        wx.setStorageSync('userInfo', userInfo);

        wx.showToast({
          title: '保存成功',
          icon: 'success'
        });

        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      })
      .catch((error) => {
        console.error('更新失败:', error);
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  }
});

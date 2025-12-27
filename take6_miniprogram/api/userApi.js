/**
 * 用户相关 API
 */
const { request } = require('./request.js');

/**
 * 获取用户信息
 * @returns {Promise}
 */
function getUserInfo() {
  return request({
    url: '/api/user/info',
    method: 'POST',
    data: {}
  });
}

/**
 * 获取用户战绩
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
function getUserStats(params = {}) {
  return request({
    url: '/api/user/stats',
    method: 'POST',
    data: params
  });
}

/**
 * 获取历史战绩列表
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
function getHistoryList(params = {}) {
  return request({
    url: '/api/user/history',
    method: 'POST',
    data: params
  });
}

/**
 * 更新用户信息
 * @param {Object} data 用户信息 { nickname, avatarUrl, phone, email }
 * @returns {Promise}
 */
function updateUserInfo(data) {
  return request({
    url: '/api/user/update',
    method: 'POST',
    data: data
  });
}

/**
 * 上传头像
 * @param {string} filePath 文件路径
 * @returns {Promise}
 */
function uploadAvatar(filePath) {
  const app = getApp();
  return new Promise((resolve, reject) => {
    const token = app.globalData.token || wx.getStorageSync('token');

    if (!token) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      });
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/login/login' });
      }, 1500);
      return reject(new Error('未登录'));
    }

    wx.uploadFile({
      url: `${app.globalData.baseUrl}/api/user/upload-avatar`,
      filePath: filePath,
      name: 'file',
      header: {
        'auth': token
      },
      success(res) {
        // 处理 HTTP 状态码 401
        if (res.statusCode === 401) {
          handleAuthError(app, reject);
          return;
        }

        try {
          const data = JSON.parse(res.data);

          // 处理业务状态码 401
          if (data.code === 401) {
            handleAuthError(app, reject);
            return;
          }

          if (data.code === 200) {
            resolve(data.data.url || data.data);
          } else {
            reject(new Error(data.message || '上传失败'));
          }
        } catch (e) {
          reject(new Error('响应解析失败'));
        }
      },
      fail(err) {
        console.error('Upload failed:', err);
        wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none'
        });
        reject(err);
      }
    });
  });
}

/**
 * 处理认证失败
 */
function handleAuthError(app, reject) {
  app.globalData.token = null;
  app.globalData.userInfo = null;
  wx.removeStorageSync('token');
  wx.removeStorageSync('userInfo');

  wx.showToast({
    title: '登录已失效',
    icon: 'none'
  });

  setTimeout(() => {
    wx.reLaunch({
      url: '/pages/login/login'
    });
  }, 1500);

  reject(new Error('需要登录'));
}

module.exports = {
  getUserInfo,
  getUserStats,
  getHistoryList,
  updateUserInfo,
  uploadAvatar
};

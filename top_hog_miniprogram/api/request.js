/**
 * 通用 API 请求处理函数
 * 统一处理后端 API 返回值
 */

const app = getApp();

/**
 * 通用请求函数
 * @param {Object} options 请求配置
 * @param {string} options.url 请求地址
 * @param {string} options.method 请求方法，默认 POST
 * @param {Object} options.data 请求参数，默认 {}
 * @param {boolean} options.needAuth 是否需要认证，默认 true
 * @returns {Promise} 返回 Promise
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const {
      url,
      method = 'POST',
      data = {},
      needAuth = true
    } = options;

    // 构建请求头
    const header = {
      'Content-Type': 'application/json'
    };

    // 如果需要认证，添加 token
    if (needAuth) {
      const token = app.globalData.token || wx.getStorageSync('token');
      if (token) {
        header['auth'] = token;
      }
    }

    wx.request({
      url: `${app.globalData.baseUrl}${url}`,
      method: method,
      header: header,
      data: data,
      success: (res) => {
        // 统一处理返回结果
        handleResponse(res, resolve, reject);
      },
      fail: (error) => {
        console.error('请求失败:', error);
        wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none'
        });
        reject(error);
      }
    });
  });
}

/**
 * 处理响应结果
 * @param {Object} res 响应对象
 * @param {Function} resolve Promise resolve
 * @param {Function} reject Promise reject
 */
function handleResponse(res, resolve, reject) {
  const { code, message, data } = res.data;

  // code = 200 表示成功
  if (code === 200) {
    resolve(data);
    return;
  }

  // code = 500 表示系统异常
  if (code === 500) {
    wx.showToast({
      title: '系统异常，请稍后重试',
      icon: 'none',
      duration: 2000
    });
    reject(new Error(message || '系统异常'));
    return;
  }

  // code = 401 表示需要登录
  if (code === 401) {
    // 清除登录信息
    app.globalData.token = null;
    app.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');

    wx.showToast({
      title: '请先登录',
      icon: 'none'
    });

    // 跳转到登录页
    setTimeout(() => {
      wx.reLaunch({
        url: '/pages/login/login'
      });
    }, 1500);

    reject(new Error('需要登录'));
    return;
  }

  // 其他业务异常，直接弹出 message
  wx.showToast({
    title: message || '操作失败',
    icon: 'none'
  });
  reject(new Error(message || '操作失败'));
}

module.exports = {
  request
};


/**
 * 认证相关 API
 */
const { request } = require('./request.js');

/**
 * 微信登录
 * @param {string} code 微信登录 code
 * @returns {Promise}
 */
function wechatLogin(code) {
  return request({
    url: '/api/auth/wechat/login',
    method: 'POST',
    data: { code },
    needAuth: false
  });
}

/**
 * 绑定手机号
 * @param {string} phone 手机号
 * @param {string} code 验证码
 * @returns {Promise}
 */
function bindPhone(phone, code) {
  return request({
    url: '/api/auth/bind/phone',
    method: 'POST',
    data: { phone, code }
  });
}

/**
 * 获取手机验证码
 * @param {string} phone 手机号
 * @returns {Promise}
 */
function getPhoneCode(phone) {
  return request({
    url: '/api/auth/phone/code',
    method: 'POST',
    data: { phone },
    needAuth: false
  });
}

module.exports = {
  wechatLogin,
  bindPhone,
  getPhoneCode
};


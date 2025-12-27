/**
 * 房间相关 API
 */
const { request } = require('./request.js');

/**
 * 创建房间
 * @param {Object} params 房间参数
 * @param {number} params.maxPlayers 最大人数
 * @param {number} params.maxRounds 最大局数
 * @param {number} params.targetScore 目标分数
 * @param {boolean} params.isPrivate 是否私密
 * @param {string} params.password 密码（私密房间）
 * @returns {Promise}
 */
function createRoom(params) {
  return request({
    url: '/api/room/create',
    method: 'POST',
    data: params
  });
}

/**
 * 加入房间
 * @param {string} roomId 房间号
 * @param {string} password 密码（可选）
 * @returns {Promise}
 */
function joinRoom(roomId, password) {
  return request({
    url: '/api/room/join',
    method: 'POST',
    data: { roomId, password: password || '' }
  });
}

/**
 * 获取房间列表
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
function getRoomList(params = {}) {
  return request({
    url: '/api/room/list',
    method: 'POST',
    data: params
  });
}

/**
 * 离开房间
 * @param {string} roomId 房间号
 * @returns {Promise}
 */
function leaveRoom(roomId) {
  return request({
    url: '/api/room/leave',
    method: 'POST',
    data: { roomId }
  });
}

/**
 * 准备游戏
 * @param {string} roomId 房间号
 * @returns {Promise}
 */
function readyGame(roomId) {
  return request({
    url: '/api/room/ready',
    method: 'POST',
    data: { roomId }
  });
}

/**
 * 开始游戏
 * @param {string} roomId 房间号
 * @returns {Promise}
 */
function startGame(roomId) {
  return request({
    url: '/api/room/start',
    method: 'POST',
    data: { roomId }
  });
}

/**
 * 添加机器人
 * @param {string} roomId 房间号
 * @param {number} botCount 机器人数量
 * @returns {Promise}
 */
function addBots(roomId, botCount) {
  return request({
    url: '/api/room/add-bots',
    method: 'POST',
    data: { roomId, botCount }
  });
}

module.exports = {
  createRoom,
  joinRoom,
  getRoomList,
  leaveRoom,
  readyGame,
  startGame,
  addBots
};

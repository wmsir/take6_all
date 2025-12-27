/**
 * 游戏相关 API
 */
const { request } = require('./request.js');

/**
 * 出牌
 * @param {string} roomId 房间号
 * @param {number} cardNumber 卡牌数字
 * @returns {Promise}
 */
function playCard(roomId, cardNumber) {
  return request({
    url: '/api/game/play',
    method: 'POST',
    data: { roomId, cardNumber }
  });
}

/**
 * 选择收走某行（当牌太小无处可接时）
 * @param {string} roomId 房间号
 * @param {number} rowIndex 行索引
 * @returns {Promise}
 */
function takeRow(roomId, rowIndex) {
  return request({
    url: '/api/game/take-row',
    method: 'POST',
    data: { roomId, rowIndex }
  });
}

/**
 * 获取游戏状态
 * @param {string} roomId 房间号
 * @returns {Promise}
 */
function getGameState(roomId) {
  return request({
    url: '/api/game/state',
    method: 'POST',
    data: { roomId }
  });
}

module.exports = {
  playCard,
  takeRow,
  getGameState
};


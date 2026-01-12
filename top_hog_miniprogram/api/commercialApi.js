/**
 * 商业化 API (广告、新手引导)
 */
const { request } = require('./request.js');

/**
 * 获取广告奖励
 * @param {string} type 广告类型
 */
function rewardAd(type) {
    return request({
        url: '/api/commercial/ad/reward',
        method: 'POST',
        data: { type }
    });
}

/**
 * 完成新手引导
 */
function finishGuide() {
    return request({
        url: '/api/commercial/guide/finish',
        method: 'POST',
        data: {}
    });
}

module.exports = {
    rewardAd,
    finishGuide
};

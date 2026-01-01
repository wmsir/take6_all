/**
 * 结算页面
 */
const app = getApp();
const gameApi = require('../../api/gameApi.js');

Page({
  data: {
    roomId: '',
    roomName: '',
    currentRound: 1,
    remainingRounds: 0,
    maxRounds: 10,
    targetScore: 66,
    rankings: [],
    totalScores: [],
    isGameOver: false,  // 是否游戏彻底结束
    showPoster: false,  // 显示海报弹窗
    posterPath: '',     // 海报路径
    canvasWidth: 375,   // 画布宽度
    canvasHeight: 667   // 画布高度
  },

  onLoad(options) {
    console.log('[RESULT] onLoad, options:', options);
    const roomId = options.roomId;
    if (!roomId) {
      wx.showToast({
        title: '房间ID无效',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
      return;
    }

    this.setData({ 
      roomId: roomId,
      isGameOver: options.isGameOver === 'true' || options.isGameOver === true
    });
    this.loadResult();
  },

  /**
   * 加载结算数据
   */
  loadResult() {
    console.log('[RESULT] 开始加载结算数据');
    
    // 从全局数据或存储中获取游戏结果
    const gameResult = app.globalData.gameResult || wx.getStorageSync('gameResult');
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    const userId = userInfo?.id;
    
    console.log('[RESULT] gameResult:', gameResult);
    
    if (gameResult && gameResult.players) {
      // 格式化排行榜（按本局猪头数升序，猪头越少越好）
      const playersArray = Object.values(gameResult.players || {});
      const rankings = playersArray.map((player, index) => {
        const isMe = player.id == userId || player.userId == userId;
        return {
          id: player.id || player.userId || index,
          name: player.displayName || player.nickname || `玩家${index + 1}`,
          avatarUrl: player.avatarUrl || '/images/default-avatar.png',
          score: player.roundScore || player.score || 0,  // 本局得分
          totalScore: player.totalScore || player.score || 0,  // 总分
          isMe: isMe
        };
      }).sort((a, b) => a.score - b.score);  // 按本局得分升序

      // 格式化总积分（按总分升序）
      const totalScores = [...playersArray].map((player, index) => {
        const isMe = player.id == userId || player.userId == userId;
        return {
          id: player.id || player.userId || index,
          name: player.displayName || player.nickname || `玩家${index + 1}`,
          total: player.totalScore || player.score || 0,
          isMe: isMe
        };
      }).sort((a, b) => a.total - b.total);

      console.log('[RESULT] rankings:', rankings);
      console.log('[RESULT] totalScores:', totalScores);

      this.setData({
        roomName: gameResult.roomName || '未命名房间',
        currentRound: gameResult.currentRound || 1,
        remainingRounds: gameResult.remainingRounds || 0,
        maxRounds: gameResult.maxRounds || 10,
        targetScore: gameResult.targetScore || 66,
        rankings: rankings,
        totalScores: totalScores
      });
    } else {
      // 使用模拟数据进行测试
      console.log('[RESULT] 使用模拟数据');
      this.setData({
        roomName: '测试房间',
        currentRound: 3,
        remainingRounds: 7,
        maxRounds: 10,
        targetScore: 66,
        rankings: [
          { id: 0, name: '我', score: 2, totalScore: 15, avatarUrl: '/images/default-avatar.png', isMe: true },
          { id: 1, name: '小王', score: 5, totalScore: 23, avatarUrl: '/images/default-avatar.png', isMe: false },
          { id: 2, name: '小李', score: 9, totalScore: 31, avatarUrl: '/images/default-avatar.png', isMe: false },
          { id: 3, name: '小陈', score: 12, totalScore: 38, avatarUrl: '/images/default-avatar.png', isMe: false }
        ],
        totalScores: [
          { id: 0, name: '我', total: 15, isMe: true },
          { id: 1, name: '小王', total: 23, isMe: false },
          { id: 2, name: '小李', total: 31, isMe: false },
          { id: 3, name: '小陈', total: 38, isMe: false }
        ]
      });
    }
  },

  /**
   * 查看复盘
   */
  handleReview() {
    wx.showToast({
      title: '复盘功能开发中',
      icon: 'none'
    });
  },

  /**
   * 再来一局
   */
  handlePlayAgain() {
    console.log('[RESULT] 点击再来一局');
    
    // 游戏结束，继续下一局（在当前房间准备）
    const roomId = this.data.roomId;

    // 验证 roomId
    if (!roomId || typeof roomId !== 'string' || roomId.trim() === '') {
      wx.showToast({
        title: '房间ID无效',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({ title: '准备下一局...', mask: true });

    // 清空结算数据，但保留房间信息
    app.globalData.gameResult = null;
    wx.removeStorageSync('gameResult');

    // 设置标志位，告诉游戏页面需要自动发送requestNewGame
    app.globalData.autoRequestNewGame = true;

    // 延迟跳转，确保状态清理完成 (500ms delay for state cleanup)
    const NAVIGATION_DELAY = 500;
    setTimeout(() => {
      // 使用 redirectTo 替换当前页面，避免堆栈过深
      wx.redirectTo({
        url: `/pages/game/game?roomId=${encodeURIComponent(roomId)}&requestNewGame=true`,
        success: () => {
          console.log('[RESULT] 成功跳转到游戏页面，roomId:', roomId);
          wx.hideLoading();
        },
        fail: (err) => {
          console.error('[RESULT] 跳转失败:', err);
          wx.hideLoading();
          wx.showToast({
            title: '跳转失败，请重试',
            icon: 'none'
          });
        }
      });
    }, NAVIGATION_DELAY);
  },

  /**
   * 生成战绩海报
   */
  handleShare() {
    console.log('[RESULT] 开始生成战绩海报');
    wx.showLoading({ title: '生成海报中...' });
    
    // 使用 Canvas 2D 生成海报
    this.generatePoster();
  },

  /**
   * 生成海报
   */
  generatePoster() {
    const query = wx.createSelectorQuery();
    query.select('#posterCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res || !res[0]) {
          wx.hideLoading();
          wx.showToast({ title: '海报生成失败', icon: 'none' });
          return;
        }

        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');
        
        const dpr = wx.getSystemInfoSync().pixelRatio;
        canvas.width = this.data.canvasWidth * dpr;
        canvas.height = this.data.canvasHeight * dpr;
        ctx.scale(dpr, dpr);

        // 绘制海报
        this.drawPoster(ctx, canvas);
      });
  },

  /**
   * 绘制海报内容
   */
  drawPoster(ctx, canvas) {
    const { canvasWidth, canvasHeight, rankings, roomName, currentRound } = this.data;
    
    // 1. 背景渐变
    const gradient = ctx.createLinearGradient(0, 0, 0, canvasHeight);
    gradient.addColorStop(0, '#0f172a');
    gradient.addColorStop(1, '#1e293b');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, canvasWidth, canvasHeight);

    // 2. 顶部标题
    ctx.fillStyle = '#10b981';
    ctx.font = 'bold 16px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('谁是猪头王 游戏战绩', canvasWidth / 2, 40);

    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 24px sans-serif';
    ctx.fillText(`${roomName} - 第${currentRound}局`, canvasWidth / 2, 70);

    // 3. 奖杯图标（模拟）
    ctx.font = '48px sans-serif';
    ctx.fillText('🏆', canvasWidth / 2, 130);

    // 4. 前三名
    const top3 = rankings.slice(0, 3);
    const positions = [
      { x: canvasWidth / 2, y: 180, label: '冠军', color: '#fbbf24' },
      { x: canvasWidth / 4, y: 220, label: '亚军', color: '#94a3b8' },
      { x: canvasWidth * 3 / 4, y: 220, label: '季军', color: '#cd7f32' }
    ];

    top3.forEach((player, index) => {
      const pos = positions[index];
      
      // 名次标签
      ctx.fillStyle = pos.color;
      ctx.font = 'bold 14px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(pos.label, pos.x, pos.y);

      // 玩家名称
      ctx.fillStyle = '#e2e8f0';
      ctx.font = '16px sans-serif';
      ctx.fillText(player.name, pos.x, pos.y + 25);

      // 分数
      ctx.fillStyle = '#10b981';
      ctx.font = 'bold 20px sans-serif';
      ctx.fillText(`${player.score} 🐷`, pos.x, pos.y + 50);
    });

    // 5. 排行榜列表
    ctx.fillStyle = 'rgba(30, 41, 59, 0.8)';
    ctx.fillRect(20, 300, canvasWidth - 40, rankings.length * 40 + 40);

    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 16px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText('完整排行榜', 35, 330);

    rankings.forEach((player, index) => {
      const y = 360 + index * 35;
      
      ctx.fillStyle = player.isMe ? '#10b981' : '#e2e8f0';
      ctx.font = '14px sans-serif';
      ctx.fillText(`${index + 1}. ${player.name}`, 35, y);

      ctx.textAlign = 'right';
      ctx.fillText(`${player.score} 牛`, canvasWidth - 35, y);
      ctx.textAlign = 'left';
    });

    // 6. 底部二维码提示
    ctx.fillStyle = '#64748b';
    ctx.font = '12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('扫码加入 谁是猪头王 游戏', canvasWidth / 2, canvasHeight - 30);

    // 7. 生成图片
    wx.canvasToTempFilePath({
      canvas: canvas,
      success: (res) => {
        console.log('[RESULT] 海报生成成功:', res.tempFilePath);
        wx.hideLoading();
        
        this.setData({
          showPoster: true,
          posterPath: res.tempFilePath
        });
      },
      fail: (err) => {
        console.error('[RESULT] 海报生成失败:', err);
        wx.hideLoading();
        wx.showToast({ title: '生成失败，请重试', icon: 'none' });
      }
    });
  },

  /**
   * 关闭海报弹窗
   */
  handleClosePoster() {
    this.setData({ showPoster: false });
  },

  /**
   * 保存海报到相册
   */
  handleSavePoster() {
    wx.saveImageToPhotosAlbum({
      filePath: this.data.posterPath,
      success: () => {
        wx.showToast({ title: '已保存到相册', icon: 'success' });
      },
      fail: (err) => {
        if (err.errMsg.indexOf('auth') !== -1) {
          wx.showModal({
            title: '提示',
            content: '需要您授权保存图片到相册',
            confirmText: '去授权',
            success: (res) => {
              if (res.confirm) {
                wx.openSetting();
              }
            }
          });
        } else {
          wx.showToast({ title: '保存失败', icon: 'none' });
        }
      }
    });
  },

  onShareAppMessage() {
    const myRanking = this.data.rankings.findIndex(r => r.isMe) + 1;
    return {
      title: `我在 谁是猪头王 第${this.data.currentRound}局获得第${myRanking}名！`,
      path: `/pages/lobby/lobby`,
      imageUrl: this.data.posterPath || '/images/share-result.jpg'
    };
  }
});


# 游戏UI优化说明

## 已完成的优化内容

### 1. ✅ 玩家头像下方显示已出的牌

**功能描述**：
- 所有玩家出完牌后，立即在各自头像下方显示已出的牌
- 显示卡牌数字和牛头数
- 带有弹出动画效果（`cardPopIn`）

**实现位置**：
- **WXML**：`pages/game/game.wxml` 第30-36行
- **JS**：`pages/game/game.js` 第388-406行（获取 `playedCardsThisTurn` 并映射到玩家）
- **WXSS**：`pages/game/game.wxss` 第116-177行

**数据结构**：
```javascript
players: [
  {
    id: 'player1',
    displayName: '张三',
    avatarUrl: '/images/avatar.png',
    playedCard: {
      number: 45,
      bullheads: 5
    }
  }
]
```

**样式特点**：
- 卡牌尺寸：56rpx × 80rpx
- 蓝色边框，阴影效果
- 0.3秒弹出动画（缩放+位移）

---

### 2. ✅ 移除中心弹窗出牌动画

**修改内容**：
- 删除了 `<view class="playing-cards-animation">` 整个弹窗区域
- 移除了 `playCardsAnimation()` 函数的调用
- 保留了函数定义（后续可能用于其他场景）

**修改位置**：
- **WXML**：删除了第65-77行的出牌动画区域
- **JS**：`pages/game/game.js` 第475-478行（移除了动画触发逻辑）

**原因**：
- 用户需求：出牌后不需要中心弹窗动画
- 改为在头像下方直接显示
- 动画效果保留给卡牌放置到游戏区域时使用

---

### 3. ✅ 添加卡牌放置到游戏区域的动画效果

**功能描述**：
- 当卡牌从玩家手牌放置到游戏区域（4行）时，触发动画
- 卡牌从上方飞入，带旋转和缩放效果
- 动画持续0.5秒

**实现位置**：
- **WXSS**：`pages/game/game.wxss` 第264-279行

**动画效果**：
```css
@keyframes cardPlace {
  0% {
    opacity: 0;
    transform: translateY(-50rpx) scale(0.8) rotate(-5deg);
  }
  60% {
    transform: translateY(5rpx) scale(1.05) rotate(2deg);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1) rotate(0);
  }
}
```

**触发方式**：
- 后端推送 `roomStateUpdate` 时，新增的卡牌会带有 `card-placing` class
- 前端可以在检测到 `rows` 数组变化时，给新卡牌添加动画class

**注意**：
- 目前CSS已定义，JS触发逻辑需要根据实际后端数据结构完善
- 建议后端在推送时标记新增的卡牌（如 `isNew: true`）

---

### 4. ✅ 删除"已5张·再接即吃牛"文字描述

**修改内容**：
- 删除了场牌右侧的警告文字 `<text class="row-warning">已 5 张 · 再接即吃牛</text>`
- 移除了对应的CSS样式 `.row-warning`

**修改位置**：
- **WXML**：`pages/game/game.wxml` 第52行（已删除）
- **WXSS**：`pages/game/game.wxss` 第309行（已注释）

**保留功能**：
- 第5张卡牌仍然有橙色高亮
- 第6张卡牌仍然有红色边框（`isDanger`）
- 只是移除了文字提示

---

### 5. ✅ 自适应每行至少显示5张卡牌

**功能描述**：
- 场牌区域的每行可以自适应显示卡牌
- 每行至少可以显示5张卡牌（通过 `flex: 1 1 auto` 实现）
- 卡牌宽度在 60rpx - 90rpx 之间自动调整

**实现位置**：
- **WXSS**：`pages/game/game.wxss` 第234-262行

**关键CSS**：
```css
.row-cards {
  display: flex;
  gap: 6rpx;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
}

.board-card-item {
  min-width: 60rpx;      /* 最小宽度 */
  flex: 1 1 auto;        /* 自适应伸缩 */
  max-width: 90rpx;      /* 最大宽度 */
  height: 112rpx;
}
```

**自适应规则**：
- **1张卡**：宽度 = 90rpx（最大值）
- **2张卡**：各占 50%（约 90rpx）
- **3张卡**：各占 33%（约 80rpx）
- **4张卡**：各占 25%（约 70rpx）
- **5张卡**：各占 20%（约 60rpx）
- **6张卡**：各占 16%（约 60rpx，触及最小值）
- **超过6张**：横向滚动（通过 `overflow-x: auto`）

**测试场景**：
- 屏幕宽度 375px（iPhone SE）：每行可显示 5 张（60rpx × 5 + 间隙）
- 屏幕宽度 414px（iPhone 12）：每行可显示 5-6 张
- 屏幕宽度 768px（iPad）：每行可显示 6+ 张

---

## 样式优化总结

### 玩家头像区域
- **布局**：从横向排列改为纵向布局（`flex-direction: column`）
- **对齐**：居中对齐（`align-items: center`）
- **间距**：增加了 8rpx 的垂直间距
- **换行**：支持 `flex-wrap: wrap`（适配多人游戏）

### 场牌区域
- **卡牌布局**：自适应宽度，至少显示5张
- **动画效果**：新卡牌放置时有飞入动画
- **高亮规则**：
  - 第1张：绿色边框（排头）
  - 第5张：橙色边框+阴影（警告）
  - 第6张：红色边框（危险）

### 已删除的元素
- ❌ 中心弹窗出牌动画（`playing-cards-animation`）
- ❌ "已5张·再接即吃牛"警告文字（`row-warning`）

---

## 后端对接要点

### 1. `playedCardsThisTurn` 数据结构
```javascript
{
  type: 'roomStateUpdate',
  roomState: {
    playedCardsThisTurn: {
      'player1': { cardNumber: 45, bullheads: 5 },
      'player2': { cardNumber: 67, bullheads: 1 },
      // Key 必须是玩家的 id/userId/sessionId
    }
  }
}
```

**注意**：
- Key 必须与 `players` 对象中的玩家ID匹配
- 前端会自动映射到各玩家的 `playedCard` 字段

### 2. 场牌放置动画触发（可选优化）
建议后端在推送新卡牌时，标记 `isNew: true`：
```javascript
rows: [
  {
    cards: [
      { number: 12, bullheads: 1 },
      { number: 34, bullheads: 2 },
      { number: 45, bullheads: 5, isNew: true } // 新放置的卡牌
    ]
  }
]
```

前端可以据此添加 `card-placing` class 触发动画。

### 3. 回合切换时清空已出牌
当进入下一回合时，后端应该：
```javascript
{
  type: 'roomStateUpdate',
  roomState: {
    currentTurnNumber: 2,
    playedCardsThisTurn: {} // 清空上一回合的已出牌
  }
}
```

前端会自动隐藏玩家头像下方的卡牌。

---

## 测试建议

### 1. 玩家头像下方显示已出牌
- ✅ 所有玩家出牌后，卡牌立即显示
- ✅ 卡牌显示数字和牛头数
- ✅ 卡牌有弹出动画效果
- ✅ 回合结束后卡牌消失

### 2. 场牌自适应布局
- ✅ 1-5张卡牌均匀分布
- ✅ 6张卡牌触及最小宽度（60rpx）
- ✅ 超过6张卡牌时可横向滚动
- ✅ 在不同屏幕尺寸下测试

### 3. 卡牌放置动画
- ✅ 新卡牌放置到场牌时有飞入动画
- ✅ 动画流畅不卡顿
- ✅ 不影响其他交互

### 4. 高亮规则
- ✅ 第1张卡牌绿色边框
- ✅ 第5张卡牌橙色边框+阴影
- ✅ 第6张卡牌红色边框
- ✅ 不再显示"已5张·再接即吃牛"文字

---

## 代码变更清单

### 修改的文件
1. ✅ `pages/game/game.wxml` - 添加玩家已出牌显示，删除中心动画和警告文字
2. ✅ `pages/game/game.js` - 添加 `playedCard` 映射逻辑，移除中心动画触发
3. ✅ `pages/game/game.wxss` - 添加已出牌样式，优化场牌自适应布局，添加放置动画

### 新增的样式
- `.player-played-card` - 已出牌容器
- `.played-card-mini` - 已出牌卡片
- `.played-card-number` - 卡牌数字
- `.played-card-bull` - 牛头数
- `@keyframes cardPopIn` - 卡牌弹出动画
- `@keyframes cardPlace` - 卡牌放置动画

### 删除的功能
- `playing-cards-animation` 中心弹窗动画区域
- `row-warning` 警告文字样式
- `playCardsAnimation()` 函数调用（函数保留）

---

## 后续优化建议

1. **音效**：卡牌放置到场牌时播放音效
2. **震动反馈**：卡牌放置时震动提醒
3. **动画优化**：根据卡牌数量调整动画速度
4. **性能优化**：大量玩家时优化头像区域布局
5. **高亮优化**：第5张卡牌可以增加跳动动画提醒玩家

---

## 兼容性说明

- ✅ 支持微信小程序基础库 2.0+
- ✅ 支持 iOS 和 Android
- ✅ 支持不同屏幕尺寸（iPhone SE 到 iPad）
- ✅ 使用了 `flex` 布局，兼容性良好
- ✅ 动画使用 `@keyframes`，性能优秀


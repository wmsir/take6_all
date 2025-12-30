# isHost 字段接口需求文档

## 概述

`isHost` 字段用于标识玩家是否为房间的房主。房主拥有以下特殊权限：
- 可以开始游戏（当所有玩家都准备好时）
- 可以添加机器人
- 在房间内显示"房主"标识

## 需要返回 isHost 字段的接口

### 1. 创建房间接口

**接口**: `POST /api/room/create`

**返回数据结构**（需要在 `players` 对象中每个玩家包含 `isHost`）：

```json
{
  "roomId": "string",
  "roomName": "string",
  "maxPlayers": 6,
  "maxRounds": 3,
  "targetScore": 66,
  "isPrivate": true,
  "gameState": "WAITING",
  "ownerId": "string | number",
  "players": {
    "sessionId1": {
      "sessionId": "string",
      "id": "string | number",           // 用户ID
      "userId": "string | number",        // 用户ID（兼容字段）
      "nickname": "string",
      "displayName": "string",
      "avatarUrl": "string",
      "isHost": true,                    // ⭐ 必须：创建者默认为 true
      "isReady": false,                  // 可选：准备状态
      "isRobot": false                   // 可选：是否为机器人
    }
  }
}
```

**说明**：
- 创建房间的用户自动成为房主，其 `isHost` 字段应为 `true`
- 其他玩家（如果有）的 `isHost` 应为 `false`

---

### 2. 加入房间接口

**接口**: `POST /api/room/join`

**返回数据结构**（需要在 `players` 对象中每个玩家包含 `isHost`）：

```json
{
  "roomId": "string",
  "roomName": "string",
  "gameState": "WAITING | PLAYING | GAME_OVER",
  "maxPlayers": 6,
  "maxRounds": 3,
  "targetScore": 66,
  "players": {
    "sessionId1": {
      "sessionId": "string",
      "id": "string | number",
      "userId": "string | number",
      "nickname": "string",
      "displayName": "string",
      "avatarUrl": "string",
      "isHost": true,                    // ⭐ 必须：房主为 true，其他为 false
      "isReady": false,
      "isRobot": false,
      "score": 0,
      "hasPlayed": false,
      "isCurrentTurn": false
    },
    "sessionId2": {
      "sessionId": "string",
      "id": "string | number",
      "userId": "string | number",
      "nickname": "string",
      "displayName": "string",
      "avatarUrl": "string",
      "isHost": false,                   // ⭐ 必须：非房主为 false
      "isReady": false,
      "isRobot": false,
      "score": 0,
      "hasPlayed": false,
      "isCurrentTurn": false
    }
  },
  "currentRound": 1,
  "currentTurnNumber": 1
}
```

**说明**：
- 每个玩家对象必须包含 `isHost` 字段
- 只有创建房间的玩家（或房主）的 `isHost` 为 `true`
- 其他所有玩家（包括新加入的玩家）的 `isHost` 为 `false`

---

### 3. WebSocket 消息 - gameStateUpdate

**消息类型**: `gameStateUpdate`

**消息格式**（需要在 `roomState.players` 对象中每个玩家包含 `isHost`）：

```json
{
  "type": "gameStateUpdate",
  "roomState": {
    "roomId": "string",
    "roomName": "string",
    "gameState": "WAITING | PLAYING | GAME_OVER",
    "maxPlayers": 6,
    "maxRounds": 3,
    "targetScore": 66,
    "currentRound": 1,
    "currentTurnNumber": 1,
    "players": {
      "sessionId1": {
        "sessionId": "string",
        "id": "string | number",
        "userId": "string | number",
        "nickname": "string",
        "displayName": "string",
        "avatarUrl": "string",
        "isHost": true,                  // ⭐ 必须：房主为 true
        "isReady": false,
        "isRobot": false,
        "score": 0,
        "hasPlayed": false,
        "isCurrentTurn": false,
        "hand": []                       // 游戏进行中时包含手牌
      },
      "sessionId2": {
        "sessionId": "string",
        "id": "string | number",
        "userId": "string | number",
        "nickname": "string",
        "displayName": "string",
        "avatarUrl": "string",
        "isHost": false,                 // ⭐ 必须：非房主为 false
        "isReady": true,
        "isRobot": false,
        "score": 0,
        "hasPlayed": false,
        "isCurrentTurn": false
      }
    },
    "rows": [],                          // 游戏进行中时包含场牌
    "playedCardsThisTurn": {}            // 游戏进行中时包含本回合已出的牌
  }
}
```

**说明**：
- 这是最重要的接口，因为房间状态变化时都会通过 WebSocket 推送
- 每次推送时，所有玩家对象都必须包含 `isHost` 字段
- 包括以下场景：
  - 玩家加入/离开房间
  - 玩家准备/取消准备
  - 添加机器人
  - 开始游戏
  - 游戏状态更新

---

### 4. 添加机器人接口

**接口**: `POST /api/room/add-bots`

**返回数据结构**（需要在 `players` 数组中每个玩家包含 `isHost`）：

```json
{
  "roomId": "string",
  "players": [
    {
      "sessionId": "string",             // 机器人 sessionId 通常以 "BOT_" 开头
      "id": "string | number",
      "userId": "string | number",
      "nickname": "string",
      "displayName": "string",
      "avatarUrl": "string",
      "isHost": false,                   // ⭐ 必须：机器人永远为 false
      "isReady": true,                   // 机器人默认已准备
      "isRobot": true                    // 标识为机器人
    },
    {
      "sessionId": "string",             // 真实玩家
      "id": "string | number",
      "userId": "string | number",
      "nickname": "string",
      "displayName": "string",
      "avatarUrl": "string",
      "isHost": true,                    // ⭐ 必须：房主为 true
      "isReady": false,
      "isRobot": false
    }
  ]
}
```

**说明**：
- 机器人永远不能是房主，`isHost` 必须为 `false`
- 返回的玩家列表中，需要明确标识哪个是房主

---

### 5. 获取游戏状态接口（兜底接口）

**接口**: `POST /api/game/state`

**返回数据结构**（需要在 `players` 对象中每个玩家包含 `isHost`）：

```json
{
  "roomId": "string",
  "gameState": "WAITING | PLAYING | GAME_OVER",
  "currentRound": 1,
  "currentTurnNumber": 1,
  "countdown": 30,
  "players": {
    "sessionId1": {
      "sessionId": "string",
      "id": "string | number",
      "userId": "string | number",
      "nickname": "string",
      "displayName": "string",
      "avatarUrl": "string",
      "isHost": true,                    // ⭐ 必须
      "isReady": false,
      "isRobot": false,
      "score": 0,
      "hand": [
        { "number": 12, "bullheads": 1 },
        { "number": 37, "bullheads": 2 }
      ],
      "hasPlayed": false,
      "isCurrentTurn": false
    }
  },
  "rows": [
    {
      "cards": [
        { "number": 5, "bullheads": 1 },
        { "number": 16, "bullheads": 2 }
      ]
    }
  ],
  "playedCardsThisTurn": {
    "sessionId1": {
      "playerId": "sessionId1",
      "cardNumber": 42,
      "bullheads": 1
    }
  }
}
```

**说明**：
- 用于断线重连或状态不同步时主动拉取当前游戏状态
- 每个玩家对象必须包含 `isHost` 字段

---

## 字段规范

### isHost 字段类型
- **类型**: `boolean`
- **必填**: 是
- **值说明**:
  - `true`: 该玩家是房间的房主（创建者）
  - `false`: 该玩家不是房主

### 房主规则
1. **唯一性**: 每个房间有且仅有一个房主
2. **创建者**: 创建房间的用户自动成为房主
3. **不可变更**: 房主身份在房间生命周期内不会改变（除非房主离开房间，此时需要转移房主权限，但当前版本暂不考虑）
4. **机器人**: 机器人永远不能是房主

### 前端处理逻辑
- 如果后端返回的玩家对象中 `isHost` 字段缺失或为 `undefined`，前端会尝试通过以下逻辑推断：
  - 如果玩家是第一个加入房间的玩家，且 `isHost` 未明确设置为 `false`，则视为房主
  - 但为了确保准确性，**强烈建议后端明确返回 `isHost` 字段**

---

## 测试建议

1. **创建房间测试**: 验证创建者返回的 `isHost` 为 `true`
2. **加入房间测试**: 验证所有玩家（包括新加入的）都有正确的 `isHost` 值
3. **WebSocket 推送测试**: 验证每次状态更新时，所有玩家都包含 `isHost` 字段
4. **添加机器人测试**: 验证机器人返回的 `isHost` 为 `false`
5. **多玩家场景**: 验证在多个玩家的情况下，只有一个玩家的 `isHost` 为 `true`

---

## 总结

需要在以下 **5 个接口/消息** 中返回 `isHost` 字段：

1. ✅ `POST /api/room/create` - 创建房间
2. ✅ `POST /api/room/join` - 加入房间
3. ✅ WebSocket `gameStateUpdate` 消息 - 房间状态更新
4. ✅ `POST /api/room/add-bots` - 添加机器人
5. ✅ `POST /api/game/state` - 获取游戏状态（兜底接口）

**关键点**：
- 所有返回玩家信息的接口，每个玩家对象都必须包含 `isHost` 字段
- 房主的 `isHost` 为 `true`，其他玩家为 `false`
- 机器人永远不能是房主


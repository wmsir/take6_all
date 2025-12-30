## 接口文档（小程序后端 API 总览）

> 说明：本项目所有 HTTP 接口均通过 `api/request.js` 的 `request` 方法调用，统一处理鉴权与返回值。本文档按模块梳理所有已使用的接口及其入参与返回数据结构，作为前后端对齐依据。

---

## 一、通用约定

- **基础地址**
  - 实际请求地址：`app.globalData.baseUrl + url`

- **请求头**
  - `Content-Type: application/json`
  - `auth: <token>`（除声明 `needAuth: false` 的登录/验证码接口外，其余接口均需要）

- **请求方法**
  - 默认 `POST`，当前所有接口均为 `POST`

- **请求体**
  - 统一使用 JSON，即使无参数也传 `{}`。

- **统一响应格式**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    /* 具体业务数据 */
  }
}
```

- **code 约定**
  - `200`：成功，前端直接使用 `data`
  - `500`：系统异常，前端统一弹出“系统异常，请稍后重试”
  - `401`：未登录/登录失效，前端清除本地登录信息并跳转到登录页
  - 其它：业务异常，前端统一 `wx.showToast(message || '操作失败')`

---

## 二、认证模块（`api/authApi.js`）

### 1. 微信登录

- **接口说明**：通过 `wx.login` 获取的 `code` 与后端交互，换取用户身份与 token。
- **URL**：`POST /api/auth/wechat/login`
- **鉴权**：`needAuth: false`

#### 请求参数（Body）

```json
{
  "code": "string" // wx.login 返回的临时登录凭证
}
```

#### 返回数据 `data`

前端在 `pages/login/login.js` 中使用如下字段：

```json
{
  "id": "string | number",
  "nickname": "string",
  "avatarUrl": "string",
  "token": "string",
  "phone": "string (可选)",
  "registerTime": 1710000000000
}
```

- `token`：写入 `app.globalData.token` 与本地 `storage` 的登录凭证
- `id / nickname / avatarUrl`：写入 `app.globalData.userInfo` 及本地 `storage`
- `phone`：若存在，则视为已绑定手机号
- `registerTime`：用于“注册天数”统计（单位：毫秒时间戳，可选）

---

### 2. 获取手机验证码

- **接口说明**：向指定手机号发送短信验证码。
- **URL**：`POST /api/auth/phone/code`
- **鉴权**：`needAuth: false`

#### 请求参数（Body）

```json
{
  "phone": "string" // 11 位手机号
}
```

#### 返回数据 `data`

前端只关心是否成功，不读取字段；建议：

```json
{
  "expireIn": 60
}
```

- `expireIn`：验证码有效时间，单位秒（可选）

---

### 3. 绑定手机号

- **接口说明**：为当前登录用户绑定手机号。
- **URL**：`POST /api/auth/bind/phone`
- **鉴权**：需要登录

#### 请求参数（Body）

```json
{
  "phone": "string",
  "code": "string" // 短信验证码
}
```

#### 返回数据 `data`

前端只需知道成功/失败；建议返回绑定结果：

```json
{
  "phone": "string"
}
```

- 绑定成功后，前端会将 `phone` 写入本地 `userInfo`。

---

## 三、房间模块（`api/roomApi.js`）

### 1. 创建房间

- **接口说明**：创建一场新的对局房间。
- **URL**：`POST /api/room/create`
- **鉴权**：需要登录

#### 请求参数（Body）

```json
{
  "maxPlayers": 6,        // 最大玩家数
  "maxRounds": 3,         // 最大局数（可选）
  "targetScore": 66,      // 目标分数，超过则结束（可选）
  "isPrivate": true,      // 是否私密房间
  "password": "1234"      // 房间密码（isPrivate 为 true 时有效，可为空字符串）
}
```

#### 返回数据 `data`

建议返回完整房间信息（用于前端存储与后续显示）：

```json
{
  "roomId": "string",
  "roomName": "string",
  "maxPlayers": 6,
  "maxRounds": 3,
  "targetScore": 66,
  "isPrivate": true,
  "gameState": "WAITING",
  "ownerId": "string | number"
}
```

---

### 2. 加入房间

- **接口说明**：通过房间号加入指定房间。
- **URL**：`POST /api/room/join`
- **鉴权**：需要登录

#### 请求参数（Body）

```json
{
  "roomId": "string",
  "password": "string" // 可为空字符串
}
```

#### 返回数据 `data`

`pages/lobby/lobby.js` 中会将返回值整体保存为当前房间信息，建议结构与游戏状态保持一致核心字段：

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
      "nickname": "string",
      "displayName": "string",
      "avatarUrl": "string",
      "score": 0,
      "hasPlayed": false,
      "isCurrentTurn": false
    }
  },
  "currentRound": 1,
  "currentTurnNumber": 1
}
```

---

### 3. 获取房间列表

- **接口说明**：用于大厅展示当前可见的房间列表。
- **URL**：`POST /api/room/list`
- **鉴权**：需要登录

#### 请求参数（Body）

当前前端传入空对象 `{}`，可扩展分页过滤：

```json
{
  "page": 1,
  "pageSize": 20,
  "onlyAvailable": true
}
```

#### 返回数据 `data`

`pages/lobby/lobby.js` 中使用如下字段：

```json
{
  "list": [
    {
      "roomId": "string",
      "roomName": "string",
      "currentPlayers": 4,
      "maxPlayers": 6,
      "maxRounds": 3,      // 用于展示“最多 X 局”
      "targetScore": 66,   // 用于展示“超过 X 分结束”
      "gameState": "WAITING | PLAYING"
    }
  ],
  "total": 100
}
```

前端映射逻辑：

- `gameState === 'PLAYING'` → `statusText: '进行中'`
- 否则 → `statusText: '等待中'`

---

### 4. 离开房间

- **接口说明**：当前用户离开指定房间。
- **URL**：`POST /api/room/leave`
- **鉴权**：需要登录

#### 请求参数（Body）

```json
{
  "roomId": "string"
}
```

#### 返回数据 `data`

前端只关心成功/失败，可简单返回：

```json
{}
```

---

### 5. 准备游戏

- **接口说明**：在房间内标记“已准备”。
- **URL**：`POST /api/room/ready`
- **鉴权**：需要登录

#### 请求参数（Body）

```json
{
  "roomId": "string"
}
```

#### 返回数据 `data`

主要通过 WebSocket 推送最新房间状态，此处可简单返回：

```json
{}
```

---

### 6. 开始游戏

- **接口说明**：房主在所有人准备后发起开局。
- **URL**：`POST /api/room/start`
- **鉴权**：需要登录（且应在后端做房主校验）

#### 请求参数（Body）

```json
{
  "roomId": "string"
}
```

#### 返回数据 `data`

同样建议以 WebSocket 推送为主，此接口可返回当前房间快照或空对象：

```json
{}
```

---

## 四、游戏模块（`api/gameApi.js`）

> 实时对局过程采用 WebSocket 推送；HTTP 接口主要负责提交指令和兜底拉取状态。

### 1. 出牌

- **接口说明**：在自己的回合选择一张牌出牌。
- **URL**：`POST /api/game/play`
- **鉴权**：需要登录

#### 请求参数（Body）

```json
{
  "roomId": "string",
  "cardNumber": 42
}
```

#### 返回数据 `data`

出牌结果与新的房间状态通过 WebSocket 推送，此处可简单返回：

```json
{}
```

---

### 2. 选择收走某一行

- **接口说明**：当玩家所出的牌小于四行末尾所有牌时，强制选择收走一行。
- **URL**：`POST /api/game/take-row`
- **鉴权**：需要登录

#### 请求参数（Body）

```json
{
  "roomId": "string",
  "rowIndex": 0 // 行索引，从 0 开始
}
```

#### 返回数据 `data`

同样以 WebSocket 推送为主，可返回：

```json
{}
```

---

### 3. 获取游戏状态

- **接口说明**：兜底接口，用于断线重连或状态不同步时主动拉取当前游戏状态。
- **URL**：`POST /api/game/state`
- **鉴权**：需要登录

#### 请求参数（Body）

```json
{
  "roomId": "string"
}
```

#### 返回数据 `data`

建议与 WebSocket 推送的 `roomStateUpdate.data` 完全一致：

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
      "nickname": "string",
      "displayName": "string",
      "avatarUrl": "string",
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

---

## 五、用户模块（`api/userApi.js`）

### 1. 获取用户信息

- **接口说明**：获取当前登录用户的基础信息。
- **URL**：`POST /api/user/info`
- **鉴权**：需要登录

#### 请求参数（Body）

```json
{}
```

#### 返回数据 `data`

建议与登录接口返回结构保持一致，字段示例：

```json
{
  "id": "string | number",
  "nickname": "string",
  "avatarUrl": "string",
  "phone": "string",
  "registerTime": 1710000000000,
  "totalScore": 0
}
```

---

### 2. 获取用户战绩统计

- **接口说明**：用于“我的”页面展示累计战绩与数据统计。
- **URL**：`POST /api/user/stats`
- **鉴权**：需要登录

#### 请求参数（Body）

当前前端传 `{}`，可按需扩展筛选条件：

```json
{
  "from": "2024-01-01", // 可选，开始日期
  "to": "2024-12-31"    // 可选，结束日期
}
```

#### 返回数据 `data`

`pages/profile/profile.js` 中使用如下字段：

```json
{
  "totalGames": 23,          // 总对局数
  "winRate": 54,             // 胜率（百分数，0-100）
  "avgScore": 11.3,          // 平均猪头数
  "maxStreak": 4,            // 最高连胜
  "maxBullScore": 18,        // 单局最高猪头
  "minBullScore": 2,         // 单局最低猪头
  "commonOpponents": "小王 / 小李 / 小陈"
}
```

---

### 3. 获取历史战绩列表

- **接口说明**：获取最近若干场对局，用于绘制最近战绩条和胜平负统计。
- **URL**：`POST /api/user/history`
- **鉴权**：需要登录

#### 请求参数（Body）

```json
{
  "limit": 10 // 当前前端传 10，可扩展分页字段
}
```

#### 返回数据 `data`

`pages/profile/profile.js` 中使用如下字段：

```json
{
  "list": [
    {
      "score": 10,    // 当前用户在该局的猪头数（越小越好）
      "avgScore": 12, // 房间平均猪头，用于与 score 对比判定输赢
      "roomId": "A9K3",
      "createdAt": "2024-01-01 12:00:00",
      "rank": 1
    }
  ],
  "total": 100
}
```

前端根据 `score` 与 `avgScore` 的大小关系，转换为最近 10 场的 `win / draw / lose` 结果，用于 UI 展示。

---

## 六、错误与异常处理约定

- 所有接口需遵守统一返回格式与 `code` 语义，否则前端通用处理逻辑将失效。
- 业务错误尽量通过明确的 `message` 文案返回，前端会直接透传给用户。
- 登录失效统一返回 `code = 401`，由通用请求函数负责清理状态与跳转登录页。

---

如需扩展新接口，建议：

1. 在 `api/*.js` 中添加封装函数，复用 `request`。
2. 在本文件追加对应章节，保持“URL + 入参 + 返回 data 字段”三要素齐全，方便联调与排查。



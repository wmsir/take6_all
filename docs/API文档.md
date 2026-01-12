# 游戏配置管理 API 文档

## 基础信息

**Base URL**: `http://localhost:8088/api/game-config`

**认证**: 部分管理接口需要管理员权限（待实现）

**响应格式**: JSON

## API 端点

### 1. 获取所有游戏类型

获取系统支持的所有游戏类型枚举。

**端点**: `GET /api/game-config/types`

**权限**: 公开

**请求参数**: 无

**响应示例**:
```json
[
  {
    "code": "top_hog",
    "displayName": "谁是猪头王",
    "description": "经典牛头王游戏，避免收集牛头卡牌"
  },
  {
    "code": "example_game",
    "displayName": "示例游戏",
    "description": "这是一个示例游戏"
  }
]
```

**响应字段说明**:
- `code`: 游戏类型唯一标识符
- `displayName`: 游戏显示名称
- `description`: 游戏描述

---

### 2. 获取已启用的游戏列表

获取所有已启用的游戏配置，按显示顺序排序。

**端点**: `GET /api/game-config/enabled`

**权限**: 公开

**请求参数**: 无

**响应示例**:
```json
[
  {
    "id": 1,
    "gameTypeCode": "top_hog",
    "displayName": "谁是猪头王",
    "description": "经典牛头王游戏，避免收集牛头卡牌",
    "minPlayers": 2,
    "maxPlayers": 10,
    "enabled": true,
    "iconUrl": "/images/games/top_hog.png",
    "rulesDescription": "游戏目标：避免收集牛头卡牌...",
    "gameSpecificConfig": "{\"deckSize\":104,\"handSize\":10}",
    "displayOrder": 1,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
]
```

**响应字段说明**:
- `id`: 配置记录ID
- `gameTypeCode`: 游戏类型代码
- `displayName`: 游戏显示名称
- `description`: 游戏简短描述
- `minPlayers`: 最小玩家数
- `maxPlayers`: 最大玩家数
- `enabled`: 是否启用
- `iconUrl`: 游戏图标URL
- `rulesDescription`: 详细规则说明
- `gameSpecificConfig`: 游戏特定配置（JSON字符串）
- `displayOrder`: 显示顺序
- `createdAt`: 创建时间
- `updatedAt`: 更新时间

---

### 3. 获取特定游戏配置

根据游戏类型代码获取详细配置。

**端点**: `GET /api/game-config/{gameTypeCode}`

**权限**: 公开

**路径参数**:
- `gameTypeCode`: 游戏类型代码（如 `top_hog`）

**请求示例**:
```
GET /api/game-config/top_hog
```

**成功响应** (200 OK):
```json
{
  "id": 1,
  "gameTypeCode": "top_hog",
  "displayName": "谁是猪头王",
  "description": "经典牛头王游戏",
  "minPlayers": 2,
  "maxPlayers": 10,
  "enabled": true,
  "iconUrl": "/images/games/top_hog.png",
  "rulesDescription": "游戏目标：避免收集牛头卡牌，拥有最少牛头数的玩家获胜。\n\n游戏规则：\n1. 每位玩家获得10张手牌...",
  "gameSpecificConfig": "{\"deckSize\":104,\"handSize\":10,\"rowCount\":4,\"maxCardsPerRow\":5}",
  "displayOrder": 1,
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00"
}
```

**失败响应** (404 Not Found):
```json
{
  "error": "Not Found",
  "message": "游戏配置不存在"
}
```

---

### 4. 创建或更新游戏配置

创建新的游戏配置或更新已有配置。

**端点**: `POST /api/game-config`

**权限**: 管理员

**Content-Type**: `application/json`

**请求体**:
```json
{
  "gameTypeCode": "new_game",
  "displayName": "新游戏",
  "description": "这是一个新游戏",
  "minPlayers": 2,
  "maxPlayers": 6,
  "enabled": true,
  "iconUrl": "/images/games/new_game.png",
  "rulesDescription": "游戏规则详细说明...",
  "gameSpecificConfig": "{\"setting1\":\"value1\",\"setting2\":100}",
  "displayOrder": 10
}
```

**必填字段**:
- `gameTypeCode`: 游戏类型代码
- `displayName`: 显示名称
- `minPlayers`: 最小玩家数
- `maxPlayers`: 最大玩家数

**可选字段**:
- `description`: 描述
- `enabled`: 是否启用（默认true）
- `iconUrl`: 图标URL
- `rulesDescription`: 规则说明
- `gameSpecificConfig`: 游戏特定配置（JSON字符串）
- `displayOrder`: 显示顺序（默认0）

**成功响应** (200 OK):
```json
{
  "id": 2,
  "gameTypeCode": "new_game",
  "displayName": "新游戏",
  ...
}
```

**失败响应** (400 Bad Request):
```json
{
  "error": "Bad Request",
  "message": "参数验证失败"
}
```

---

### 5. 启用或禁用游戏

设置游戏的启用状态。

**端点**: `PUT /api/game-config/{gameTypeCode}/enabled`

**权限**: 管理员

**路径参数**:
- `gameTypeCode`: 游戏类型代码

**查询参数**:
- `enabled`: 布尔值，true启用，false禁用

**请求示例**:
```
PUT /api/game-config/top_hog/enabled?enabled=false
```

**成功响应** (200 OK):
```json
{
  "success": true,
  "gameTypeCode": "top_hog",
  "enabled": false
}
```

**失败响应** (404 Not Found):
```json
{
  "success": false,
  "gameTypeCode": "top_hog",
  "message": "游戏配置不存在"
}
```

---

### 6. 重新加载配置

触发配置热更新，重新加载所有游戏配置。

**端点**: `POST /api/game-config/reload`

**权限**: 管理员

**请求参数**: 无

**成功响应** (200 OK):
```json
{
  "message": "配置已重新加载"
}
```

**使用场景**:
- 修改配置文件后触发重载
- 更新数据库配置后同步到内存
- 定期刷新配置

---

### 7. 删除游戏配置

删除指定的游戏配置。

**端点**: `DELETE /api/game-config/{gameTypeCode}`

**权限**: 管理员

**路径参数**:
- `gameTypeCode`: 游戏类型代码

**请求示例**:
```
DELETE /api/game-config/old_game
```

**成功响应** (200 OK):
```json
{
  "success": true,
  "gameTypeCode": "old_game"
}
```

**失败响应** (404 Not Found):
```json
{
  "success": false,
  "gameTypeCode": "old_game",
  "message": "游戏配置不存在"
}
```

---

## 使用示例

### 场景1: 前端获取游戏列表

```javascript
// JavaScript/Vue示例
async function loadGames() {
  const response = await fetch('http://localhost:8088/api/game-config/enabled')
  const games = await response.json()
  
  games.forEach(game => {
    console.log(`${game.displayName}: ${game.minPlayers}-${game.maxPlayers}人`)
  })
}
```

### 场景2: 管理员添加新游戏

```bash
# curl示例
curl -X POST http://localhost:8088/api/game-config \
  -H "Content-Type: application/json" \
  -d '{
    "gameTypeCode": "poker",
    "displayName": "德州扑克",
    "description": "经典扑克游戏",
    "minPlayers": 2,
    "maxPlayers": 9,
    "enabled": true,
    "displayOrder": 3
  }'
```

### 场景3: 临时禁用某个游戏

```bash
# 禁用游戏
curl -X PUT 'http://localhost:8088/api/game-config/top_hog/enabled?enabled=false'

# 重新启用
curl -X PUT 'http://localhost:8088/api/game-config/top_hog/enabled?enabled=true'
```

### 场景4: 查看游戏详细配置

```javascript
// 获取特定游戏配置
async function getGameConfig(gameType) {
  const response = await fetch(`http://localhost:8088/api/game-config/${gameType}`)
  const config = await response.json()
  
  // 解析游戏特定配置
  const specificConfig = JSON.parse(config.gameSpecificConfig)
  console.log('牌堆大小:', specificConfig.deckSize)
  console.log('手牌数量:', specificConfig.handSize)
}
```

---

## 错误代码

| HTTP状态码 | 说明 | 可能原因 |
|-----------|------|---------|
| 200 | 成功 | 请求成功处理 |
| 400 | 错误请求 | 参数验证失败 |
| 401 | 未授权 | 需要登录 |
| 403 | 禁止访问 | 权限不足 |
| 404 | 未找到 | 资源不存在 |
| 500 | 服务器错误 | 服务器内部错误 |

---

## 数据验证规则

### gameTypeCode
- 格式: 小写字母和下划线
- 长度: 1-50字符
- 示例: `top_hog`, `poker_game`

### displayName
- 长度: 1-50字符
- 不能为空

### minPlayers / maxPlayers
- 类型: 整数
- 范围: 1-100
- 约束: minPlayers <= maxPlayers

### gameSpecificConfig
- 格式: 有效的JSON字符串
- 可以为空

---

## 配置热更新流程

```
1. 修改配置
   ↓
2. 调用 POST /api/game-config/reload
   ↓
3. 服务端重新加载配置
   ↓
4. 新配置立即生效
   ↓
5. 新创建的房间使用新配置
   ↓
6. 已存在的房间继续使用原配置
```

**注意事项**:
- 热更新不影响正在进行的游戏
- 修改配置后建议立即调用reload接口
- 配置变更会记录在日志中

---

## 最佳实践

### 1. 配置管理
- 在开发环境使用API动态管理配置
- 在生产环境通过配置文件或数据库管理
- 定期备份配置数据

### 2. 版本控制
- 将配置文件纳入版本控制
- 记录配置变更历史
- 测试环境先验证配置

### 3. 性能优化
- 启用的游戏列表可以缓存
- 使用CDN托管游戏图标
- 合理设置displayOrder减少排序开销

### 4. 安全考虑
- 管理接口需要权限验证
- 验证gameSpecificConfig的JSON格式
- 防止SQL注入和XSS攻击

---

## 相关文档

- [多游戏支持架构指南](./MULTI_GAME_GUIDE.md)
- [快速开始指南](./QUICK_START.md)
- [配置文件格式说明](./top_hog_server/src/main/resources/game_configs/README.md)

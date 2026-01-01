# 多游戏支持架构设计与实现指南

## 概述

本文档详细说明如何在"谁是猪头王"项目中实现多游戏支持，使系统能够通过配置热发布新游戏，而无需修改核心代码。

## 目录

1. [架构设计](#架构设计)
2. [核心组件](#核心组件)
3. [实现步骤](#实现步骤)
4. [新增游戏教程](#新增游戏教程)
5. [配置文件格式](#配置文件格式)
6. [API接口说明](#api接口说明)
7. [前端集成](#前端集成)
8. [测试验证](#测试验证)

---

## 架构设计

### 设计原则

1. **开放-封闭原则**：对扩展开放，对修改封闭
2. **依赖倒置原则**：依赖抽象而非具体实现
3. **单一职责原则**：每个组件只负责一个明确的功能
4. **配置驱动**：通过配置文件而非代码来控制游戏行为

### 架构层次

```
┌─────────────────────────────────────────┐
│          前端层 (Vue/WeChat)            │
│  - 游戏选择界面                          │
│  - 动态组件加载                          │
│  - 游戏特定UI组件                        │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│          API层 (REST/WebSocket)         │
│  - 游戏配置查询接口                      │
│  - 房间管理接口                          │
│  - 游戏逻辑接口                          │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│          服务层 (Service Layer)         │
│  - GameConfigurationService             │
│  - GameEngineFactory                    │
│  - GameRoomService                      │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│       游戏引擎层 (Game Engines)          │
│  - GameEngine (接口)                    │
│  - TopHogGameEngine (猪头王实现)         │
│  - [其他游戏引擎实现]                    │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│       数据层 (Data Layer)                │
│  - GameConfiguration (游戏配置)          │
│  - GameRoom (游戏房间)                   │
│  - 配置文件 (JSON)                       │
└─────────────────────────────────────────┘
```

---

## 核心组件

### 1. GameType (游戏类型枚举)

定义系统支持的所有游戏类型。

**位置**: `model/GameType.java`

```java
public enum GameType {
    TOP_HOG("top_hog", "谁是猪头王", "经典牛头王游戏"),
    // 添加新游戏时在这里扩展
}
```

### 2. GameConfiguration (游戏配置实体)

存储每种游戏的配置信息。

**位置**: `model/GameConfiguration.java`

**主要字段**:
- `gameTypeCode`: 游戏类型唯一标识
- `displayName`: 显示名称
- `description`: 游戏描述
- `minPlayers`/`maxPlayers`: 玩家数量范围
- `enabled`: 是否启用
- `gameSpecificConfig`: 游戏特定配置(JSON)
- `rulesDescription`: 规则说明

### 3. GameEngine (游戏引擎接口)

定义所有游戏引擎必须实现的标准接口。

**位置**: `service/game/GameEngine.java`

**核心方法**:
- `initializeGame()`: 初始化游戏
- `startNewRound()`: 开始新一轮
- `handlePlayerAction()`: 处理玩家动作
- `isGameOver()`: 判断游戏是否结束
- `calculateFinalScores()`: 计算最终得分
- `getGameStateForPlayer()`: 获取游戏状态

### 4. GameEngineFactory (游戏引擎工厂)

负责管理和提供不同类型的游戏引擎实例。

**位置**: `service/game/GameEngineFactory.java`

**功能**:
- 自动注入所有GameEngine实现
- 根据游戏类型返回对应引擎
- 支持运行时动态扩展

### 5. GameConfigurationService (配置服务)

管理游戏配置的CRUD操作。

**位置**: `service/GameConfigurationService.java`

**功能**:
- 初始化默认配置
- 查询、创建、更新、删除配置
- 支持配置热加载

### 6. GameConfigController (配置接口)

提供游戏配置的REST API。

**位置**: `controller/GameConfigController.java`

---

## 实现步骤

### 阶段一：后端核心架构 ✅ 已完成

1. ✅ 创建 `GameType` 枚举
2. ✅ 创建 `GameConfiguration` 实体
3. ✅ 创建 `GameEngine` 接口
4. ✅ 创建 `GameEngineFactory` 工厂类
5. ✅ 创建 `GameConfigurationRepository`
6. ✅ 创建 `GameConfigurationService`
7. ✅ 创建 `GameConfigController`
8. ✅ 更新 `GameRoom` 添加 `gameType` 字段

### 阶段二：TopHog游戏引擎实现

需要将现有的 `GameLogicService` 重构为 `TopHogGameEngine` 实现 `GameEngine` 接口。

**步骤**:
1. 创建 `TopHogGameEngine` 类
2. 实现 `GameEngine` 接口的所有方法
3. 将原有逻辑迁移到新引擎中
4. 保持与原有逻辑的兼容性

### 阶段三：服务层集成

1. 更新 `GameRoomService` 使用 `GameEngineFactory`
2. 修改 `GameWebSocketHandler` 支持多游戏
3. 更新房间创建逻辑，添加游戏类型选择

### 阶段四：前端适配

1. **Web前端** (`top_hog_web`):
   - 添加游戏选择页面
   - 实现动态组件加载
   - 创建游戏特定组件

2. **微信小程序** (`top_hog_miniprogram`):
   - 添加游戏选择界面
   - 更新房间创建页面
   - 支持多游戏页面切换

### 阶段五：测试与文档

1. 单元测试
2. 集成测试
3. 文档完善

---

## 新增游戏教程

### 完整流程

以添加一个名为"Example Game"的新游戏为例：

#### 步骤1: 添加游戏类型枚举

编辑 `GameType.java`:

```java
public enum GameType {
    TOP_HOG("top_hog", "谁是猪头王", "经典牛头王游戏"),
    EXAMPLE_GAME("example_game", "示例游戏", "这是一个示例游戏"),
}
```

#### 步骤2: 创建游戏引擎实现

创建 `ExampleGameEngine.java`:

```java
package com.example.top_hog_server.service.game;

import com.example.top_hog_server.model.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;

@Component
public class ExampleGameEngine implements GameEngine {
    
    @Override
    public GameType getGameType() {
        return GameType.EXAMPLE_GAME;
    }
    
    @Override
    public void initializeGame(GameRoom room) {
        // 实现游戏初始化逻辑
        // 例如：初始化牌堆、设置游戏状态等
    }
    
    @Override
    public void startNewRound(GameRoom room) {
        // 实现新一轮游戏开始逻辑
        // 例如：发牌、重置计时器等
    }
    
    @Override
    public boolean handlePlayerAction(GameRoom room, WebSocketSession session, 
                                     String action, Map<String, Object> data) {
        // 实现玩家动作处理逻辑
        switch (action) {
            case "play_card":
                // 处理出牌
                return true;
            case "pass":
                // 处理跳过
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public boolean isGameOver(GameRoom room) {
        // 实现游戏结束判断逻辑
        return false;
    }
    
    @Override
    public Map<String, Integer> calculateFinalScores(GameRoom room) {
        // 实现分数计算逻辑
        Map<String, Integer> scores = new HashMap<>();
        // ... 计算每个玩家的分数
        return scores;
    }
    
    @Override
    public Map<String, Object> getGameStateForPlayer(GameRoom room, Player forPlayer) {
        // 实现游戏状态序列化逻辑
        Map<String, Object> state = new HashMap<>();
        state.put("gameType", getGameType().getCode());
        state.put("gameState", room.getGameState());
        // ... 添加更多状态信息
        return state;
    }
    
    @Override
    public void cleanupGame(GameRoom room) {
        // 实现游戏资源清理逻辑
    }
    
    @Override
    public boolean validatePlayerAction(GameRoom room, Player player, 
                                        String action, Map<String, Object> data) {
        // 实现动作验证逻辑
        return true;
    }
    
    @Override
    public void handlePlayerDisconnect(GameRoom room, Player player) {
        // 实现玩家断线处理逻辑
    }
    
    @Override
    public void handlePlayerReconnect(GameRoom room, Player player) {
        // 实现玩家重连处理逻辑
    }
    
    @Override
    public GameConfiguration getGameConfiguration() {
        // 返回游戏配置（可从数据库或配置文件加载）
        return null;
    }
}
```

#### 步骤3: 创建游戏配置

通过API或数据库直接插入配置：

**方式1：使用API**

```bash
POST /api/game-config
Content-Type: application/json

{
  "gameTypeCode": "example_game",
  "displayName": "示例游戏",
  "description": "这是一个示例游戏",
  "minPlayers": 2,
  "maxPlayers": 6,
  "enabled": true,
  "displayOrder": 2,
  "rulesDescription": "游戏规则说明...",
  "gameSpecificConfig": "{\"setting1\": \"value1\", \"setting2\": 123}"
}
```

**方式2：在Service中初始化**

在 `GameConfigurationService.initializeDefaultConfigurations()` 中添加：

```java
if (!configRepository.existsByGameTypeCode(GameType.EXAMPLE_GAME.getCode())) {
    GameConfiguration config = new GameConfiguration();
    config.setGameTypeCode(GameType.EXAMPLE_GAME.getCode());
    config.setDisplayName("示例游戏");
    // ... 设置其他字段
    configRepository.save(config);
}
```

#### 步骤4: 前端添加游戏UI组件

**Web前端**:

创建 `src/views/games/ExampleGame.vue`:

```vue
<template>
  <div class="example-game">
    <h1>{{ roomInfo.roomName }}</h1>
    <!-- 游戏特定UI -->
  </div>
</template>

<script>
export default {
  name: 'ExampleGame',
  data() {
    return {
      roomInfo: {},
      // 游戏特定数据
    }
  },
  methods: {
    // 游戏特定方法
  }
}
</script>
```

**微信小程序**:

创建 `pages/example_game/example_game.js` 和对应的 `.wxml`, `.wxss` 文件。

#### 步骤5: 测试新游戏

1. 启动后端服务
2. 检查游戏配置是否正确初始化
3. 测试创建新游戏类型的房间
4. 测试游戏逻辑

#### 步骤6: 热发布（无需重启）

如果只修改配置而不修改代码：

```bash
# 更新配置
PUT /api/game-config/example_game

# 重新加载配置
POST /api/game-config/reload
```

---

## 配置文件格式

### 游戏配置JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "gameTypeCode": {
      "type": "string",
      "description": "游戏类型唯一标识"
    },
    "displayName": {
      "type": "string",
      "description": "游戏显示名称"
    },
    "description": {
      "type": "string",
      "description": "游戏简短描述"
    },
    "minPlayers": {
      "type": "integer",
      "minimum": 1,
      "description": "最小玩家数"
    },
    "maxPlayers": {
      "type": "integer",
      "minimum": 1,
      "description": "最大玩家数"
    },
    "enabled": {
      "type": "boolean",
      "description": "是否启用"
    },
    "iconUrl": {
      "type": "string",
      "description": "游戏图标URL"
    },
    "rulesDescription": {
      "type": "string",
      "description": "游戏规则详细说明"
    },
    "gameSpecificConfig": {
      "type": "string",
      "description": "游戏特定配置（JSON字符串）"
    },
    "displayOrder": {
      "type": "integer",
      "description": "显示顺序"
    }
  },
  "required": ["gameTypeCode", "displayName", "minPlayers", "maxPlayers"]
}
```

### Top Hog游戏特定配置示例

```json
{
  "deckSize": 104,
  "handSize": 10,
  "rowCount": 4,
  "maxCardsPerRow": 5,
  "defaultMaxRounds": 3,
  "defaultTargetScore": 66,
  "playerChoiceTimeoutMs": 30000,
  "specialCards": [
    {"number": 55, "bullheads": 7},
    {"multiples": [11], "bullheads": 5},
    {"multiples": [10], "bullheads": 3},
    {"multiples": [5], "bullheads": 2}
  ]
}
```

---

## API接口说明

### 1. 获取所有游戏类型

**请求**:
```
GET /api/game-config/types
```

**响应**:
```json
[
  {
    "code": "top_hog",
    "displayName": "谁是猪头王",
    "description": "经典牛头王游戏，避免收集牛头卡牌"
  }
]
```

### 2. 获取启用的游戏列表

**请求**:
```
GET /api/game-config/enabled
```

**响应**:
```json
[
  {
    "id": 1,
    "gameTypeCode": "top_hog",
    "displayName": "谁是猪头王",
    "description": "经典牛头王游戏",
    "minPlayers": 2,
    "maxPlayers": 10,
    "enabled": true,
    "displayOrder": 1,
    "rulesDescription": "游戏规则...",
    "gameSpecificConfig": "{...}",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
]
```

### 3. 获取特定游戏配置

**请求**:
```
GET /api/game-config/{gameTypeCode}
```

**响应**:
```json
{
  "id": 1,
  "gameTypeCode": "top_hog",
  "displayName": "谁是猪头王",
  ...
}
```

### 4. 创建/更新游戏配置

**请求**:
```
POST /api/game-config
Content-Type: application/json

{
  "gameTypeCode": "new_game",
  "displayName": "新游戏",
  "minPlayers": 2,
  "maxPlayers": 4,
  ...
}
```

**响应**:
```json
{
  "id": 2,
  "gameTypeCode": "new_game",
  ...
}
```

### 5. 启用/禁用游戏

**请求**:
```
PUT /api/game-config/{gameTypeCode}/enabled?enabled=true
```

**响应**:
```json
{
  "success": true,
  "gameTypeCode": "top_hog",
  "enabled": true
}
```

### 6. 重新加载配置（热更新）

**请求**:
```
POST /api/game-config/reload
```

**响应**:
```json
{
  "message": "配置已重新加载"
}
```

### 7. 删除游戏配置

**请求**:
```
DELETE /api/game-config/{gameTypeCode}
```

**响应**:
```json
{
  "success": true,
  "gameTypeCode": "old_game"
}
```

---

## 前端集成

### Web前端 (Vue)

#### 1. 游戏选择页面

创建 `src/views/GameSelect.vue`:

```vue
<template>
  <div class="game-select">
    <h1>选择游戏</h1>
    <div class="game-list">
      <div v-for="game in availableGames" 
           :key="game.gameTypeCode"
           class="game-card"
           @click="selectGame(game)">
        <img :src="game.iconUrl || '/default-game-icon.png'" 
             :alt="game.displayName">
        <h3>{{ game.displayName }}</h3>
        <p>{{ game.description }}</p>
        <div class="game-info">
          <span>👥 {{ game.minPlayers }}-{{ game.maxPlayers }}人</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import api from '@/services/api'

export default {
  name: 'GameSelect',
  data() {
    return {
      availableGames: []
    }
  },
  async mounted() {
    await this.loadGames()
  },
  methods: {
    async loadGames() {
      try {
        const response = await api.get('/game-config/enabled')
        this.availableGames = response.data
      } catch (error) {
        console.error('加载游戏列表失败:', error)
      }
    },
    selectGame(game) {
      // 导航到房间创建页面，传递游戏类型
      this.$router.push({
        name: 'CreateRoom',
        query: { gameType: game.gameTypeCode }
      })
    }
  }
}
</script>
```

#### 2. 动态游戏组件加载

在 `src/views/Game.vue` 中:

```vue
<template>
  <component :is="gameComponent" 
             :room-id="roomId"
             :game-type="gameType"
             @leave="handleLeave" />
</template>

<script>
import TopHogGame from './games/TopHogGame.vue'
// 导入其他游戏组件

export default {
  name: 'Game',
  data() {
    return {
      roomId: null,
      gameType: 'top_hog'
    }
  },
  computed: {
    gameComponent() {
      const components = {
        'top_hog': TopHogGame,
        // 'example_game': ExampleGame,
      }
      return components[this.gameType] || TopHogGame
    }
  },
  mounted() {
    this.roomId = this.$route.params.roomId
    this.gameType = this.$route.query.gameType || 'top_hog'
  }
}
</script>
```

### 微信小程序

#### 1. 游戏选择页面

创建 `pages/game_select/game_select.js`:

```javascript
const gameApi = require('../../api/gameApi.js');

Page({
  data: {
    games: []
  },
  
  onLoad() {
    this.loadGames();
  },
  
  async loadGames() {
    try {
      const res = await gameApi.getEnabledGames();
      this.setData({
        games: res.data
      });
    } catch (error) {
      console.error('加载游戏列表失败', error);
    }
  },
  
  selectGame(e) {
    const gameType = e.currentTarget.dataset.gametype;
    wx.navigateTo({
      url: `/pages/room_create/room_create?gameType=${gameType}`
    });
  }
});
```

对应的 `game_select.wxml`:

```xml
<view class="game-select">
  <view class="game-list">
    <view class="game-card" 
          wx:for="{{games}}" 
          wx:key="gameTypeCode"
          data-gametype="{{item.gameTypeCode}}"
          bindtap="selectGame">
      <image src="{{item.iconUrl || '/images/default-game.png'}}" 
             mode="aspectFit" />
      <view class="game-title">{{item.displayName}}</view>
      <view class="game-desc">{{item.description}}</view>
      <view class="game-players">👥 {{item.minPlayers}}-{{item.maxPlayers}}人</view>
    </view>
  </view>
</view>
```

#### 2. API封装

在 `api/gameApi.js` 中添加:

```javascript
function getEnabledGames() {
  return request({
    url: '/api/game-config/enabled',
    method: 'GET'
  });
}

function getGameConfig(gameTypeCode) {
  return request({
    url: `/api/game-config/${gameTypeCode}`,
    method: 'GET'
  });
}

module.exports = {
  // ... 现有方法
  getEnabledGames,
  getGameConfig
};
```

---

## 测试验证

### 单元测试

#### 测试GameEngineFactory

```java
@SpringBootTest
class GameEngineFactoryTest {
    
    @Autowired
    private GameEngineFactory factory;
    
    @Test
    void testGetTopHogEngine() {
        GameEngine engine = factory.getEngine(GameType.TOP_HOG);
        assertNotNull(engine);
        assertEquals(GameType.TOP_HOG, engine.getGameType());
    }
    
    @Test
    void testIsSupported() {
        assertTrue(factory.isSupported(GameType.TOP_HOG));
    }
}
```

#### 测试GameConfigurationService

```java
@SpringBootTest
class GameConfigurationServiceTest {
    
    @Autowired
    private GameConfigurationService service;
    
    @Test
    void testGetEnabledGames() {
        List<GameConfiguration> games = service.getAllEnabledGames();
        assertFalse(games.isEmpty());
    }
    
    @Test
    void testGetGameConfiguration() {
        Optional<GameConfiguration> config = 
            service.getGameConfiguration(GameType.TOP_HOG.getCode());
        assertTrue(config.isPresent());
        assertEquals("谁是猪头王", config.get().getDisplayName());
    }
}
```

### 集成测试

#### 测试完整流程

```java
@SpringBootTest
@AutoConfigureMockMvc
class GameIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCreateRoomWithGameType() throws Exception {
        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomName\":\"测试房间\",\"gameType\":\"top_hog\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameType").value("TOP_HOG"));
    }
    
    @Test
    void testGetEnabledGames() throws Exception {
        mockMvc.perform(get("/api/game-config/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].gameTypeCode").exists());
    }
}
```

### 手动测试清单

- [ ] 启动后端服务，检查日志中的配置初始化信息
- [ ] 访问 `/api/game-config/enabled` 确认返回游戏列表
- [ ] 访问 `/api/game-config/top_hog` 确认返回详细配置
- [ ] 创建房间时指定游戏类型
- [ ] 进入游戏房间，确认游戏逻辑正常
- [ ] 更新游戏配置，调用reload接口
- [ ] 验证配置热更新生效

---

## 常见问题

### Q1: 如何添加新游戏类型？

A: 参考"新增游戏教程"章节，主要步骤：
1. 在GameType枚举中添加新类型
2. 实现GameEngine接口
3. 创建游戏配置
4. 添加前端UI组件

### Q2: 如何实现配置热更新？

A: 
1. 通过API更新配置: `POST /api/game-config`
2. 调用重载接口: `POST /api/game-config/reload`
3. 配置立即生效，无需重启服务

### Q3: 如何禁用某个游戏？

A: 调用API: `PUT /api/game-config/{gameType}/enabled?enabled=false`

### Q4: 游戏特定配置如何使用？

A: 在GameEngine实现中，通过getGameConfiguration()获取配置，解析gameSpecificConfig字段的JSON内容。

### Q5: 如何测试新游戏引擎？

A: 建议先编写单元测试，然后进行集成测试，最后在开发环境手动测试完整游戏流程。

---

## 未来扩展

### 可能的游戏类型

1. **狼人杀** (Werewolf)
   - 角色扮演类游戏
   - 需要语音/文字聊天支持
   
2. **德州扑克** (Texas Hold'em)
   - 纸牌类游戏
   - 需要筹码管理系统
   
3. **三国杀** (Sanguosha)
   - 角色扮演 + 纸牌
   - 复杂技能系统

4. **UNO**
   - 休闲纸牌游戏
   - 规则相对简单

### 架构优化方向

1. **插件系统**: 支持完全独立的游戏插件，无需修改主项目代码
2. **动态类加载**: 运行时加载新游戏引擎
3. **游戏脚本化**: 使用Lua或JavaScript定义游戏逻辑
4. **云配置**: 从云端拉取游戏配置和资源

---

## 总结

本多游戏支持架构通过以下设计实现了灵活的游戏扩展能力：

1. **接口抽象**: GameEngine接口统一了不同游戏的处理方式
2. **工厂模式**: GameEngineFactory管理游戏引擎实例
3. **配置驱动**: 通过数据库和配置文件控制游戏行为
4. **热更新**: 支持运行时修改配置，无需重启
5. **前后端分离**: 清晰的API接口，便于前端适配

通过这个架构，添加新游戏只需要：
- 实现GameEngine接口
- 添加游戏配置
- 创建前端UI组件

无需修改核心框架代码，实现了真正的开放-封闭原则。

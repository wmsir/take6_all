# 多游戏支持 - 快速开始指南

本指南帮助开发者快速上手，在现有项目中添加新游戏。

## 前置条件

- 已阅读 [MULTI_GAME_GUIDE.md](./MULTI_GAME_GUIDE.md) 了解架构设计
- 熟悉 Java Spring Boot 开发
- 了解项目现有代码结构

## 5分钟快速添加新游戏

### 步骤1: 添加游戏类型 (1分钟)

编辑 `model/GameType.java`，添加新游戏枚举：

```java
public enum GameType {
    TOP_HOG("top_hog", "谁是猪头王", "经典牛头王游戏"),
    MY_GAME("my_game", "我的游戏", "这是我的新游戏"), // 添加这一行
}
```

### 步骤2: 创建游戏引擎 (2分钟)

复制 `service/game/ExampleGameEngine.java` 并重命名为 `MyGameEngine.java`：

```bash
cd top_hog_server/src/main/java/com/example/top_hog_server/service/game/
cp ExampleGameEngine.java MyGameEngine.java
```

修改关键部分：

```java
@Component // 取消注释以启用
public class MyGameEngine implements GameEngine {
    
    @Override
    public GameType getGameType() {
        return GameType.MY_GAME; // 修改为你的游戏类型
    }
    
    // 实现其他方法...
}
```

### 步骤3: 创建配置文件 (1分钟)

复制配置文件模板：

```bash
cd top_hog_server/src/main/resources/game_configs/
cp example_game.json my_game.json
```

编辑 `my_game.json`：

```json
{
  "gameTypeCode": "my_game",
  "displayName": "我的游戏",
  "description": "游戏描述",
  "minPlayers": 2,
  "maxPlayers": 6,
  "enabled": true,
  "displayOrder": 2
}
```

### 步骤4: 初始化配置 (1分钟)

在 `GameConfigurationService.java` 的 `initializeDefaultConfigurations()` 方法中添加：

```java
if (!configRepository.existsByGameTypeCode(GameType.MY_GAME.getCode())) {
    GameConfiguration config = new GameConfiguration();
    config.setGameTypeCode(GameType.MY_GAME.getCode());
    config.setDisplayName("我的游戏");
    config.setDescription("游戏描述");
    config.setMinPlayers(2);
    config.setMaxPlayers(6);
    config.setEnabled(true);
    config.setDisplayOrder(2);
    configRepository.save(config);
}
```

### 步骤5: 启动测试

```bash
cd top_hog_server
mvn spring-boot:run
```

访问 http://localhost:8088/api/game-config/enabled 查看游戏是否已注册。

## 详细开发流程

### 1. 实现游戏逻辑

在 `MyGameEngine.java` 中实现所有 GameEngine 接口方法：

#### 核心方法优先级：

**必须实现（高优先级）**:
- `initializeGame()` - 游戏初始化
- `startNewRound()` - 开始新回合
- `handlePlayerAction()` - 处理玩家动作
- `getGameStateForPlayer()` - 获取游戏状态

**重要实现（中优先级）**:
- `isGameOver()` - 判断游戏结束
- `calculateFinalScores()` - 计算得分
- `validatePlayerAction()` - 验证动作合法性

**可选实现（低优先级）**:
- `handlePlayerDisconnect()` - 处理断线（默认托管）
- `handlePlayerReconnect()` - 处理重连
- `cleanupGame()` - 清理资源

### 2. 定义游戏动作

在 `handlePlayerAction()` 中定义游戏支持的所有动作：

```java
@Override
public boolean handlePlayerAction(GameRoom room, WebSocketSession session, 
                                 String action, Map<String, Object> data) {
    switch (action) {
        case "start_game":
            return handleStartGame(room, session);
        case "play_card":
            return handlePlayCard(room, session, data);
        case "end_turn":
            return handleEndTurn(room, session);
        // 添加更多动作...
        default:
            return false;
    }
}
```

### 3. 设计游戏状态

在 `getGameStateForPlayer()` 中定义返回给客户端的状态结构：

```java
@Override
public Map<String, Object> getGameStateForPlayer(GameRoom room, Player forPlayer) {
    Map<String, Object> state = new HashMap<>();
    
    // 基础信息
    state.put("gameType", "my_game");
    state.put("gameState", room.getGameState().toString());
    
    // 玩家信息
    state.put("players", getPlayersInfo(room));
    
    // 当前玩家的私有信息
    if (forPlayer != null) {
        state.put("myHand", forPlayer.getHand());
        state.put("myScore", forPlayer.getScore());
    }
    
    // 游戏公共信息
    state.put("board", getBoardState(room));
    state.put("currentPlayer", getCurrentPlayer(room));
    
    return state;
}
```

### 4. 配置游戏特定参数

在配置文件的 `gameSpecificConfig` 中添加游戏特定参数：

```json
{
  "gameSpecificConfig": {
    "deckSize": 52,
    "handSize": 7,
    "turnTimeLimit": 30,
    "pointsToWin": 100,
    "customRule1": true,
    "customRule2": "value"
  }
}
```

在代码中读取配置：

```java
GameConfiguration config = getGameConfiguration();
String configJson = config.getGameSpecificConfig();
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> gameConfig = mapper.readValue(configJson, Map.class);
int deckSize = (int) gameConfig.get("deckSize");
```

## 前端集成

### Web前端 (Vue)

创建游戏组件 `src/views/games/MyGame.vue`：

```vue
<template>
  <div class="my-game">
    <h1>{{ roomInfo.roomName }}</h1>
    
    <!-- 游戏UI -->
    <div class="game-board">
      <!-- 棋盘/牌桌/游戏区域 -->
    </div>
    
    <div class="player-area">
      <!-- 玩家手牌/操作区 -->
    </div>
    
    <div class="controls">
      <button @click="performAction">执行动作</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MyGame',
  props: ['roomId', 'gameType'],
  data() {
    return {
      roomInfo: {},
      gameState: {},
      ws: null
    }
  },
  mounted() {
    this.connectWebSocket()
  },
  methods: {
    connectWebSocket() {
      this.ws = new WebSocket(`ws://localhost:8088/ws-game`)
      this.ws.onmessage = this.handleMessage
    },
    handleMessage(event) {
      const message = JSON.parse(event.data)
      if (message.type === 'game_state') {
        this.gameState = message.data
      }
    },
    performAction() {
      const action = {
        type: 'player_action',
        action: 'play_card',
        data: { cardId: 1 }
      }
      this.ws.send(JSON.stringify(action))
    }
  }
}
</script>
```

### 微信小程序

创建游戏页面 `pages/my_game/my_game.js`：

```javascript
Page({
  data: {
    roomId: null,
    gameState: {}
  },
  
  onLoad(options) {
    this.setData({ roomId: options.roomId })
    this.connectWebSocket()
  },
  
  connectWebSocket() {
    wx.connectSocket({
      url: 'ws://localhost:8088/ws-game'
    })
    
    wx.onSocketMessage((res) => {
      const message = JSON.parse(res.data)
      this.handleMessage(message)
    })
  },
  
  handleMessage(message) {
    if (message.type === 'game_state') {
      this.setData({ gameState: message.data })
    }
  },
  
  performAction() {
    const action = {
      type: 'player_action',
      action: 'play_card',
      data: { cardId: 1 }
    }
    wx.sendSocketMessage({
      data: JSON.stringify(action)
    })
  }
})
```

## 测试清单

### 单元测试

创建 `MyGameEngineTest.java`：

```java
@SpringBootTest
class MyGameEngineTest {
    
    @Autowired
    private GameEngineFactory factory;
    
    @Test
    void testGetEngine() {
        GameEngine engine = factory.getEngine(GameType.MY_GAME);
        assertNotNull(engine);
        assertEquals(GameType.MY_GAME, engine.getGameType());
    }
    
    @Test
    void testInitializeGame() {
        GameEngine engine = factory.getEngine(GameType.MY_GAME);
        GameRoom room = new GameRoom("test-room", "Test Room");
        room.setGameType(GameType.MY_GAME);
        
        engine.initializeGame(room);
        
        assertEquals(GameState.WAITING, room.getGameState());
    }
}
```

### 手动测试

1. **配置测试**
   - [ ] GET `/api/game-config/enabled` - 确认游戏在列表中
   - [ ] GET `/api/game-config/my_game` - 获取游戏配置

2. **房间创建测试**
   - [ ] POST `/api/rooms` - 创建新游戏房间
   - [ ] 验证房间的 `gameType` 字段正确

3. **WebSocket测试**
   - [ ] 连接到房间
   - [ ] 发送玩家动作
   - [ ] 接收游戏状态更新

4. **完整游戏流程测试**
   - [ ] 多个玩家加入
   - [ ] 开始游戏
   - [ ] 执行游戏动作
   - [ ] 游戏结束并计分

## 常见问题

### Q: 游戏引擎没有被注册？
A: 确保类上有 `@Component` 注解，并且Spring能扫描到该包。

### Q: 如何调试游戏逻辑？
A: 在关键方法中添加日志：
```java
private static final Logger logger = LoggerFactory.getLogger(MyGameEngine.class);
logger.info("处理动作: {}, 数据: {}", action, data);
```

### Q: 如何处理并发问题？
A: GameRoomService 已经提供了房间锁机制，在处理房间数据时会自动加锁。

### Q: 如何添加自定义游戏状态？
A: 可以在 GameRoom 中使用 `@Transient` 字段存储游戏特定数据：
```java
@Transient
private Map<String, Object> gameSpecificData = new ConcurrentHashMap<>();
```

## 下一步

- 阅读 [MULTI_GAME_GUIDE.md](./MULTI_GAME_GUIDE.md) 了解架构细节
- 参考 `GameLogicService.java` 了解 TopHog 游戏的完整实现
- 查看 `GameWebSocketHandler.java` 了解 WebSocket 消息处理

## 获取帮助

- 查看现有代码实现
- 阅读 JavaDoc 注释
- 参考单元测试示例

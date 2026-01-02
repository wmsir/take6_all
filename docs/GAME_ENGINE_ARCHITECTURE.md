# 游戏引擎架构与通用化方案 (Game Engine Architecture & Generalization)

## 1. 当前架构分析 (Current Architecture Analysis)
目前的 `Top Hog` 服务器是一个**专用型**游戏服务器。所有的游戏逻辑、规则判断、分值计算都紧密耦合在 `GameLogicService` 类中。

*   **优点**: 开发速度快，针对单一游戏（牛头王）逻辑清晰，易于调试。
*   **缺点**: 扩展性差。如果想增加一个新游戏（如“UNO”或“斗地主”），必须复制粘贴整套代码或编写大量 `if-else` 判断，无法通过简单的“配置”来实现。

## 2. 是否可以做成配置化游戏引擎？ (Can it be a Configurable Engine?)
**答案是肯定的。**
要实现通过配置来定义不同的游戏（如定义牌堆大小、出牌规则、胜利条件），我们需要将“核心引擎”与“游戏规则”分离。

## 3. 重构建议：通用游戏引擎设计 (Refactoring Proposal)

为了实现“配置化”，我们需要引入**策略模式 (Strategy Pattern)** 和 **数据驱动设计 (Data-Driven Design)**。

### 3.1 核心抽象层 (Core Abstractions)
我们需要定义一套通用的接口，不再局限于“猪头”或“行”。

1.  **`GameRuleStrategy` (游戏规则策略接口)**
    *   定义游戏的核心行为，所有具体游戏（牛头王、UNO等）都实现此接口。
    ```java
    public interface GameRuleStrategy {
        // 初始化牌堆
        List<Card> initializeDeck();
        // 初始发牌逻辑
        void dealInitialCards(GameRoom room);
        // 验证玩家出牌是否合法
        boolean validatePlay(Player player, Card card, GameContext context);
        // 处理出牌后的效果（计分、惩罚、下一位玩家等）
        TurnResult processTurn(GameRoom room, List<PlayedCard> playedCards);
        // 判断游戏是否结束
        boolean isGameOver(GameRoom room);
    }
    ```

2.  **`Card` (通用卡牌模型)**
    *   目前的 `Card` 类只包含 `number` 和 `bullheads`。
    *   **改进**: 增加 `suit` (花色/颜色), `rank` (点数), `attributes` (Map<String, Object> 用于存储“猪头数”、“功能牌效果”等动态属性)。

3.  **`GameState` (通用游戏状态)**
    *   目前的 `GameRoom` 强绑定了 `List<GameRow> rows`（4行牌）。
    *   **改进**: 使用更抽象的 `Board` 对象。
    *   `Board` 可以包含：`List<Stack>` (牌堆), `List<Row>` (牌列), `Map<Player, Zone>` (玩家区域)。
    *   对于“牛头王”，配置为“4个Row”；对于“斗地主”，配置为“3张底牌 + 玩家手牌区”。

### 3.2 配置文件驱动 (Configuration Driven)
一旦代码层面完成了抽象，就可以使用 JSON 或 YAML 文件来定义游戏。

**示例配置 (game_config_6nimmt.json):**
```json
{
  "gameName": "Top Hog",
  "deck": {
    "type": "numeric",
    "range": [1, 104],
    "attributes": [
      { "condition": "value == 55", "effect": { "bullheads": 7 } },
      { "condition": "value % 11 == 0", "effect": { "bullheads": 5 } }
    ]
  },
  "board": {
    "structure": "rows",
    "count": 4,
    "capacity": 5
  },
  "rules": {
    "handSize": 10,
    "maxScore": 66,
    "playStyle": "simultaneous_reveal" // 同时亮牌机制
  }
}
```

### 3.3 实施路线图 (Implementation Roadmap)

如果要将当前项目改造为通用引擎，建议分三步走：

1.  **阶段一：提取常量 (Extraction)**
    *   将 `GameLogicService` 中的硬编码数字（104, 66, 10, 4, 5）全部提取到 `GameConfiguration` 类中。
    *   **成果**: 可以在不改代码的情况下，通过修改 DB 或 Config 文件调整“牛头王”的参数（如改为 200 张牌、8 行）。

2.  **阶段二：接口抽象 (Abstraction)**
    *   创建 `IGameLogic` 接口。
    *   将现有的逻辑重命名为 `TopHogGameLogic` 并实现该接口。
    *   `GameRoom` 不再直接调用 `GameLogicService`，而是持有一个 `IGameLogic` 实例。
    *   **成果**: 可以在同一个服务器上同时运行不同规则的“牛头王”变种。

3.  **阶段三：多游戏支持 (Polymorphism)**
    *   引入新的游戏逻辑（如 `UnoGameLogic`）。
    *   改造 WebSocket 协议，使其支持通用的状态更新（不再只发送 `rows`，而是发送 `boardState`）。
    *   **成果**: 真正的通用游戏平台。

## 4. 总结
目前的 `Top Hog` 暂时不支持“配置新游戏”。它是一个高度定制的单款游戏服务器。
如果您希望它成为一个通用引擎，需要进行显著的架构重构（如上所述）。建议先从**阶段一**开始，将硬编码的规则参数化，以最低成本获得一定的灵活性。

# 游戏配置手册 (Game Configuration Guide)

## 1. 简介 (Introduction)
本文档详细介绍了当前《Top Hog》（谁是牛头王 / 6 nimmt!）服务器的配置方式。

## 2. 基础配置 (Basic Configuration)
服务器的基础配置位于 `top_hog_server/src/main/resources/application.properties` 文件中。

### 2.1 服务器与网络
*   `server.port`: 服务器监听端口（默认 `8088`）。
*   `aliyun.oss.*`: 阿里云对象存储配置（用于头像等资源，如果未使用可忽略）。

### 2.2 数据库连接
*   `spring.datasource.url`: MySQL 数据库连接地址。
*   `spring.datasource.username`: 数据库用户名。
*   `spring.datasource.password`: 数据库密码。
*   *注意：当前配置指向一个远程测试数据库，生产环境请务必修改为本地或私有数据库。*

### 2.3 邮件服务 (Email)
*   `spring.mail.*`: 用于发送验证码或通知的 SMTP 服务器配置（默认使用 QQ 邮箱 SMTP）。

### 2.4 JWT 安全验证
*   `taskmanager.app.jwtSecret`: 用于加密用户 Token 的密钥。
*   `taskmanager.app.jwtExpirationMs`: Token 有效期（默认 24 小时）。

## 3. 游戏逻辑配置 (Game Logic Configuration)

目前绝大多数游戏规则逻辑是**硬编码**在 Java 代码中的，仅有少量参数可通过配置文件修改。

### 3.1 可配置参数 (application.properties)
*   `game.playerChoice.timeoutMs`: **玩家选择牌列的超时时间**。
    *   默认值: `30000` (30秒)。
    *   说明: 当玩家出的牌比所有牌列都小时，需要手动选择一行拿走。此参数控制玩家有多少时间进行选择，超时后服务器将自动选择猪头数最少的一行。

### 3.2 硬编码规则 (Hardcoded Rules)
以下规则直接编写在 `GameLogicService.java` 和相关 Model 类中，无法通过配置文件修改。如需修改，必须更改源代码并重新编译。

*   **牌堆 (Deck)**:
    *   总牌数: **104张** (1-104)。
    *   猪头数计算:
        *   55号: 7个猪头。
        *   11的倍数: 5个猪头。
        *   10的倍数: 3个猪头。
        *   5的倍数 (非10倍数): 2个猪头。
        *   其他: 1个猪头。
*   **游戏流程**:
    *   **初始手牌**: 每人 **10张**。
    *   **牌列数**: **4行**。
    *   **行容量**: 每行最多 **5张** (第6张导致拿牌)。
*   **胜利条件**:
    *   **66分**: 当任意玩家分数达到或超过 66 分时，游戏立即结束。
    *   **赢家**: 分数最低者获胜。

## 4. 如何修改硬编码规则 (Modifying Hardcoded Rules)

如果您需要修改上述硬编码规则，请参考以下代码位置：

1.  **修改牌堆与猪头数**:
    *   文件: `src/main/java/com/example/top_hog_server/service/GameLogicService.java`
    *   位置: `static { ... }` 静态初始化块。
2.  **修改手牌数量**:
    *   文件: `src/main/java/com/example/top_hog_server/service/GameLogicService.java`
    *   方法: `startGame` 和 `startNewRound` 中的循环次数 (`i < 10`)。
3.  **修改最大分数 (66分)**:
    *   文件: `src/main/java/com/example/top_hog_server/service/GameLogicService.java`
    *   方法: `finalizeTurn` 中的判断 `if (player.getScore() >= 66)`.
4.  **修改每行最大牌数 (5张)**:
    *   文件: `src/main/java/com/example/top_hog_server/model/GameRow.java` (推测) 或 `GameLogicService.java`
    *   查找: `MAX_CARDS_IN_ROW` 常量。

## 5. 常见问题
*   **Q: 如何让游戏支持 200 张牌？**
    *   A: 需要修改 `GameLogicService.java` 中的静态初始化块，增加循环上限，并定义新增牌的猪头数规则。
*   **Q: 能否把所有规则都变成配置项？**
    *   A: 可以，但这需要对游戏引擎进行架构重构。详情请参阅《游戏引擎架构与通用化方案文档》。

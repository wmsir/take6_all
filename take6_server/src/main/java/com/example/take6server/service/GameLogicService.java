// 文件位置: src/main/java/com/example/take6server/service/GameLogicService.java
package com.example.take6server.service;

import com.example.take6server.handler.GameWebSocketHandler;
// 确保这里导入了所有需要的模型类，比如 GameRoom, Player, Card, GameRow, GameState, User 等
import com.example.take6server.model.*;
import com.example.take6server.repository.UserRepository;
import com.example.take6server.repository.GameHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class GameLogicService {

    private static final Logger logger = LoggerFactory.getLogger(GameLogicService.class);
    private final GameRoomService gameRoomService;
    private final UserRepository userRepository;
    private final GameHistoryRepository gameHistoryRepository;
    // 处理WebSocket消息发送
    private final GameWebSocketHandler gameWebSocketHandler;
    // 用于安排定时任务，比如玩家选择超时
    private final TaskScheduler taskScheduler;
    // 存储玩家选择的定时器
    private final Map<String, ScheduledFuture<?>> choiceTimers = new ConcurrentHashMap<>();
    private final BotProfileService botProfileService;

    // 从配置文件读取玩家选择超时时间，默认30秒
    @Value("${game.playerChoice.timeoutMs:30000}")
    private long playerChoiceTimeoutMs;

    // 初始的完整牌堆
    private static final List<Card> INITIAL_FULL_DECK = new ArrayList<>();
    // 用于保证每个房间操作的线程安全
    private final Map<String, Lock> roomLocks = new ConcurrentHashMap<>();

    // 静态代码块，在类加载时初始化完整牌堆 (1到104张牌)
    static {
        for (int i = 1; i <= 104; i++) {
            // 默认牛头数为1
            int bullheads = 1;
            if (i == 55) {
                // 特殊牌：55号牌7个牛头
                bullheads = 7;
            } else if (i % 11 == 0) {
                // 11的倍数5个牛头
                bullheads = 5;
            } else if (i % 10 == 0) {
                // 10的倍数3个牛头
                bullheads = 3;
            } else if (i % 5 == 0 && i % 10 != 0) {
                // 5的倍数（且非10的倍数）2个牛头
                bullheads = 2;
            }
            INITIAL_FULL_DECK.add(new Card(i, bullheads));
        }
    }

    // 自动注入依赖的服务
    // @Lazy确保在循环依赖时能正确加载
    @Autowired
    public GameLogicService(GameRoomService gameRoomService,
                            UserRepository userRepository,
                            GameHistoryRepository gameHistoryRepository,
                            @Lazy GameWebSocketHandler gameWebSocketHandler,
                            TaskScheduler taskScheduler,
                            BotProfileService botProfileService) {
        this.gameRoomService = gameRoomService;
        this.userRepository = userRepository;
        this.gameHistoryRepository = gameHistoryRepository;
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.taskScheduler = taskScheduler;
        this.botProfileService = botProfileService;
    }

    // 获取指定房间的锁，如果不存在则创建一个新的
    private Lock getRoomLock(String roomId) {
        return roomLocks.computeIfAbsent(roomId, k -> new ReentrantLock());
    }

    // 向单个用户会话发送错误消息
    public void sendErrorToUserSession(WebSocketSession session, String roomId, String errorMessage) {
        if (session == null || !session.isOpen()) {
            logger.warn("尝试向一个null或已关闭的会话发送错误。");
            return;
        }
        Map<String, Object> errorPayload = new HashMap<>();
        // 消息类型为 "error"
        errorPayload.put("type", "error");
        // 错误信息内容
        errorPayload.put("message", errorMessage);
        if (roomId != null) {
            // 如果有房间ID，也附加上
            errorPayload.put("roomId", roomId);
        }

        try {
            gameWebSocketHandler.sendMessageToSession(session, errorPayload);
        } catch (IOException e) {
            logger.error("向会话 {} 发送错误时发生IOException: {}", session.getId(), e.getMessage(), e);
        }
    }

    // 向指定房间内的所有玩家广播错误消息
    public void sendErrorToRoom(String roomId, String errorMessage) {
        if (roomId == null) {
            logger.warn("尝试向一个null的roomId广播错误。");
            return;
        }
        Map<String, Object> errorPayload = new HashMap<>();
        // 消息类型为 "roomError"
        errorPayload.put("type", "roomError");
        errorPayload.put("message", errorMessage);
        errorPayload.put("roomId", roomId);

        try {
            gameWebSocketHandler.broadcastToRoom(roomId, errorPayload);
        } catch (IOException e) {
            logger.error("向房间 {} 广播错误时发生IOException: {}", roomId, e.getMessage(), e);
        }
    }

    // 向指定房间内的所有玩家广播当前游戏状态
    private void broadcastGameState(String roomId, String message, GameRoom room) {
        if (room == null) {
            logger.warn("尝试为roomId {} 广播游戏状态，但room对象为null。", roomId);
            return;
        }

        // 遍历房间中的所有玩家（会话），并发送个性化的状态
        for (String sessionId : room.getPlayers().keySet()) {
            Player p = room.getPlayers().get(sessionId);
            if (p == null) {
                continue;
            }

            // 如果是机器人玩家，不需要发送消息
            if (p.isRobot()) {
                continue;
            }

            // 构建个性化载荷
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "gameStateUpdate");
            payload.put("message", message);

            // 使用 GameRoomDTO 过滤数据
            String userIdStr = p.getUserId() != null ? String.valueOf(p.getUserId()) : null;

            com.example.take6server.payload.dto.response.GameRoomDTO dto = com.example.take6server.payload.dto.response.GameRoomDTO.from(room, userIdStr);
            payload.put("roomState", dto);

            WebSocketSession session = gameWebSocketHandler.getSessionById(sessionId);
            try {
                gameWebSocketHandler.sendMessageToSession(session, payload);
            } catch (IOException e) {
                 logger.error("Error sending state to session {}", sessionId, e);
            }
        }
    }

    // 开始新游戏（或一局游戏结束后的“再来一局”）
    public void startGame(String roomId, String instigatingPlayerSessionId) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            sendErrorToUserSession(gameWebSocketHandler.getSessionById(instigatingPlayerSessionId), roomId, "房间未找到。");
            return;
        }

        // 获取房间锁，开始操作
        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            // 检查游戏状态是否允许开始新游戏
            if (room.getGameState() != GameState.WAITING && room.getGameState() != GameState.GAME_OVER) {
                sendErrorToRoom(roomId, "游戏不处于可开始新游戏的状态 (当前: " + room.getGameState() + ")。");
                return;
            }

            // 检查玩家数量是否足够 (至少2名)
            // 修改: 放宽对纯人类玩家数量的限制，允许PvE模式 (如1人+5机器人)
            if (room.getPlayers().size() < 2) {
                sendErrorToRoom(roomId, "玩家数量不足 (至少需要2名)，无法开始新游戏。");
                return;
            }


            boolean allReadyForNewGame;
            // 如果是从“游戏结束”状态开始新游戏
            if (room.getGameState() == GameState.GAME_OVER) {
                // 只看未托管的玩家
                // 是否都请求了新游戏
                allReadyForNewGame = room.getPlayers().values().stream()
                        .filter(p -> !p.isTrustee())
                        .allMatch(Player::isRequestedNewGame);
                if (!allReadyForNewGame) {
                    sendErrorToRoom(roomId, "还有玩家未选择“再来一局”。");
                    return;
                }
            } else { // 如果是从“等待中”状态开始新游戏
                // 只看未托管的玩家
                // 是否都已准备
                allReadyForNewGame = room.getPlayers().values().stream()
                        .filter(p -> !p.isTrustee())
                        .allMatch(Player::isReady);
                if (!allReadyForNewGame) {
                    sendErrorToRoom(roomId, "还有玩家未准备好开始游戏。");
                    return;
                }
            }

            logger.info("在房间 {} 中开始一个全新的游戏。", roomId);
            // 重置房间状态以开始新游戏（分数等）
            room.resetForNewGame();

            // 创建新牌堆
            List<Card> newDeck = new ArrayList<>(INITIAL_FULL_DECK);
            // 洗牌
            Collections.shuffle(newDeck);
            room.setDeck(newDeck);
            logger.info("房间 {} 新游戏：牌堆已创建并洗牌，共 {} 张牌。", roomId, room.getDeck().size());

            // 清空桌面上的所有牌列
            room.clearAllRows();
            // 给每个牌列发一张起始牌
            for (GameRow gameRow : room.getRows()) {
                if (room.getDeck().isEmpty()) {
                    logger.error("严重错误：在房间 {} 新游戏初始化牌列时牌堆为空！", roomId);
                    sendErrorToRoom(roomId, "内部错误：牌堆在游戏初始化时为空！");
                    room.setGameState(GameState.GAME_OVER);
                    broadcastGameState(roomId,"游戏因错误未能开始。",room);
                    return;
                }
                // 从牌堆顶取一张牌
                gameRow.addCard(room.getDeck().remove(0));
            }
            logger.info("房间 {} 新游戏：初始4张牌已放置到牌列。", roomId);

            // 存储所有玩家手牌，供AI提示使用
            Map<String, List<Card>> allHandsForAI = new HashMap<>();
            //给每个玩家发10张手牌
            for (Player player : room.getPlayers().values()) {
                // 检查牌堆是否够发
                if (room.getDeck().size() < 10) {
                    logger.error("严重错误：房间 {} 新游戏发初始手牌时牌堆不足！", roomId);
                    sendErrorToRoom(roomId, "内部错误：牌堆不足以发初始手牌！");
                    room.setGameState(GameState.GAME_OVER);
                    broadcastGameState(roomId,"游戏因错误未能开始。",room);
                    return;
                }
                // 清空玩家之前的手牌
                player.getHand().clear();
                for (int i = 0; i < 10; i++) {
                    player.addCardToHand(room.getDeck().remove(0));
                }
                // 手牌排序
                player.getHand().sort(Comparator.comparingInt(Card::getNumber));
                // 记录手牌副本给AI
                allHandsForAI.put(player.getSessionId(), new ArrayList<>(player.getHand()));
            }
            // 更新房间内所有玩家手牌信息（AI用）
            room.setAllPlayerHandsForAI(allHandsForAI);

            logger.info("房间 {} 新游戏：已向 {} 位玩家发出初始10张手牌。", roomId, room.getPlayers().size());

            // 设置为第一轮
            room.setCurrentTurnNumber(1);
            // 设置游戏状态为“进行中”
            room.setGameState(GameState.PLAYING);
            broadcastGameState(roomId, "新游戏开始！第 1 轮，请出牌。", room);

            processBotTurnsAndCheckTurnCompletion(room);

        } finally {
            // 释放房间锁
            roomLock.unlock();
        }
    }

    // 开始新一轮（当上一轮10张牌出完后）
    private void startNewRound(GameRoom room) {
        logger.info("为房间 {} 开始新一轮 (当前已进行 {} 手)。", room.getRoomId(), room.getCurrentTurnNumber());

        // 重置每个玩家的与回合相关的状态
        for (Player player : room.getPlayers().values()) {
            player.resetForNewRound();
        }

        // 计算活跃玩家数量，用于判断牌堆是否足够
        long activePlayersCount = room.getPlayers().values().stream().filter(p -> !p.isTrustee() || room.getPlayers().size() == 1).count();
        // 如果没有活跃玩家但房间有人，则按总人数算（全托管局）
        if (activePlayersCount == 0 && !room.getPlayers().isEmpty()) {
            activePlayersCount = room.getPlayers().size();
        }

        // 检查牌堆剩余牌数是否足够开始新一轮
        if (room.getDeck().size() < (activePlayersCount * 10)) {
            logger.info("牌堆剩余牌数 ({}) 不足以在房间 {} 为 {} 位玩家开始新一轮。游戏结束。",
                    room.getDeck().size(), room.getRoomId(), activePlayersCount);
            // 设置游戏结束
            room.setGameState(GameState.GAME_OVER);
            // 决定最终赢家
            Player winner = determineWinner(room);
            String gameOverMsg = "牌堆的牌不足以开始新一轮！游戏结束。";
            if (winner != null && winner.getDisplayName() != null && !winner.getDisplayName().isEmpty()) {
                gameOverMsg += "最终赢家是: " + winner.getDisplayName() + " (" + winner.getScore() + "牛头)";
                room.setWinnerDisplayName(winner.getDisplayName());
            } else {
                // 没有明确赢家
                room.setWinnerDisplayName(null);
                logger.info("startNewRound (牌堆不足): 未找到明确赢家或赢家名称为空。");
            }
            // 广播游戏结束状态
            broadcastGameState(room.getRoomId(), gameOverMsg, room);
            // 保存历史记录
            saveGameHistory(room);
            return;
        }

        Map<String, List<Card>> allHandsForAI = room.getAllPlayerHandsForAI() != null ? room.getAllPlayerHandsForAI() : new HashMap<>();
        // 为每个玩家发新一轮的10张手牌
        for (Player player : room.getPlayers().values()) {
            player.getHand().clear();
            for (int i = 0; i < 10; i++) {
                if (!room.getDeck().isEmpty()) {
                    player.addCardToHand(room.getDeck().remove(0));
                } else { // 理论上前面已经检查过牌堆，这里是个保险
                    logger.error("严重错误：在房间 {} 新一轮发牌时牌堆意外耗尽！", room.getRoomId());
                    room.setGameState(GameState.GAME_OVER);
                    Player winnerOnDeckEmpty = determineWinner(room);
                    String errorGameOverMsg = "错误：发牌时牌堆意外耗尽！游戏结束。";
                    if (winnerOnDeckEmpty != null && winnerOnDeckEmpty.getDisplayName() != null && !winnerOnDeckEmpty.getDisplayName().isEmpty()) {
                        errorGameOverMsg += "最终赢家是: " + winnerOnDeckEmpty.getDisplayName() + " (" + winnerOnDeckEmpty.getScore() + "牛头)";
                        room.setWinnerDisplayName(winnerOnDeckEmpty.getDisplayName());
                    } else {
                        room.setWinnerDisplayName(null);
                    }
                    broadcastGameState(room.getRoomId(), errorGameOverMsg, room);
                    // 保存历史记录
                    saveGameHistory(room);
                    return;
                }
            }
            // 非托管玩家手牌排序
            if (!player.isTrustee()) {
                player.getHand().sort(Comparator.comparingInt(Card::getNumber));
            }
            // 更新AI用手牌信息
            allHandsForAI.put(player.getSessionId(), new ArrayList<>(player.getHand()));
        }
        room.setAllPlayerHandsForAI(allHandsForAI);

        logger.info("房间 {} 新一轮：已向 {} 位玩家发出10张手牌。牌堆剩余 {} 张。",
                room.getRoomId(), room.getPlayers().size(), room.getDeck().size());

        // 清空上一轮的出牌记录
        room.getPlayedCardsThisTurn().clear();
        // 新一轮从第1次出牌开始
        room.setCurrentTurnNumber(1);
        // 设置游戏状态为进行中
        room.setGameState(GameState.PLAYING);
        broadcastGameState(room.getRoomId(), "新一轮开始！第 1 次出牌。", room);
        processBotTurnsAndCheckTurnCompletion(room);
    }

    // 处理玩家主动请求离开房间 (HTTP/API)
    public void playerRequestsLeave(String roomId, Long userId) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            return;
        }

        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            // 查找玩家
            String sessionIdToRemove = null;
            Player leavingPlayer = null;
            for (Map.Entry<String, Player> entry : room.getPlayers().entrySet()) {
                if (entry.getValue().getUserId() != null && entry.getValue().getUserId().equals(userId)) {
                    sessionIdToRemove = entry.getKey();
                    leavingPlayer = entry.getValue();
                    break;
                }
            }

            if (leavingPlayer != null && sessionIdToRemove != null) {
                logger.info("玩家 {} (用户ID: {}) 明确请求离开房间 {}。", leavingPlayer.getDisplayName(), userId, roomId);
                // 标记明确离开
                leavingPlayer.setPendingLeave(true);

                // 执行移除
                performPlayerRemoval(room, sessionIdToRemove, leavingPlayer);
            }
        } finally {
            roomLock.unlock();
        }
    }

    // 执行实际的移除逻辑 (提取为公共方法)
    private void performPlayerRemoval(GameRoom room, String sessionId, Player player) {
        logger.info("将玩家 {} 从房间 {} 中移除。", player.getDisplayName(), room.getRoomId());
        room.getPlayers().remove(sessionId);
        room.getPlayedCardsThisTurn().remove(sessionId);
        room.getPlayersRequestedNewGame().remove(sessionId);
        if (room.getAllPlayerHandsForAI() != null) {
            room.getAllPlayerHandsForAI().remove(sessionId);
        }

        if (room.getPlayers().isEmpty()) {
            logger.info("房间 {} 已空，将被移除。", room.getRoomId());
            gameRoomService.removeRoom(room.getRoomId());
            roomLocks.remove(room.getRoomId());
        } else {
            broadcastGameState(room.getRoomId(), player.getDisplayName() + " 已离开房间。", room);
        }
    }

    // 处理玩家离开房间（通常是WebSocket连接断开时被调用）
    public void playerLeavesRaw(String roomId, String webSocketSessionId, Long userId) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            // 房间可能已经被清理了，这在断线时很常见，不做处理
            return;
        }
        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            Player leavingPlayer = room.getPlayers().get(webSocketSessionId);
            String logPlayerName = (leavingPlayer != null && leavingPlayer.getDisplayName() != null) ? leavingPlayer.getDisplayName() : String.valueOf(userId);

            if (leavingPlayer != null) {
                // 如果玩家已经标记为 pendingLeave，说明是明确请求离开，已经被 API 处理过移除，或者在此处移除
                if (leavingPlayer.isPendingLeave()) {
                    logger.info("玩家 {} 已标记为离开，确认移除连接。", logPlayerName);
                    performPlayerRemoval(room, webSocketSessionId, leavingPlayer);
                    return;
                }

                // 非明确离开（即意外断线）：
                // 无论是在等待还是游戏中，都不直接移除，而是转为托管/断线状态，等待重连
                logger.info("玩家 {} (会话: {}) 意外断开连接 (非主动离开)。转为托管/离线状态。", logPlayerName, webSocketSessionId);

                leavingPlayer.setTrustee(true);
                // 如果是在等待阶段，取消准备状态
                if (room.getGameState() == GameState.WAITING || room.getGameState() == GameState.GAME_OVER) {
                    leavingPlayer.setReady(false);
                    broadcastGameState(roomId, leavingPlayer.getDisplayName() + " 已断开连接 (等待重连)。", room);
                } else {
                    // 游戏中
                    broadcastGameState(roomId, leavingPlayer.getDisplayName() + " 已断开连接并进入托管模式。", room);
                    // 托管后自动出牌
                    processBotTurnsAndCheckTurnCompletion(room);
                }

            } else {
                // 玩家可能已经被移除了
                logger.debug("玩家离开处理：在房间 {} 中未找到会话ID {} 对应的玩家 (用户 {})。可能已移除。",
                        roomId, webSocketSessionId, userId);
            }
        } finally {
            roomLock.unlock();
        }
    }

    public void playerPlaysCard(String roomId, String sessionId, Long userIdentifier, int cardNumber) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
             // 如果通过 HTTP 调用，我们无法轻松发送 WS 错误。 记录下来。
             logger.error("Room not found: " + roomId);
             return;
        }
        WebSocketSession session = gameWebSocketHandler.getSessionById(sessionId);
        playerPlaysCardRaw(roomId, session, userIdentifier, cardNumber);
    }

    // 处理玩家出牌的原始请求
    public void playerPlaysCardRaw(String roomId, WebSocketSession webSocketSession, Long userIdentifier, int cardNumber) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            sendErrorToUserSession(webSocketSession, roomId, "房间未找到。");
            return;
        }

        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            String sessionId = webSocketSession != null ? webSocketSession.getId() : null;
            if (sessionId == null) {
                 // 尝试在内部调用时查找会话，如果会话为 null，但我们需要识别玩家
                 // 但是，调用者 playerPlaysCard 传递了 session。
                 // 如果 session 为 null（例如用户离线但 HTTP 被调用？不太可能），我们无法轻松继续。
                 logger.error("Session is null for playCard");
                 return;
            }
            Player player = room.getPlayers().get(sessionId);
            if (player == null) {
                sendErrorToUserSession(webSocketSession, roomId, "在此房间未找到该玩家。");
                return;
            }
            if (player.isTrustee()) {
                sendErrorToUserSession(webSocketSession, roomId, "您当前处于托管状态，无法手动出牌。");
                return;
            }
            if (room.getGameState() != GameState.PLAYING) {
                sendErrorToUserSession(webSocketSession, roomId, "当前不是出牌时间。");
                return;
            }
            if (room.getPlayedCardsThisTurn().containsKey(player.getSessionId())) {
                sendErrorToUserSession(webSocketSession, roomId, "您本轮已出过牌。");
                return;
            }

            // 检查玩家手牌中是否有这张牌
            Optional<Card> cardToPlayOpt = player.getHand().stream().filter(c -> c.getNumber() == cardNumber).findFirst();
            if (!cardToPlayOpt.isPresent()) {
                sendErrorToUserSession(webSocketSession, roomId, "卡牌 " + cardNumber + " 不在您的手牌中。");
                return;
            }

            Card cardToPlay = cardToPlayOpt.get();
            // 记录玩家本轮出的牌
            room.getPlayedCardsThisTurn().put(player.getSessionId(), cardToPlay);
            // 从手牌中移除
            player.removeCardFromHand(cardToPlay);
            // 如果是AI可见模式，也更新AI手牌记录
            if (room.getAllPlayerHandsForAI() != null && room.getAllPlayerHandsForAI().containsKey(player.getSessionId())) {
                room.getAllPlayerHandsForAI().get(player.getSessionId()).removeIf(c -> c.getNumber() == cardToPlay.getNumber());
            }

            broadcastGameState(roomId, player.getDisplayName() + " 已出牌。", room);
            // 检查是否所有人都出牌了
            processBotTurnsAndCheckTurnCompletion(room);

        } finally {
            roomLock.unlock();
        }
    }

    // 尝试为托管/机器人玩家出牌，并检查本回合是否已准备好进行处理（即所有人都已出牌）
    private void processBotTurnsAndCheckTurnCompletion(GameRoom room) {
        // 防止重复处理或在不当状态下处理
        if (room.getGameState() == GameState.PROCESSING_TURN || room.getGameState() == GameState.WAITING_FOR_PLAYER_CHOICE) {
            logger.debug("房间 {} 状态为 {}，跳过 processBotTurnsAndCheckTurnCompletion。", room.getRoomId(), room.getGameState());
            return;
        }
        // 游戏应该处于 PLAYING 状态
        if (room.getGameState() != GameState.PLAYING) {
            logger.debug("房间 {} 状态为 {} (非PLAYING)，跳过 processBotTurnsAndCheckTurnCompletion。", room.getRoomId(), room.getGameState());
            return;
        }

        boolean botsPlayed = false;
        logger.info("检查房间 {} 的托管玩家出牌情况。当前 {} 位玩家，已出牌 {} 位。",
                room.getRoomId(), room.getPlayers().size(), room.getPlayedCardsThisTurn().size());

        // 检查是否有活跃的人类玩家，以确定我们是否应该暂停为人类托管玩家自动出牌
        long activeHumans = room.getPlayers().values().stream().filter(p -> !p.isRobot() && !p.isTrustee()).count();
        long disconnectedHumans = room.getPlayers().values().stream().filter(p -> !p.isRobot() && p.isTrustee()).count();
        boolean shouldPauseForHumanTrustees = (activeHumans == 0 && disconnectedHumans > 0);

        if (shouldPauseForHumanTrustees) {
            logger.info("房间 {} 中无活跃人类玩家，将暂停为断线人类托管玩家自动出牌，等待重连。", room.getRoomId());
        }

        // 为所有未出牌的机器人或托管玩家自动出牌
        for (Player player : room.getPlayers().values()) {
            // 只要是机器人(isRobot)或者处于托管状态(isTrustee)，都应该自动出牌
            boolean shouldAutoPlay = player.isRobot() || player.isTrustee();

            // 特殊情况：如果所有人类都已断开连接，暂停他们的自动出牌以允许重连
            if (shouldPauseForHumanTrustees && !player.isRobot() && player.isTrustee()) {
                shouldAutoPlay = false;
            }

            boolean hasPlayed = room.getPlayedCardsThisTurn().containsKey(player.getSessionId());
            boolean hasHand = !player.getHand().isEmpty();

            if (shouldAutoPlay && !hasPlayed && hasHand) {
                logger.info("为玩家 {} (Robot: {}, Trustee: {}) 自动出牌。",
                        player.getDisplayName(), player.isRobot(), player.isTrustee());
                // 调用AI为托管玩家出牌
                aiPlayCardForTrustee(player, room);
                botsPlayed = true;
            } else if (shouldAutoPlay && !hasPlayed && !hasHand) {
                logger.warn("自动出牌玩家 {} (Robot: {}, Trustee: {}) 尚未出牌但手中无牌！可能状态异常。",
                        player.getDisplayName(), player.isRobot(), player.isTrustee());
            }
        }

        if (botsPlayed) {
            broadcastGameState(room.getRoomId(), "托管玩家已出牌。", room);
        }

        // 检查是否所有玩家（包括人类和机器人）都已出牌
        int playedCount = room.getPlayedCardsThisTurn().size();
        int totalCount = room.getPlayers().size();

        if (playedCount == totalCount) {
            logger.info("房间 {} 中所有玩家 ({}) 均已出牌。开始处理回合。", room.getRoomId(), totalCount);
            // 开始处理本回合所有已出的牌
            processTurnInternal(room);
        } else {
            logger.info("房间 {}: {}/{} 位玩家已出牌。等待其他玩家...", room.getRoomId(), playedCount, totalCount);
            // 找出还没出牌的玩家，记录日志以便排查
            for (Player p : room.getPlayers().values()) {
                if (!room.getPlayedCardsThisTurn().containsKey(p.getSessionId())) {
                    logger.info("等待玩家出牌: {} (托管: {}, 手牌数: {})",
                            p.getDisplayName(), p.isTrustee(), p.getHand().size());
                }
            }
        }
    }

    // 添加机器人到房间
    public void addBotsToRoom(String roomId, int botCount) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            return;
        }

        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            List<BotProfileService.BotProfile> profiles = botProfileService.getRandomBotProfiles(botCount);
            for (int i = 0; i < botCount; i++) {
                if (room.getPlayers().size() >= room.getMaxPlayers()) {
                    break;
                }

                BotProfileService.BotProfile profile = profiles.get(i % profiles.size());
                // 确保房间内名称唯一（如果可能的话，简单的冲突处理）
                String botName = profile.getName();
                if (room.getPlayers().values().stream().anyMatch(p -> p.getDisplayName().equals(profile.getName()))) {
                     botName = botName + "_" + (i+1);
                }

                String botSessionId = "BOT_" + roomId + "_" + UUID.randomUUID().toString().substring(0, 8);

                Player bot = new Player(botSessionId, botName);
                bot.setAvatarUrl(profile.getAvatarUrl());
                // 机器人始终是“托管”
                bot.setTrustee(true);
                // 机器人始终准备就绪
                bot.setReady(true);
                // 机器人永远不是房主
                bot.setHost(false);
                // 标记为机器人
                bot.setRobot(true);

                room.addPlayer(bot);
            }
        } finally {
            roomLock.unlock();
        }
    }

    // AI为托管玩家选择并打出一张牌（简单策略：出手中最小的牌）
    private void aiPlayCardForTrustee(Player trusteePlayer, GameRoom room) {
        if (trusteePlayer == null) {
            logger.error("aiPlayCardForTrustee called with null player.");
            return;
        }
        if (trusteePlayer.getHand() == null || trusteePlayer.getHand().isEmpty()) {
            logger.warn("托管玩家 {} (session: {}) 在房间 {} 手中无牌，无法自动出牌。",
                    trusteePlayer.getDisplayName(), trusteePlayer.getSessionId(), room.getRoomId());
            return;
        }
        // 托管AI出牌策略：出手中最小的牌
        //理论上不会是null，因为前面判断了手牌不为空
        Card cardToPlay = trusteePlayer.getHand().stream()
                .min(Comparator.comparingInt(Card::getNumber))
                .orElse(null);

        if (cardToPlay != null) {
            room.getPlayedCardsThisTurn().put(trusteePlayer.getSessionId(), cardToPlay);
            trusteePlayer.removeCardFromHand(cardToPlay);
            if (room.getAllPlayerHandsForAI() != null && room.getAllPlayerHandsForAI().containsKey(trusteePlayer.getSessionId())) {
                room.getAllPlayerHandsForAI().get(trusteePlayer.getSessionId()).removeIf(c -> c.getNumber() == cardToPlay.getNumber());
            }
            logger.info("托管玩家 {} 在房间 {} 自动打出牌: {}", trusteePlayer.getDisplayName(), room.getRoomId(), cardToPlay.getNumber());
        } else {
            logger.error("托管玩家 {} 在房间 {} 无法选择出牌（手牌可能意外为空）。", trusteePlayer.getDisplayName(), room.getRoomId());
        }
    }

    // 内部方法：处理一回合中所有玩家打出的牌
    private void processTurnInternal(GameRoom room) {
        // 各种边界条件检查
        if (room.getGameState() == GameState.GAME_OVER) { logger.info("processTurnInternal: 房间 {} 游戏已结束。", room.getRoomId()); return; }
        if (room.getPlayers().isEmpty()){ logger.info("processTurnInternal: 房间 {} 已空。", room.getRoomId()); return; }
        if (room.getGameState() == GameState.WAITING_FOR_PLAYER_CHOICE) {
            logger.info("processTurnInternal: 房间 {} 正在等待玩家 {} 选择。跳过正常回合处理。",
                    room.getRoomId(), room.getPlayerChoosingRowSessionId());
            return;
        }
        // 状态检查
        if (room.getGameState() != GameState.PLAYING && room.getGameState() != GameState.PROCESSING_TURN) {
            if(room.getPlayedCardsThisTurn().isEmpty() && (room.getGameState() == GameState.ROUND_OVER || room.getGameState() == GameState.WAITING)){
                logger.info("processTurnInternal: 房间 {} 回合已结束或等待中，且无人出牌。跳过处理。", room.getRoomId()); return;
            }
            logger.warn("processTurnInternal 为房间 {} 调用时状态异常: {}，但尝试继续处理。", room.getRoomId(), room.getGameState());
        }

        // 将状态设为“正在处理回合”
        room.setGameState(GameState.PROCESSING_TURN);
        // 获取本轮所有玩家出的牌，并按牌的数字从小到大排序
        List<Map.Entry<String, Card>> sortedPlayedCards = room.getPlayedCardsThisTurn().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(Card::getNumber)))
                .collect(Collectors.toList());

        if (sortedPlayedCards.isEmpty() && !room.getPlayers().isEmpty() && room.getGameState() != GameState.ROUND_OVER){
            logger.warn("房间 {} 在处理回合时发现没有玩家出牌，但房间内有玩家。恢复到PLAYING。", room.getRoomId());
            room.setGameState(GameState.PLAYING);
            broadcastGameState(room.getRoomId(), "回合处理异常，请重新出牌。", room);
            return;
        }
        if(sortedPlayedCards.isEmpty()) {
            logger.info("房间 {} 没有牌需要处理（可能所有玩家已离开），结束回合。", room.getRoomId());
            finalizeTurn(room);
            return;
        }

        // 将排序后的牌存起来，准备逐张处理
        room.setCardsRemainingInTrick(new ArrayList<>(sortedPlayedCards));
        // 开始处理第一张牌
        processNextCardInTrick(room);
    }

    // 逐张处理本轮打出的牌（按从小到大的顺序）
    private void processNextCardInTrick(GameRoom room) {
        // 确保线程安全
        Lock roomLock = getRoomLock(room.getRoomId());
        roomLock.lock();
        try {
            // 如果当前是在等某个玩家选择牌列，则暂停处理其他牌
            if (room.getGameState() == GameState.WAITING_FOR_PLAYER_CHOICE) {
                logger.debug("房间 {} 正在等待玩家选择，processNextCardInTrick 暂停。", room.getRoomId());
                return;
            }

            List<Map.Entry<String, Card>> cardsToProcess = room.getCardsRemainingInTrick();
            // 如果所有牌都处理完了
            if (cardsToProcess == null || cardsToProcess.isEmpty()) {
                logger.info("房间 {} 第 {} 轮的所有牌已处理完毕。", room.getRoomId(), room.getCurrentTurnNumber());
                // 清空本轮出牌记录
                room.getPlayedCardsThisTurn().clear();
                // 结束本回合，判断游戏是否结束或开始下一轮/回合
                finalizeTurn(room);
                return;
            }

            // 取出当前要处理的最小的牌
            Map.Entry<String, Card> entry = cardsToProcess.remove(0);
            String playerSessionId = entry.getKey();
            Card playedCard = entry.getValue();
            Player currentPlayer = room.getPlayers().get(playerSessionId);

            // 如果找不到出牌的玩家（理论上不应该发生）
            if (currentPlayer == null) {
                logger.error("processNextCardInTrick: 在房间 {} 未找到会话 {} 对应的玩家。", room.getRoomId(), playerSessionId);
                // 继续处理下一张牌
                processNextCardInTrick(room);
                return;
            }

            String playerDisplayNameForLog = currentPlayer.getDisplayName() + (currentPlayer.isTrustee() ? " (托管)" : "");
            logger.info("房间 {} - 处理玩家 {} 的牌 {}", room.getRoomId(), playerDisplayNameForLog, playedCard.getNumber());

            // 寻找这张牌应该放置到哪个牌列
            // 目标牌列的索引
            int targetRowIndex = -1;
            // 牌是否可以正常放置（即大于某行的最后一张牌）
            boolean cardCanBePlacedNormally = false;
            // 记录最小的差值，用于选择最接近的牌列
            int bestDifference = Integer.MAX_VALUE;

            for (int i = 0; i < room.getRows().size(); i++) {
                GameRow currentRow = room.getRows().get(i);
                Card lastCardInRow = currentRow.getLastCard();
                // 如果当前牌列是空的，或者打出的牌比这一行最后一张牌大
                if (lastCardInRow != null && playedCard.getNumber() > lastCardInRow.getNumber()) {
                    // 标记可以正常放置
                    cardCanBePlacedNormally = true;
                    int diff = playedCard.getNumber() - lastCardInRow.getNumber();
                    // 如果差值更小，更新目标行和最小差值
                    if (diff < bestDifference) {
                        bestDifference = diff;
                        targetRowIndex = i;
                    }
                } else if (lastCardInRow == null) { // 如果牌列是空的
                    // 也可以正常放置
                    cardCanBePlacedNormally = true;
                    // 100000是一个足够大的数
                    if (targetRowIndex == -1 || bestDifference > 100000) {
                        // 确保选择第一个空牌列
                        bestDifference = 100000 + i;
                        targetRowIndex = i;
                    }
                }
            }

            // 如果不能正常放置（即打出的牌比所有牌列的最后一张牌都小）
            if (!cardCanBePlacedNormally) {
                logger.info("玩家 {} 的牌 {} 小于房间 {} 所有牌列的末尾牌。玩家必须选择一行。", playerDisplayNameForLog, playedCard.getNumber(), room.getRoomId());

                // 游戏状态变为等待玩家选择
                room.setGameState(GameState.WAITING_FOR_PLAYER_CHOICE);
                // 记录是哪个玩家在选择
                room.setPlayerChoosingRowSessionId(currentPlayer.getSessionId());
                // 记录是哪张牌导致了选择
                room.setCardPendingChoice(playedCard);

                // 准备需要发送给前端的牌列选项信息
                List<Map<String, Object>> rowOptions = room.getRows().stream().map(r -> {
                    Map<String, Object> info = new HashMap<>();
                    // 获取行在列表中的真实索引
                    info.put("rowIndex", room.getRows().indexOf(r));
                    info.put("bullheads", r.getBullheadSum());
                    info.put("lastCardNumber", r.getLastCard() != null ? r.getLastCard().getNumber() : 0);
                    info.put("cardCount", r.getCards().size());
                    return info;
                }).collect(Collectors.toList());

                Map<String, Object> choicePromptPayload = new HashMap<>();
                // 更新为文档要求的 needSelectRow
                choicePromptPayload.put("type", "needSelectRow");

                Map<String, Object> dataPayload = new HashMap<>();
                dataPayload.put("playerId", currentPlayer.getUserId() != null ? String.valueOf(currentPlayer.getUserId()) : currentPlayer.getSessionId());
                dataPayload.put("cardNumber", playedCard.getNumber());
                dataPayload.put("reason", "该牌比所有行的最后一张牌都小");
                // 保留选项信息供前端参考
                dataPayload.put("options", rowOptions);
                dataPayload.put("timeout", playerChoiceTimeoutMs);

                choicePromptPayload.put("data", dataPayload);
                choicePromptPayload.put("roomId", room.getRoomId());


                WebSocketSession choosingPlayerSession = gameWebSocketHandler.getSessionById(currentPlayer.getSessionId());
                // 如果玩家是在线的（非托管）
                if (choosingPlayerSession != null && !currentPlayer.isTrustee()) {
                    try {
                        // 发送选择提示
                        gameWebSocketHandler.sendMessageToSession(choosingPlayerSession, choicePromptPayload);
                        scheduleTimeoutForPlayerChoice(room.getRoomId(), currentPlayer.getSessionId());
                        broadcastGameState(room.getRoomId(), playerDisplayNameForLog + " 需选择牌列...", room);
                    } catch (IOException e) {
                        logger.error("向玩家 {} 发送选择提示时出错: {}", currentPlayer.getSessionId(), e.getMessage());
                        // **FIXED**: 使用 room.getRoomId()
                        handlePlayerChoiceTimeout(room.getRoomId(), currentPlayer.getSessionId());
                    }
                } else if (currentPlayer.isTrustee()) { // 如果是托管玩家需要选择
                    logger.info("托管玩家 {} 必须选择牌列。自动选择。", playerDisplayNameForLog);
                    // **FIXED**: 使用 room.getRoomId()
                    handlePlayerChoiceTimeout(room.getRoomId(), currentPlayer.getSessionId());
                } else { // 找不到会话（理论上不应该，除非玩家瞬间断线）
                    logger.error("未找到需要选择的玩家 {} 的会话。", currentPlayer.getSessionId());
                    // **FIXED**: 使用 room.getRoomId()
                    handlePlayerChoiceTimeout(room.getRoomId(), currentPlayer.getSessionId());
                }
                // 等待玩家选择，暂时不处理后续的牌
                return;
            } else { // 如果可以正常放置
                // 保险：如果前面逻辑判断能放但没选出目标行（几乎不可能）
                if (targetRowIndex == -1) {
                    logger.error("严重错误：玩家 {} 的牌 {} 在房间 {} 中未找到可放置的牌列且未被强制选择。默认放到第0行。", playedCard.getNumber(), playerDisplayNameForLog, room.getRoomId());
                    // 强制放到第一行
                    targetRowIndex = 0;
                }
                GameRow selectedRow = room.getRows().get(targetRowIndex);
                // 根据 GameRow.MAX_CARDS_IN_ROW (值为5) 的规则：
                // 如果行内已有5张牌 (selectedRow.getCards().size() == 5)，这张是第6张牌的动作，导致拿走
                if (selectedRow.getCards().size() == selectedRow.getMAX_CARDS_IN_ROW()) {
                    handlePlayerTakesRow(room, currentPlayer, targetRowIndex, playedCard, "拿走已满牌列(第"+(selectedRow.getMAX_CARDS_IN_ROW()+1)+"张)");
                } else { // 否则 (行内0-4张牌)，安全添加
                    selectedRow.addCard(playedCard);
                }
                broadcastGameState(room.getRoomId(), playerDisplayNameForLog + " 的牌 " + playedCard.getNumber() + " 已放置。", room);
                // 继续处理下一张牌
                processNextCardInTrick(room);
            }
        } finally {
            roomLock.unlock();
        }
    }

    // 为玩家选择牌列启动一个超时计时器
    private void scheduleTimeoutForPlayerChoice(String roomId, String sessionId) {
        // 如果该玩家已有计时器，先取消旧的
        ScheduledFuture<?> existingTimer = choiceTimers.remove(sessionId);
        if (existingTimer != null) {
            existingTimer.cancel(false);
        }
        logger.info("为玩家 {} 在房间 {} 安排选择超时任务 ({}毫秒)。", sessionId, roomId, playerChoiceTimeoutMs);
        // 安排一个一次性的任务，在超时后执行 handlePlayerChoiceTimeout
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(() -> {
            // 任务执行前也移除自己，避免重复处理
            choiceTimers.remove(sessionId);
            handlePlayerChoiceTimeout(roomId, sessionId);
        }, new Date(System.currentTimeMillis() + playerChoiceTimeoutMs));
        // 保存这个计时器任务的引用
        choiceTimers.put(sessionId, scheduledTask);
    }

    public void playerChoosesRow(String roomId, String sessionId, Long userId, int chosenRowIndex) {
        WebSocketSession session = gameWebSocketHandler.getSessionById(sessionId);
        playerChoosesRowRaw(roomId, session, userId, chosenRowIndex);
    }

    // 处理玩家选择了要拿走的牌列
    public void playerChoosesRowRaw(String roomId, WebSocketSession session, Long userId, int chosenRowIndex) {
        GameRoom room = gameRoomService.getRoom(roomId);
        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            String sessionId = session != null ? session.getId() : null;
            if (sessionId == null) {
                return;
            }

            // 状态和玩家身份校验
            if (room == null || room.getGameState() != GameState.WAITING_FOR_PLAYER_CHOICE ||
                    !sessionId.equals(room.getPlayerChoosingRowSessionId())) {
                logger.warn("无效的牌列选择尝试。房间: {}, 状态: {}, 应选择玩家: {}, 当前玩家: {}",
                        roomId, room != null ? room.getGameState() : "N/A", room != null ? room.getPlayerChoosingRowSessionId() : "N/A", sessionId);
                sendErrorToUserSession(session, roomId, "现在不是您选择牌列的时间或状态无效。");
                return;
            }

            // 校验选择的牌列索引是否有效
            if (chosenRowIndex < 0 || chosenRowIndex >= room.getRows().size()) {
                sendErrorToUserSession(session, roomId, "无效的牌列选择。");
                return;
            }

            // 取消超时计时器
            ScheduledFuture<?> timer = choiceTimers.remove(sessionId);
            if (timer != null) {
                timer.cancel(false);
                logger.info("玩家 {} 选择了牌列 {}。房间 {} 的超时任务已取消。", userId, chosenRowIndex + 1, roomId);
            }

            Player currentPlayer = room.getPlayers().get(sessionId);
            // 获取之前导致选择的那张牌
            Card playedCard = room.getCardPendingChoice();

            if (currentPlayer == null || playedCard == null) {
                logger.error("在 playerChoosesRowRaw 期间，玩家或待处理的牌为null，会话 {}，房间 {}", session.getId(), roomId);
                // 尝试恢复状态
                room.setGameState(GameState.PLAYING);
                clearChoiceState(room);
                // 继续处理回合
                processNextCardInTrick(room);
                return;
            }

            // 执行拿走牌列的操作
            int collectedBullheads = handlePlayerTakesRow(room, currentPlayer, chosenRowIndex, playedCard, "玩家选择的牌列");

            // 发送 rowSelected 确认消息
            sendRowSelectedMessage(roomId, currentPlayer, chosenRowIndex, collectedBullheads, room.getRows().get(chosenRowIndex));

            // 清除选择状态
            clearChoiceState(room);
            // 回到处理回合状态
            room.setGameState(GameState.PROCESSING_TURN);
            broadcastGameState(roomId, currentPlayer.getDisplayName() + " 选择了第 " + (chosenRowIndex + 1) + " 行。", room);

            // 继续处理本轮剩下的牌
            processNextCardInTrick(room);

        } finally {
            roomLock.unlock();
        }
    }

    // 处理玩家选择牌列超时（或托管玩家自动选择）
    public void handlePlayerChoiceTimeout(String roomId, String choosingPlayerSessionId) {
        GameRoom room = gameRoomService.getRoom(roomId);
        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            // 再次校验状态，确保是对应的玩家和状态
            if (room == null || room.getGameState() != GameState.WAITING_FOR_PLAYER_CHOICE ||
                    !choosingPlayerSessionId.equals(room.getPlayerChoosingRowSessionId())) {
                logger.info("超时触发，但房间 {} 状态已非等待玩家 {} 选择，或选择者不匹配。", roomId, choosingPlayerSessionId);
                return;
            }

            logger.info("玩家 {} 在房间 {} 选择超时。服务器将自动选择。", choosingPlayerSessionId, roomId);

            Player currentPlayer = room.getPlayers().get(choosingPlayerSessionId);
            Card playedCard = room.getCardPendingChoice();

            if (currentPlayer == null || playedCard == null) {
                logger.error("超时：玩家或待处理的牌为null，会话 {}，房间 {}。无法自动选择。", choosingPlayerSessionId, roomId);
                room.setGameState(GameState.PLAYING);
                clearChoiceState(room);
                processNextCardInTrick(room);
                return;
            }

            // 服务器自动选择策略：选择当前牛头数最少的那一行
            int autoChosenRowIndex = 0;
            int minBullheads = Integer.MAX_VALUE;
            for (int i = 0; i < room.getRows().size(); i++) {
                int currentBullheads = room.getRows().get(i).getBullheadSum();
                if (currentBullheads < minBullheads) {
                    minBullheads = currentBullheads;
                    autoChosenRowIndex = i;
                }
            }
            logger.info("服务器为玩家 {} (超时) 在房间 {} 自动选择了牌列 {} (牛头数: {})。",
                    currentPlayer.getDisplayName(), roomId, autoChosenRowIndex + 1, minBullheads);


            int collectedBullheads = handlePlayerTakesRow(room, currentPlayer, autoChosenRowIndex, playedCard, "超时后服务器自动选择");

            // 发送 rowSelected 确认消息
            sendRowSelectedMessage(roomId, currentPlayer, autoChosenRowIndex, collectedBullheads, room.getRows().get(autoChosenRowIndex));

            clearChoiceState(room);
            room.setGameState(GameState.PROCESSING_TURN);
            broadcastGameState(roomId, currentPlayer.getDisplayName() + " 选择超时，服务器自动选择了第 " + (autoChosenRowIndex + 1) + " 行。", room);

            processNextCardInTrick(room);

        } finally {
            roomLock.unlock();
        }
    }

    // 清除与玩家选择牌列相关的状态
    private void clearChoiceState(GameRoom room) {
        room.setPlayerChoosingRowSessionId(null);
        room.setCardPendingChoice(null);
        // 理论上对应的timer在被选择或超时时已经被移除了，这里作为保险再次检查并清除所有（或特定）计时器
        List<String> timersToRemove = new ArrayList<>(choiceTimers.keySet());
        timersToRemove.forEach(key -> {
            ScheduledFuture<?> timer = choiceTimers.remove(key);
            if (timer != null) {
                // true 表示尝试中断正在执行的任务
                timer.cancel(true);
            }
        });
        logger.debug("房间 {} 的选择状态已清除，所有相关计时器已尝试取消。", room.getRoomId());
    }


    // 处理玩家拿走指定牌列，并将自己的牌作为该列新牌，返回收取的牛头数
    private int handlePlayerTakesRow(GameRoom room, Player player, int rowIndex, Card newCardForThisRow, String reason) {
        if (rowIndex < 0 || rowIndex >= room.getRows().size()) {
            logger.error("handlePlayerTakesRow: 无效的牌列索引 {} 对于房间 {}", rowIndex, room.getRoomId());
            return 0;
        }
        GameRow rowToTake = room.getRows().get(rowIndex);
        // 使用 GameRow 自带的 takeRowAndReplace 方法来处理拿牌和替换的逻辑
        LinkedList<Card> takenCards = rowToTake.takeRowAndReplace(newCardForThisRow);
        // 玩家收集这些牌（内部会计算牛头数并更新玩家分数）
        player.addCollectedCards(takenCards);

        int collectedBullheads = takenCards.stream().mapToInt(Card::getBullheads).sum();

        logger.info("玩家 {} {}，拿走牌列 {} 的牌。放置新牌 {}。新牛头总数: {}.",
                player.getDisplayName(), reason, rowIndex + 1, newCardForThisRow.getNumber(), player.getScore());

        return collectedBullheads;
    }

    private void sendRowSelectedMessage(String roomId, Player player, int rowIndex, int collectedBullheads, GameRow newRow) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "rowSelected");
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", player.getUserId() != null ? String.valueOf(player.getUserId()) : player.getSessionId());
        data.put("rowIndex", rowIndex);
        data.put("collectedBullheads", collectedBullheads);

        Map<String, Object> newRowMap = new HashMap<>();
        newRowMap.put("cards", newRow.getCards());
        data.put("newRow", newRowMap);

        payload.put("data", data);

        try {
            // 这个消息主要是给操作者确认，但也需要广播给所有人吗？
            // 文档 2.3 标题是 "服务器确认收牌行已选择"，且前端处理提到"等待服务器推送新的 roomStateUpdate"。
            // 既然是确认，通常发给请求者。但其他玩家也需要知道发生了什么吗？
            // "Frontend pops up selection dialog" -> "User must choose" -> "Confirm".
            // 通常广播 roomStateUpdate 足以更新场面，这个 rowSelected 更多是给操作者的反馈弹窗用 (如显示 "收牌成功")。
            // 不过如果是为了让所有人都看到动画（如果有），可能需要广播。
            // 文档没明确说广播，但通常 "Frontend: Close dialog, Show 'Collected Success'" implies user context.
            // 还是发给操作者吧。
            WebSocketSession session = gameWebSocketHandler.getSessionById(player.getSessionId());
            if (session != null && session.isOpen()) {
                gameWebSocketHandler.sendMessageToSession(session, payload);
            }
        } catch (IOException e) {
            logger.error("Failed to send rowSelected message", e);
        }
    }

    // 结束一个出牌回合（所有牌都处理完后）
    private void finalizeTurn(GameRoom room) {
        if(room == null) {
            logger.warn("finalizeTurn: room is null");
            return;
        }

        logger.info("结束房间 {} 第 {} 轮出牌的处理。", room.getRoomId(), room.getCurrentTurnNumber());

        boolean gameShouldEndByScore = false; // 是否因有玩家分数达到上限而结束游戏
        Player playerReachedScoreLimit = null; // 记录哪个玩家达到了分数上限
        for (Player player : room.getPlayers().values()) {
            if (player.getScore() >= 66) { // 游戏结束条件：有玩家牛头数达到66
                gameShouldEndByScore = true;
                playerReachedScoreLimit = player;
                break;
            }
        }

        // 如果有玩家分数达到上限，则游戏结束
        if (gameShouldEndByScore) {
            room.setGameState(GameState.GAME_OVER);
            // 根据最终分数决定赢家
            Player winner = determineWinner(room);
            String winnerName = (winner != null && winner.getDisplayName() != null && !winner.getDisplayName().isEmpty())
                    ? winner.getDisplayName() : null;
            room.setWinnerDisplayName(winnerName);

            String gameOverMsg = "游戏结束！";
            if (playerReachedScoreLimit != null) {
                gameOverMsg += "玩家 " + playerReachedScoreLimit.getDisplayName() + (playerReachedScoreLimit.isTrustee() ? " (托管)" : "") + " 牛头数达到或超过66。";
            }
            if (winner != null && winnerName != null) {
                gameOverMsg += "最终赢家是: " + winnerName + " (" + winner.getScore() + "牛头)";
            } else if (playerReachedScoreLimit == null && winner == null && !room.getPlayers().isEmpty()) {
                gameOverMsg += " 本局游戏没有明确赢家。";
            } else if (room.getPlayers().isEmpty()){
                gameOverMsg += " 所有玩家已离开。";
            }

            logger.info("finalizeTurn (游戏因分数结束): room.winnerDisplayName = {}, 消息: {}", room.getWinnerDisplayName(), gameOverMsg);
            broadcastGameState(room.getRoomId(), gameOverMsg, room);
            // 保存历史记录
            saveGameHistory(room);
            return;
        }

        // 如果本轮是第10次出牌（即10张手牌都出完了）
        if (room.getCurrentTurnNumber() >= 10) {
            logger.info("房间 {} 完成10手牌，本轮结束。", room.getRoomId());
            // 设置为“一轮结束”状态
            room.setGameState(GameState.ROUND_OVER);
            broadcastGameState(room.getRoomId(), "第 " + room.getCurrentTurnNumber() + " 次出牌结束，本轮完毕。", room);
            // 开始新一轮发牌和游戏
            startNewRound(room);
        } else { // 否则，回合数加1，继续下一回合出牌
            room.setCurrentTurnNumber(room.getCurrentTurnNumber() + 1);
            room.setGameState(GameState.PLAYING);
            broadcastGameState(room.getRoomId(), "准备进行第 " + room.getCurrentTurnNumber() + " 次出牌。", room);
            processBotTurnsAndCheckTurnCompletion(room);
        }
    }

    private void saveGameHistory(GameRoom room) {
        if (room == null || room.getPlayers() == null) {
            return;
        }

        // 计算房间平均分
        double avgScore = room.getPlayers().values().stream()
                .mapToInt(Player::getScore)
                .average()
                .orElse(0.0);

        // 计算排名
        List<Player> sortedPlayers = room.getPlayers().values().stream()
                .sorted(Comparator.comparingInt(Player::getScore))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player p = sortedPlayers.get(i);
            if (p.getUserId() != null) {
                GameHistory history = new GameHistory(p.getUserId(), room.getRoomId(), p.getScore(), i + 1, avgScore);
                gameHistoryRepository.save(history);
            }
        }
        logger.info("Game history saved for room {}", room.getRoomId());
    }

    // 处理玩家请求“再来一局”
    public void handleRequestNewGame(String roomId, WebSocketSession session, Long userId) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null || room.getGameState() != GameState.GAME_OVER) {
            sendErrorToUserSession(session, roomId, "非游戏结束状态，无法请求新局。");
            return;
        }
        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            Player p = room.getPlayers().get(session.getId());
            if (p == null || p.isTrustee()) {
                sendErrorToUserSession(session, roomId, "托管玩家无法请求。");
                return;
            }
            p.setRequestedNewGame(true);
            long active = room.getPlayers().values().stream().filter(pl -> !pl.isTrustee()).count();
            long requested = room.getPlayers().values().stream().filter(pl -> !pl.isTrustee() && pl.isRequestedNewGame()).count();
            // 改为>=，以防万一有额外请求
            if (active > 0 && requested >= active) {
                startGame(roomId, session.getId());
            } else {
                broadcastGameState(roomId, p.getDisplayName() + " 请求再来一局 (" + requested + "/" + active + ")。", room);
            }
        } finally {
            roomLock.unlock();
        }
    }

    // 当一个新WebSocket连接建立时，检查该用户是否可以重返之前因断线而托管的房间
    public GameRoom checkAndRejoinPlayerOnConnectRaw(WebSocketSession newSession, Long userIdentifier) {
        if (userIdentifier == null) {
            return null;
        }
        Optional<User> userOpt = userRepository.findById(userIdentifier);

        if (!userOpt.isPresent()) {
            return null;
        }
        User user = userOpt.get();

        for (GameRoom room : gameRoomService.getAllActiveRoomsInMemory()) {
            if (room == null) {
                // 跳过空的room对象
                continue;
            }
            Lock roomLock = getRoomLock(room.getRoomId());
            roomLock.lock();
            try {
                if (room.getGameState() == GameState.WAITING) {
                    continue;
                }

                Player playerToRejoin = null; String oldSessionId = null;
                // 遍历查找是否有与当前连接用户ID相同且处于托管状态的玩家
                // 复制Set进行迭代，允许修改原Map
                for (Map.Entry<String, Player> entry : new HashSet<>(room.getPlayers().entrySet())) {
                    if (entry.getValue().getUserId() != null && entry.getValue().getUserId().equals(user.getId()) && entry.getValue().isTrustee()) {
                        playerToRejoin = entry.getValue(); oldSessionId = entry.getKey(); break;
                    }
                }
                if (playerToRejoin != null) {
                    if (oldSessionId != null) {
                        // 移除旧的会话条目
                        room.getPlayers().remove(oldSessionId);
                    }
                    // 更新玩家信息
                    playerToRejoin.setSessionId(newSession.getId());
                    playerToRejoin.setTrustee(false);
                    playerToRejoin.setReady(false); playerToRejoin.setRequestedNewGame(false);
                    playerToRejoin.setVipStatus(user.getVipStatus());
                    // 使用新会话ID添加回房间
                    room.getPlayers().put(newSession.getId(), playerToRejoin);
                    logger.info("玩家 {} 重返房间 {} 并取消托管。", playerToRejoin.getDisplayName(), room.getRoomName());

                    // 如果游戏已结束，玩家重返可能改变获胜者
                    if (room.getGameState() == GameState.GAME_OVER) {
                        Player newWinner = determineWinner(room);
                        String newWinnerName = newWinner != null ? newWinner.getDisplayName() : null;
                        if ((newWinnerName != null && !newWinnerName.equals(room.getWinnerDisplayName())) ||
                                (newWinnerName == null && room.getWinnerDisplayName() != null) ||
                                (newWinnerName != null && room.getWinnerDisplayName() == null) ) {
                            logger.info("因玩家重返，获胜者更新。旧: {}, 新: {}", room.getWinnerDisplayName(), newWinnerName);
                            room.setWinnerDisplayName(newWinnerName);
                        }
                    }
                    return room;
                }
            } finally { roomLock.unlock(); }
        }
        return null;
    }

    // 处理玩家加入房间的请求
    public void playerJoinsRaw(String roomId, WebSocketSession session, Long userIdentifier) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            sendErrorToUserSession(session, roomId, "房间不存在。");
            return;
        }
        Optional<User> userOpt = Optional.empty();
        try {
            userOpt = userRepository.findById(userIdentifier);
        } catch (NumberFormatException e) {
            logger.warn("playerJoins: UserIdentifier '{}' 不是有效的数字ID。", userIdentifier);
        }

        if (!userOpt.isPresent()) {
            sendErrorToUserSession(session, roomId, "用户不存在。");
            return;
        }
        User user = userOpt.get();

        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            Optional<Player> existingPlayerOpt = room.getPlayers().values().stream()
                    .filter(p -> p.getUserId() != null && p.getUserId().equals(user.getId())).findFirst();

            if (existingPlayerOpt.isPresent()) { // 玩家已在房间
                Player p = existingPlayerOpt.get();
                if (!p.getSessionId().equals(session.getId())) { // 不同会话，尝试重连托管
                    // 只有当玩家处于托管状态，且游戏正在进行中（非结束、非等待、非回合结束）时，才允许这种方式的“重连托管”
                    // 仅限PLAYING状态
                    if ((p.isTrustee() && room.getGameState() == GameState.PLAYING) || room.getGameState() == GameState.WAITING) {
                        room.getPlayers().remove(p.getSessionId());
                        p.setSessionId(session.getId()); p.setTrustee(false); p.setReady(false); p.setRequestedNewGame(false);
                        p.setVipStatus(user.getVipStatus());
                        // 维持房主状态
                        // 维持机器人状态（通过会话重连不是机器人）
                        room.getPlayers().put(session.getId(), p);
                        broadcastGameState(roomId, p.getDisplayName() + " 已重连并取消托管！", room);
                    } else {
                        sendErrorToUserSession(session, roomId, "无法重连托管状态（可能游戏状态不对或玩家非托管）。");
                        return;
                    }
                } else { // 同会话，重复加入请求
                    // 更新一下VIP状态以防万一
                    p.setVipStatus(user.getVipStatus());
                    // 确保 isHost 正确
                    if (user.getId().equals(room.getOwnerId())) {
                        p.setHost(true);
                    } else {
                        p.setHost(false);
                    }
                    Map<String, Object> payload = new HashMap<>(); payload.put("type", "gameStateUpdate");
                    payload.put("message", "您已在房间中。"); payload.put("roomState", room);
                    try {
                        gameWebSocketHandler.sendMessageToSession(session, payload);
                    } catch (IOException e) {
                        logger.error("发送重复加入状态时出错",e);
                    }
                }
            } else { // 新玩家加入
                // 只有在等待或游戏已结束时才允许新玩家加入
                if (room.getGameState() != GameState.WAITING && room.getGameState() != GameState.GAME_OVER) {
                    sendErrorToUserSession(session, roomId, "游戏正在进行中，新玩家无法加入。");
                    return;
                }
                // 检查房间人数是否已满（只算未托管的）
                if (room.getPlayers().values().stream().filter(pl->!pl.isTrustee()).count() >= room.getMaxPlayers()) {
                    sendErrorToUserSession(session, roomId, "房间活跃玩家人数已满。");
                    return;
                }
                String name = user.getNickname()!=null && !user.getNickname().isEmpty() ? user.getNickname() : user.getUsername();
                Player newP = new Player(session.getId(), user.getId(), name, user.getVipStatus());
                // 初始化新玩家的游戏数据
                newP.resetForNewGame();
                // 如果是房主，默认设置为已准备
                if (user.getId().equals(room.getOwnerId())) {
                    newP.setReady(true);
                    newP.setHost(true);
                } else {
                    newP.setHost(false);
                }
                newP.setRobot(false);
                room.getPlayers().put(session.getId(), newP);
                broadcastGameState(roomId, name + " 已加入房间。", room);
            }
        } finally {
            roomLock.unlock();
        }
    }

    /**
     * 房主移除玩家 (踢人)
     * @param roomId 房间ID
     * @param requesterUserId 发起请求的用户ID (必须是房主)
     * @param targetUserId 被踢的目标用户ID
     */
    public void kickPlayer(String roomId, Long requesterUserId, Long targetUserId) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        if (room.getOwnerId() == null || !room.getOwnerId().equals(requesterUserId)) {
            throw new IllegalArgumentException("只有房主可以踢人。");
        }

        if (requesterUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("不能踢自己。");
        }

        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            // 寻找目标玩家
            Player targetPlayer = null;
            for (Player p : room.getPlayers().values()) {
                if (p.getUserId() != null && p.getUserId().equals(targetUserId)) {
                    targetPlayer = p;
                    break;
                }
            }

            if (targetPlayer == null) {
                throw new IllegalArgumentException("该玩家不在房间内。");
            }

            String targetSessionId = targetPlayer.getSessionId();
            logger.info("玩家 {} (ID: {}) 被房主 {} 从房间 {} 移除。", targetPlayer.getDisplayName(), targetUserId, requesterUserId, roomId);

            // 通知目标
            WebSocketSession session = gameWebSocketHandler.getSessionById(targetSessionId);
            if (session != null && session.isOpen()) {
                Map<String, Object> kickMsg = new HashMap<>();
                kickMsg.put("type", "kicked");
                kickMsg.put("message", "您已被房主移除出了房间。");
                try {
                    gameWebSocketHandler.sendMessageToSession(session, kickMsg);
                } catch (IOException e) {
                    logger.error("Error sending kick message", e);
                }
            }

            // 执行离开逻辑
            playerLeavesRaw(roomId, targetSessionId, targetUserId);

            // 显式关闭会话以强制客户端断开连接
            if (session != null && session.isOpen()) {
                try {
                    session.close();
                } catch (IOException e) {
                    logger.error("Error closing session for kicked player", e);
                }
            }

        } finally {
            roomLock.unlock();
        }
    }

    public void togglePlayerReadyStatus(String roomId, String sessionId, Long userIdentifier) {
        WebSocketSession session = gameWebSocketHandler.getSessionById(sessionId);
        togglePlayerReadyStatusRaw(roomId, session, userIdentifier);
    }

    // 切换玩家的准备状态
    public void togglePlayerReadyStatusRaw(String roomId, WebSocketSession session, Long userIdentifier) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null || room.getGameState() != GameState.WAITING) {
            sendErrorToUserSession(session, roomId, "非等待状态，无法准备。");
            return;
        }
        Lock roomLock = getRoomLock(roomId);
        roomLock.lock();
        try {
            String sid = session != null ? session.getId() : null;
            if (sid == null) {
                return;
            }

            Player p = room.getPlayers().get(sid);
            if (p == null) {
                sendErrorToUserSession(session, roomId, "玩家未找到。");
                return;
            }
            if (p.isTrustee()) {
                // 如果玩家被标记为托管，但通过有效会话发送了指令，说明玩家已活跃，自动解除托管
                logger.info("玩家 {} (会话 {}) 处于托管状态但发送了指令，自动解除托管。", p.getDisplayName(), sid);
                p.setTrustee(false);
            }
            p.setReady(!p.isReady());
            broadcastGameState(roomId, p.getDisplayName() + (p.isReady() ? " 已准备" : " 取消准备"), room);
        } finally {
            roomLock.unlock();
        }
    }

    // 决定赢家（规则：不论是否托管，分数最低者胜）
    private Player determineWinner(GameRoom room) {
        if (room.getPlayers().isEmpty()) {
            return null;
        }
        return room.getPlayers().values().stream()
                .min(Comparator.comparingInt(Player::getScore))
                .orElse(null);
    }

    /**
     * 获取出牌提示（VIP功能）
     * @param roomId 房间ID
     * @param requestingPlayerSessionId 请求提示的玩家会话ID
     * @return 包含建议牌号、预计牛头数和原因的Map，或错误信息
     */
    public Map<String, Object> getPlayTip(String roomId, String requestingPlayerSessionId) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            return Collections.singletonMap("error", "房间不存在。");
        }

        Lock roomLock = getRoomLock(roomId); // 确保在读取房间状态时数据一致
        roomLock.lock();
        try {
            Player requestingPlayer = room.getPlayers().get(requestingPlayerSessionId);
            if (requestingPlayer == null) {
                return Collections.singletonMap("error", "未找到玩家。");
            }
            if (requestingPlayer.getVipStatus() == 0) {
                return Collections.singletonMap("error", "非会员无法使用此功能。");
            }
            if (room.getGameState() != GameState.PLAYING || requestingPlayer.isTrustee() || requestingPlayer.getHand().isEmpty()) {
                return Collections.singletonMap("error", "当前状态无法获取提示。");
            }
            logger.info("会员 {} 请求提示，房间 {}", requestingPlayer.getDisplayName(), roomId);

            // 复制手牌
            List<Card> playerHand = new ArrayList<>(requestingPlayer.getHand());
            // 本轮其他玩家已出的牌
            List<Card> otherPlayedCardsThisTurn = new ArrayList<>();
            if (room.getPlayedCardsThisTurn() != null) {
                room.getPlayedCardsThisTurn().forEach((sid, card) -> {
                    if (!sid.equals(requestingPlayerSessionId)) {
                        otherPlayedCardsThisTurn.add(card);
                    }
                });
            }
            // 其他活跃玩家的手牌
            Map<String, List<Card>> otherActivePlayerHands = new HashMap<>();
            if (room.getAllPlayerHandsForAI() != null) {
                room.getPlayers().forEach((sid, p) -> {
                    if (!sid.equals(requestingPlayerSessionId) && !p.isTrustee() && room.getAllPlayerHandsForAI().containsKey(sid)) {
                        otherActivePlayerHands.put(sid, new ArrayList<>(room.getAllPlayerHandsForAI().get(sid)));
                    }
                });
            }

            Card bestCardToPlay = null;
            int minExpectedBullheads = Integer.MAX_VALUE;
            String bestReason = "无明显优选，请谨慎。";

            // 为当前玩家的每一张手牌进行模拟评估
            for (Card cardInHand : playerHand) {
                // !!! 重要：为每次模拟创建独立的牌列副本 !!!
                // 这是因为 calculateAdvancedExpectedBullheads 方法会修改传入的 simulatedRows 列表
                List<GameRow> rowsForThisSimulation = new ArrayList<>();
                // 从真实的当前房间状态复制
                for (GameRow originalRoomRow : room.getRows()) {
                    // 创建新的 GameRow 实例
                    GameRow copiedRow = new GameRow();
                    // 手动复制牌列表的内容
                    if (originalRoomRow.getCards() != null) {
                        for (Card card_c : originalRoomRow.getCards()) {
                            // 假设 Card 是不可变的或简单对象，可以直接创建新的或复制引用
                            copiedRow.addCard(new Card(card_c.getNumber(), card_c.getBullheads()));
                        }
                    }
                    rowsForThisSimulation.add(copiedRow);
                }

                EstimatedPlayResult result = calculateAdvancedExpectedBullheads(
                        cardInHand, rowsForThisSimulation, otherPlayedCardsThisTurn,
                        otherActivePlayerHands, requestingPlayerSessionId, room // 注意这里最后一个参数是 room (GameRoom)
                );
                if (result.getBullheads() < minExpectedBullheads) {
                    minExpectedBullheads = result.getBullheads();
                    bestCardToPlay = cardInHand;
                    bestReason = result.getReason();
                } else if (result.getBullheads() == minExpectedBullheads) {
                    if (bestCardToPlay == null || (cardInHand != null && cardInHand.getNumber() < bestCardToPlay.getNumber())) {
                        bestCardToPlay = cardInHand;
                        bestReason = result.getReason() + " (同牛头，选小编号)";
                    }
                }
            }

            if (bestCardToPlay != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("suggestedCardNumber", bestCardToPlay.getNumber());
                response.put("estimatedBullheads", minExpectedBullheads);
                response.put("reason", bestReason);
                return response;
            } else if (!playerHand.isEmpty()){
                Card fallbackCard = playerHand.get(0);
                List<GameRow> rowsForFallbackSim = new ArrayList<>();
                for (GameRow originalRoomRow : room.getRows()) {
                    GameRow copiedRow = new GameRow();
                    if (originalRoomRow.getCards() != null) {
                        for (Card card_c : originalRoomRow.getCards()) {
                            copiedRow.addCard(new Card(card_c.getNumber(), card_c.getBullheads()));
                        }
                    }
                    rowsForFallbackSim.add(copiedRow);
                }
                EstimatedPlayResult fallbackResult = calculateAdvancedExpectedBullheads(fallbackCard, rowsForFallbackSim, otherPlayedCardsThisTurn, otherActivePlayerHands, requestingPlayerSessionId, room);
                Map<String, Object> response = new HashMap<>();
                response.put("suggestedCardNumber", fallbackCard.getNumber());
                response.put("estimatedBullheads", fallbackResult.getBullheads());
                response.put("reason", fallbackResult.getReason() + " (备选)");
                return response;
            }
            return Collections.singletonMap("message", "无合适建议。");
        } finally {
            roomLock.unlock();
        }
    }

    // 内部辅助类：用于AI提示返回结果
    private static class EstimatedPlayResult {
        private final int bullheads; private final String reason;
        public EstimatedPlayResult(int bullheads, String reason) { this.bullheads = bullheads; this.reason = reason; }
        public int getBullheads() { return bullheads; }
        public String getReason() { return reason; }
    }

    // 内部辅助类：用于AI提示模拟回合
    private static class PlayerCardTurnEntry {
        private final String playerId; private final Card card;
        public PlayerCardTurnEntry(String playerId, Card card) { this.playerId = playerId; this.card = card; }
        public String getPlayerId() { return playerId; }
        public Card getCard() { return card; }
    }

    // AI大脑核心：评估打出某张牌的预期结果
    // roomContext 是真实的 GameRoom
    private EstimatedPlayResult calculateAdvancedExpectedBullheads(
            Card cardToPlay, List<GameRow> simulatedRows, List<Card> otherPlayedCardsThisTurn,
            Map<String, List<Card>> otherActivePlayerHands, String requestingPlayerId, GameRoom roomContext) {

        List<PlayerCardTurnEntry> cardsForSim = new ArrayList<>();
        cardsForSim.add(new PlayerCardTurnEntry(requestingPlayerId, cardToPlay));
        // 给一个模拟ID
        otherPlayedCardsThisTurn.forEach(c -> cardsForSim.add(new PlayerCardTurnEntry("other_known_player_sim_id", c)));
        // 按牌号排序
        cardsForSim.sort(Comparator.comparingInt(e -> e.getCard().getNumber()));

        int bullheadsTaken = 0;
        String reason = "分析中...";

        // 模拟本轮所有已知牌的放置
        for (PlayerCardTurnEntry turnEntry : cardsForSim) {
            Card currentCard = turnEntry.getCard();
            // 是否是当前请求提示的玩家的牌
            boolean isMyCard = turnEntry.getPlayerId().equals(requestingPlayerId);
            int targetRowIdx = -1, bestDiff = Integer.MAX_VALUE;
            boolean canPlace = false;

            // 查找最佳放置行 (与主游戏逻辑 processNextCardInTrick 中的放置部分类似)
            // 操作的是模拟行 simulatedRows
            for (int i = 0; i < simulatedRows.size(); i++) {
                GameRow row = simulatedRows.get(i);
                Card last = row.getLastCard();
                if (last != null && currentCard.getNumber() > last.getNumber()) {
                    canPlace = true;
                    int diff = currentCard.getNumber() - last.getNumber();
                    if (diff < bestDiff) { bestDiff = diff; targetRowIdx = i; }
                } else if (last == null) { // 空行
                    canPlace = true;
                    if (targetRowIdx == -1 || bestDiff > 100000) { bestDiff = 100000 + i; targetRowIdx = i;}
                }
            }

            // 可以正常放置
            if (canPlace && targetRowIdx != -1) {
                GameRow selectedSimRow = simulatedRows.get(targetRowIdx);
                // GameRow.MAX_CARDS_IN_ROW 在 GameRow.java 中是 5
                // 表示行内已有5张牌时，这张是第6个操作的牌，导致拿走前5张
                if (selectedSimRow.getCards().size() == selectedSimRow.getMAX_CARDS_IN_ROW()) {
                    if (isMyCard) {
                        bullheadsTaken = selectedSimRow.getBullheadSum();
                        reason = "危险! 牌 " + currentCard.getNumber() + " 放第" + (targetRowIdx + 1) + "行将是第6张,拿走" + bullheadsTaken + "牛头。";
                    }
                    // 模拟拿走并替换: 先清空模拟行，再把当前牌加入
                    selectedSimRow.getCards().clear();
                    selectedSimRow.addCard(currentCard);
                } else { // 安全放置 (行内0-4张牌)
                    if (isMyCard) {
                        bullheadsTaken = 0;
                        reason = "安全。牌 " + currentCard.getNumber() + " 可放第" + (targetRowIdx + 1) + "行(当前"+(selectedSimRow.getCards().size()+1)+"/"+selectedSimRow.getMAX_CARDS_IN_ROW()+"张)。";
                    }
                    selectedSimRow.addCard(currentCard);
                }
            } else { // 必须选择一行拿走
                if (isMyCard) {
                    int minBH = Integer.MAX_VALUE, chosenIdx = 0;
                    for (int i = 0; i < simulatedRows.size(); i++) {
                        if (simulatedRows.get(i).getBullheadSum() < minBH) {
                            minBH = simulatedRows.get(i).getBullheadSum(); chosenIdx = i;
                        }
                    }
                    bullheadsTaken = minBH;
                    reason = "太小! 被迫选第" + (chosenIdx + 1) + "行，拿" + bullheadsTaken + "牛头。";
                    // 模拟拿走并替换
                    simulatedRows.get(chosenIdx).getCards().clear();
                    simulatedRows.get(chosenIdx).addCard(currentCard);
                } else { // 其他人的牌被迫选择，也要更新模拟桌面状态
                    int minBH = Integer.MAX_VALUE, chosenIdx = 0;
                    for (int i = 0; i < simulatedRows.size(); i++) {
                        if (simulatedRows.get(i).getBullheadSum() < minBH) {
                            minBH = simulatedRows.get(i).getBullheadSum(); chosenIdx = i;
                        }
                    }
                    simulatedRows.get(chosenIdx).getCards().clear();
                    simulatedRows.get(chosenIdx).addCard(currentCard);
                }
            }
            // 如果已经模拟了当前请求提示玩家的牌，就可以停止这一轮的模拟了
            if (isMyCard) {
                break;
            }
        }

        // （简化版）风险评估：检查其他活跃玩家手牌是否有潜在威胁
        // 只在初步安全时评估进一步风险
        if (bullheadsTaken == 0) {
            int initialTargetRowForMyCard = -1;
            int tempBestDiffForMyCard = Integer.MAX_VALUE;
            // 基于真实的、未经模拟修改的当前牌列（从roomContext获取）来判断cardToPlay的原始目标行
            List<GameRow> realCurrentRows = roomContext.getRows();
            for (int i = 0; i < realCurrentRows.size(); i++) {
                GameRow r = realCurrentRows.get(i);
                Card l = r.getLastCard();
                if (l != null && cardToPlay.getNumber() > l.getNumber()) {
                    int d = cardToPlay.getNumber() - l.getNumber();
                    if (d < tempBestDiffForMyCard) { tempBestDiffForMyCard = d; initialTargetRowForMyCard = i; }
                } else if (l == null) { // 空行
                    if (initialTargetRowForMyCard == -1 || tempBestDiffForMyCard > 100000) { tempBestDiffForMyCard = 100000+i; initialTargetRowForMyCard = i; }
                }
            }

            // 如果cardToPlay有一个明确的初始目标行
            if (initialTargetRowForMyCard != -1) {
                // 这是真实的行状态
                GameRow intendedRealRow = realCurrentRows.get(initialTargetRowForMyCard);
                Card lastOnIntendedRealRow = intendedRealRow.getLastCard();
                // 目标行还能放几张牌才到5张（即下一个就满了）
                int safeSpotsLeft = intendedRealRow.getMAX_CARDS_IN_ROW() - intendedRealRow.getCards().size();
                if (safeSpotsLeft < 0) {
                    // 如果已经满了或超过，则安全位置为0
                    safeSpotsLeft = 0;
                }

                // 潜在的“捣乱牌”数量
                int potentialInterferingCardsCount = 0;
                // 检查其他活跃玩家的手牌
                for (List<Card> otherHand : otherActivePlayerHands.values()) {
                    for (Card cOther : otherHand) {
                        // “捣乱牌”：比你的牌小（会先出），且能放到你的目标行
                        if (cOther.getNumber() < cardToPlay.getNumber() &&
                                (lastOnIntendedRealRow == null || cOther.getNumber() > lastOnIntendedRealRow.getNumber())) {
                            potentialInterferingCardsCount++;
                        }
                    }
                }
                // (可选)也可以考虑本轮其他已出的牌的干扰，但上面的主模拟部分应该已经处理了它们的直接影响。
                // 主要这里的风险是来自其他人“未出”的牌。

                // 如果捣乱牌数量足以填满剩余安全位（且原先有安全位）
                if (potentialInterferingCardsCount >= safeSpotsLeft && safeSpotsLeft > 0) {
                    reason += " (高风险: 其他人有 " + potentialInterferingCardsCount + " 张牌可能抢先填满目标行，该行仅剩 " + safeSpotsLeft + " 安全位!)";
                    // bullheadsTaken += 1; // 可以考虑给一个小的牛头惩罚
                } else if (potentialInterferingCardsCount > 0 && safeSpotsLeft > 0) {
                    reason += " (中风险: 其他人有 " + potentialInterferingCardsCount + " 张牌可能抢位)";
                }
            }
        }
        return new EstimatedPlayResult(bullheadsTaken, reason);
    }
}

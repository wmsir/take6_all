// src/main/java/com/example/top_hog_server/handler/GameWebSocketHandler.java
package com.example.top_hog_server.handler;

import com.example.top_hog_server.model.GameRoom;
// 确保引入Player
import com.example.top_hog_server.model.Player;
// 确保引入User
import com.example.top_hog_server.model.User;
// 确保引入UserRepository
import com.example.top_hog_server.repository.UserRepository;
import com.example.top_hog_server.service.GameLogicService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomSessionIds = new ConcurrentHashMap<>();

    private final GameLogicService gameLogicService;
    // 注入UserRepository
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public GameWebSocketHandler(@Lazy GameLogicService gameLogicService,
                                // 注入
                                UserRepository userRepository,
                                ObjectMapper objectMapper) {
        this.gameLogicService = gameLogicService;
        // 初始化
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get(UserHandshakeInterceptorRaw.USER_IDENTIFIER_SESSION_KEY);
        logger.info("WebSocket 连接已建立: SessionId={}, UserId={}, Uri={}",
                session.getId(), userId, session.getUri());
        sessions.put(session.getId(), session);

        if (userId == null) {
            logger.warn("会话 {} 的 UserIdentifier 为空。用户可能无法正确识别。发送错误并关闭。", session.getId());
            sendErrorMessage(session, null, "用户标识丢失。请使用用户标识重新连接。");
            session.close(CloseStatus.POLICY_VIOLATION.withReason("需要用户标识"));
            return;
        }

        // 1. 获取用户会员状态
        Optional<User> userOpt = userRepository.findById(userId);
        int vipStatus = 0;
        if (userOpt.isPresent()) {
            vipStatus = userOpt.get().getVipStatus();
        } else {
            logger.warn("在 afterConnectionEstablished 中未找到用户 {} 的账户信息，无法确定VIP状态。", userId);
        }
        logger.info("用户 {} 连接，VIP状态: {}", userId, vipStatus);


        // 2. 首先发送标准的连接确认消息 (现在包含vipStatus)
        sendWelcomeMessage(session, vipStatus);

        // 3. 服务器主动尝试为该 userIdentifier 重返任何其之前所在的、且处于托管状态的房间
        logger.info("尝试为用户 '{}' (新会话: {}) 进行服务器端主动重返...", userId, session.getId());
        GameRoom rejoinedRoom = gameLogicService.checkAndRejoinPlayerOnConnectRaw(session, userId);

        if (rejoinedRoom != null) {
            addSessionToRoom(rejoinedRoom.getRoomId(), session.getId());

            // 确保重返的Player对象也包含最新的vipStatus
            Player rejoinedPlayer = rejoinedRoom.getPlayers().get(session.getId());
            if (rejoinedPlayer != null) {
                // 再次确认VIP状态
                rejoinedPlayer.setVipStatus(vipStatus);
            }

            Map<String, Object> rejoinSuccessMap = new HashMap<>();
            rejoinSuccessMap.put("type", "rejoinSuccess");
            // roomState 中的 Player 对象应该有vipStatus
            rejoinSuccessMap.put("roomState", rejoinedRoom);
            sendMessageToSession(session, rejoinSuccessMap);
            logger.info("用户 {} 已由服务器主动重返房间 {}", userId, rejoinedRoom.getRoomId());
        } else {
            logger.info("用户 {} 连接时未自动重返任何房间。客户端将进入房间选择流程。", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Long userId = (Long) session.getAttributes().get(UserHandshakeInterceptorRaw.USER_IDENTIFIER_SESSION_KEY);
        logger.info("收到来自用户 '{}' (会话 {}) 的消息: {}",
                userId, session.getId(), payload.substring(0, Math.min(payload.length(), 200)));

        if (userId == null) {
            sendErrorMessage(session, null, "此会话的用户身份未建立。消息被忽略。");
            return;
        }

        try {
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String type = (String) messageData.get("type");
            String roomId = (String) messageData.get("roomId");

            if (type == null) {
                sendErrorMessage(session, roomId, "消息类型丢失。");
                return;
            }

            switch (type) {
                case "requestRejoinInfo":
                    String clientProvidedRoomId = (String) messageData.get("roomId");
                    logger.info("收到来自用户 {} 的 requestRejoinInfo 请求，客户端提供的房间ID: {}", userId, clientProvidedRoomId);
                    GameRoom rejoinedRoomFromRequest = gameLogicService.checkAndRejoinPlayerOnConnectRaw(session, userId);

                    if (rejoinedRoomFromRequest != null) {
                        if (clientProvidedRoomId != null && !clientProvidedRoomId.equals(rejoinedRoomFromRequest.getRoomId())) {
                            logger.warn("客户端请求重返房间 {}，但实际重返到房间 {}。", clientProvidedRoomId, rejoinedRoomFromRequest.getRoomId());
                        }
                        addSessionToRoom(rejoinedRoomFromRequest.getRoomId(), session.getId());
                        Map<String, Object> rejoinSuccessMap = new HashMap<>();
                        rejoinSuccessMap.put("type", "rejoinSuccess");
                        rejoinSuccessMap.put("roomState", rejoinedRoomFromRequest);
                        sendMessageToSession(session, rejoinSuccessMap);
                        logger.info("用户 {} (通过 explicit requestRejoinInfo) 成功重返房间 {}", userId, rejoinedRoomFromRequest.getRoomId());
                    } else {
                        Map<String, Object> rejoinFailedMap = new HashMap<>();
                        rejoinFailedMap.put("type", "rejoinFailed");
                        rejoinFailedMap.put("message", "无法重返房间 (服务器未找到可重返的托管状态)。");
                        if (clientProvidedRoomId != null) {
                            rejoinFailedMap.put("requestedRoomId", clientProvidedRoomId);
                        }
                        sendMessageToSession(session, rejoinFailedMap);
                        logger.info("用户 {} explicit rejoin 请求 (房间 {}) 失败 (或未找到托管状态)。", userId, clientProvidedRoomId);
                    }
                    break;
                case "joinRoom":
                    if (roomId != null) {
                        addSessionToRoom(roomId, session.getId());
                        gameLogicService.playerJoinsRaw(roomId, session, userId);
                    } else {
                        sendErrorMessage(session, null, "加入房间请求缺少 roomId。");
                    }
                    break;
                case "playerReady":
                    if (roomId != null) {
                        gameLogicService.togglePlayerReadyStatusRaw(roomId, session, userId);
                    } else {
                        sendErrorMessage(session, null, "切换准备状态请求缺少 roomId。");
                    }
                    break;
                case "startGame":
                    if (roomId != null) {
                        gameLogicService.startGame(roomId, session.getId());
                    } else {
                        sendErrorMessage(session, null, "开始游戏请求缺少 roomId。");
                    }
                    break;
                case "playCard":
                    if (roomId != null && messageData.get("data") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cardData = (Map<String, Object>) messageData.get("data");
                        Object cardNumberObj = cardData.get("cardNumber");
                        if (cardNumberObj instanceof Integer) {
                            Integer cardNumber = (Integer) cardNumberObj;
                            gameLogicService.playerPlaysCardRaw(roomId, session, userId, cardNumber);
                        } else {
                            sendErrorMessage(session, roomId, "出牌请求中的 cardNumber 无效。");
                        }
                    } else {
                        sendErrorMessage(session, roomId, "出牌请求缺少 roomId 或有效的 data 负载。");
                    }
                    break;
                case "requestPlayTip": // 处理会员的出牌提示请求
                    if (roomId != null) {
                        Map<String, Object> tip = gameLogicService.getPlayTip(roomId, session.getId());
                        Map<String, Object> tipResponsePayload = new HashMap<>();
                        tipResponsePayload.put("type", "playTipResponse");
                        if (tip != null) {
                            tipResponsePayload.put("success", true);
                            tipResponsePayload.put("tip", tip);
                            logger.info("向玩家 {} 发送出牌提示: {}", userId, tip);
                        } else {
                            tipResponsePayload.put("success", false);
                            tipResponsePayload.put("message", "无法生成提示或您不是会员。");
                            logger.info("无法为玩家 {} 生成出牌提示或非会员。", userId);
                        }
                        sendMessageToSession(session, tipResponsePayload);
                    } else {
                        sendErrorMessage(session, null, "请求出牌提示缺少 roomId。");
                    }
                    break;
                case "chat":
                    if (roomId != null && messageData.get("data") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> chatData = (Map<String, Object>) messageData.get("data");
                        String chatText = (String) chatData.get("text");
                        if (chatText != null) {
                            Map<String, Object> chatMessagePayload = new HashMap<>();
                            chatMessagePayload.put("type", "chatMessage");
                            chatMessagePayload.put("sender", userId);
                            chatMessagePayload.put("text", chatText);
                            chatMessagePayload.put("roomId", roomId);
                            broadcastToRoom(roomId, chatMessagePayload);
                        } else {
                            sendErrorMessage(session, roomId, "聊天消息文本为空。");
                        }
                    } else {
                        sendErrorMessage(session, roomId, "聊天请求缺少 roomId 或有效的 data 负载。");
                    }
                    break;
                case "playerChoosesRow": // 已弃用/旧版
                case "selectRow": // 新需求
                    if (roomId != null) {
                        Integer rowIndex = null;
                        if (messageData.containsKey("rowIndex")) {
                            // 直接字段 (Requirement 2.2 style)
                            Object idxObj = messageData.get("rowIndex");
                            if (idxObj instanceof Integer) rowIndex = (Integer) idxObj;
                        } else if (messageData.get("data") instanceof Map) {
                            // 旧版嵌套数据
                            @SuppressWarnings("unchecked")
                            Map<String, Object> choiceData = (Map<String, Object>) messageData.get("data");
                            if (choiceData.get("chosenRowIndex") instanceof Integer) {
                                rowIndex = (Integer) choiceData.get("chosenRowIndex");
                            }
                        }

                        if (rowIndex != null) {
                            gameLogicService.playerChoosesRowRaw(roomId, session, userId, rowIndex);
                        } else {
                            sendErrorMessage(session, roomId, "选择牌列的消息中 rowIndex 无效。");
                        }
                    } else {
                        sendErrorMessage(session, roomId, "选择牌列的消息缺少 roomId。");
                    }
                    break;
                case "requestNewGame":
                    if (roomId != null) {
                        gameLogicService.handleRequestNewGame(roomId, session, userId);
                    } else {
                        sendErrorMessage(session, null, "“再来一局”请求缺少 roomId。");
                    }
                    break;
                case "toggleAutoPlay":
                    if (roomId != null) {
                        gameLogicService.togglePlayerAutoPlay(roomId, session.getId());
                    } else {
                        sendErrorMessage(session, null, "toggleAutoPlay 请求缺少 roomId。");
                    }
                    break;
                case "leaveRoom":
                    if (roomId != null) {
                        logger.info("用户 {} (会话 {}) 主动离开房间 {}", userId, session.getId(), roomId);
                        removeSessionFromRoom(roomId, session.getId());
                        // Use playerRequestsLeave to properly mark as explicit leave
                        gameLogicService.playerRequestsLeave(roomId, userId);
                        Map<String, Object> leftRoomMap = new HashMap<>();
                        leftRoomMap.put("type", "leftRoomSuccess");
                        leftRoomMap.put("roomId", roomId);
                        sendMessageToSession(session, leftRoomMap);
                    } else {
                        sendErrorMessage(session, null, "离开房间请求缺少 roomId。");
                    }
                    break;
                default:
                    logger.warn("未知消息类型 '{}' 来自用户 '{}' (会话 {})", type, userId, session.getId());
                    sendErrorMessage(session, roomId, "未知的消息类型: " + type);
            }
        } catch (JsonProcessingException e) {
            logger.error("JSON 处理错误，来自用户 '{}' (会话 {}) 的消息: {}", userId, session.getId(), payload, e);
            sendErrorMessage(session, null, "无法解析消息格式 (非JSON或格式错误).");
        } catch (Exception e) {
            logger.error("处理来自用户 '{}' (会话 {}) 的消息时发生错误: {}", userId, session.getId(), payload, e);
            sendErrorMessage(session, null, "处理您的消息时发生服务器内部错误。");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get(UserHandshakeInterceptorRaw.USER_IDENTIFIER_SESSION_KEY);
        logger.info("WebSocket 连接已关闭: SessionId={}, UserId={}, Status: {}",
                session.getId(), userId, status.toString());
        sessions.remove(session.getId());

        String roomIdAssociated = removeSessionFromAllRooms(session.getId());

        if (roomIdAssociated != null && userId != null) {
            logger.info("通知 GameLogicService 玩家 {} 由于断开连接离开房间 {}", userId, roomIdAssociated);
            gameLogicService.playerLeavesRaw(roomIdAssociated, session.getId(), userId);
        } else if (userId != null) {
            logger.info("断开连接的会话 {} (用户 {}) 未在任何追踪的房间中，或在断开清理时 userIdentifier 为空。", session.getId(), userId);
        } else {
            logger.info("断开连接的会话 {} 的属性中没有 userIdentifier。无法执行特定的玩家离开操作。", session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get(UserHandshakeInterceptorRaw.USER_IDENTIFIER_SESSION_KEY);
        logger.error("WebSocket 传输错误，用户 '{}' (会话 {}): {}",
                userId, session.getId(), exception.getMessage(), exception);
        sessions.remove(session.getId());
        removeSessionFromAllRooms(session.getId());
    }

    public WebSocketSession getSessionById(String sessionId) {
        return sessions.get(sessionId);
    }

    public void sendMessageToSession(WebSocketSession session, Object messageDto) throws IOException {
        if (session != null && session.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(messageDto);
            session.sendMessage(new TextMessage(jsonMessage));
            logger.trace("已发送消息到会话 {}: {}", session.getId(), jsonMessage.substring(0, Math.min(jsonMessage.length(), 200)));
        } else {
            logger.warn("尝试向一个null、已关闭或不存在的会话发送消息 (ID: {}).", session != null ? session.getId() : "null");
        }
    }

    public void broadcastToRoom(String roomId, Object messageDto) throws IOException {
        Set<String> sessionIdsInRoom = roomSessionIds.get(roomId);
        if (sessionIdsInRoom != null && !sessionIdsInRoom.isEmpty()) {
            String jsonMessage = objectMapper.writeValueAsString(messageDto);
            logger.debug("向房间 {} ({} 个会话) 广播消息: {}", roomId, sessionIdsInRoom.size(), jsonMessage.substring(0, Math.min(jsonMessage.length(), 200)));
            int sentCount = 0;
            Set<String> sessionIdsCopy = new HashSet<>(sessionIdsInRoom);
            for (String sessionId : sessionIdsCopy) {
                WebSocketSession sessionInRoom = sessions.get(sessionId);
                if (sessionInRoom != null && sessionInRoom.isOpen()) {
                    try {
                        sessionInRoom.sendMessage(new TextMessage(jsonMessage));
                        sentCount++;
                    } catch (IOException e) {
                        logger.error("向房间 {} 中的会话 {} 发送消息时发生IOException: {}", roomId, sessionId, e.getMessage());
                    }
                } else {
                    logger.warn("房间 {} 中的会话 {} 为null或未打开。跳过广播到此会话。", roomId, sessionId);
                }
            }
            logger.debug("向房间 {} 的广播完成。已发送消息数: {}", roomId, sentCount);
        } else {
            logger.warn("没有活跃会话在房间 {} 中可供广播消息。", roomId);
        }
    }

    public void addSessionToRoom(String roomId, String sessionId) {
        if (roomId == null || sessionId == null) {
            logger.warn("尝试向房间添加会话时，roomId 或 sessionId 为空。RoomId: {}, SessionId: {}", roomId, sessionId);
            return;
        }
        removeSessionFromAllRooms(sessionId);

        roomSessionIds.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        logger.info("会话 {} 已添加到房间 {}。房间内当前会话数: {}", sessionId, roomId, roomSessionIds.get(roomId).size());
    }

    public void removeSessionFromRoom(String roomId, String sessionId) {
        if (roomId == null || sessionId == null) {
            return;
        }
        Set<String> sessionsInThisRoom = roomSessionIds.get(roomId);
        if (sessionsInThisRoom != null) {
            if (sessionsInThisRoom.remove(sessionId)) {
                logger.info("会话 {} 已从房间 {} 中显式移除。", sessionId, roomId);
                if (sessionsInThisRoom.isEmpty()) {
                    // roomSessionIds.remove(roomId);
                    // logger.info("房间 {} 在会话追踪中已空。", roomId);
                }
            }
        }
    }

    private String removeSessionFromAllRooms(String sessionId) {
        String removedFromRoomId = null;
        for (Map.Entry<String, Set<String>> entry : roomSessionIds.entrySet()) {
            if (entry.getValue().remove(sessionId)) {
                removedFromRoomId = entry.getKey();
                logger.info("会话 {} 已从房间 {} 中隐式移除 (由于加入其他房间或断开连接)。", sessionId, removedFromRoomId);
                if (entry.getValue().isEmpty()) {
                    // roomSessionIds.remove(removedFromRoomId);
                    // logger.info("房间 {} 已变空并从会话追踪中移除。", removedFromRoomId);
                }
                break;
            }
        }
        return removedFromRoomId;
    }

    private void sendErrorMessage(WebSocketSession session, String roomId, String message) {
        Map<String, Object> errorPayload = new HashMap<>();
        errorPayload.put("type", "error");
        errorPayload.put("message", message);
        if (roomId != null) {
            errorPayload.put("roomId", roomId);
        }
        try {
            sendMessageToSession(session, errorPayload);
        } catch (IOException e) {
            logger.error("发送错误消息 '{}' 到会话 {} 失败: {}", message, session.getId(), e.getMessage());
        }
    }

    // 添加 vipStatus 参数
    private void sendWelcomeMessage(WebSocketSession session, int vipStatus) {
        Map<String, Object> welcomePayload = new HashMap<>();
        welcomePayload.put("type", "connectionAcknowledged");
        welcomePayload.put("sessionId", session.getId());
        welcomePayload.put("message", "WebSocket 连接成功。您的会话ID是 " + session.getId());
        // 将 vipStatus 添加到欢迎消息中
        welcomePayload.put("vipStatus", vipStatus);
        try {
            sendMessageToSession(session, welcomePayload);
        } catch (IOException e) {
            logger.error("发送欢迎消息到会话 {} 失败: {}", session.getId(), e.getMessage());
        }
    }
}

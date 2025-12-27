package com.example.take6server.controller;

import com.example.take6server.model.GameRoom;
import com.example.take6server.payload.dto.response.GameRoomDTO;
import com.example.take6server.payload.dto.response.ApiResponse;
import com.example.take6server.service.GameLogicService;
import com.example.take6server.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.take6server.security.services.UserDetailsImpl;

import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameLogicService gameLogicService;
    private final GameRoomService gameRoomService;

    @Autowired
    public GameController(GameLogicService gameLogicService, GameRoomService gameRoomService) {
        this.gameLogicService = gameLogicService;
        this.gameRoomService = gameRoomService;
    }

    @PostMapping("/play")
    public ApiResponse<Map<String, Object>> playCard(@RequestBody Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        int cardNumber = (Integer) payload.get("cardNumber");

        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            return ApiResponse.success("Room not found", null);
        }

        String sessionId = null;
        for (var entry : room.getPlayers().entrySet()) {
            if (entry.getValue().getUserId() != null && entry.getValue().getUserId().equals(user.getId())) {
                sessionId = entry.getKey();
                break;
            }
        }

        if (sessionId == null) {
            return ApiResponse.success("User not in room or no active session found. Please connect via WebSocket.", null);
        }

        // 我们需要一个虚拟的 WebSocketSession 或者修改 GameLogicService 以直接接受 sessionId。
        // 查看 GameLogicService，`playerPlaysCardRaw` 接受 WebSocketSession。
        // 它使用 `session.getId()` 来识别玩家。
        // 我应该重载 `playerPlaysCardRaw` 或者创建一个接受 roomId, sessionId, userId 的辅助方法。

        // 实际上，`gameWebSocketHandler` 存储了会话。
        // 如果用户已连接，我们可以检索会话吗？
        // `GameWebSocketHandler` 有 `getSessionById`。
        // 所以我们可以传递一个具有正确 ID 的虚拟会话？或者只是重构 `GameLogicService`。

        // 既然我不能轻易重构 `GameLogicService` 对 `WebSocketSession` 的依赖而不改变大量代码，
        // 而且我不能构造一个有效的 `StandardWebSocketSession`。

        // 让我们看看 `playerPlaysCardRaw`：
        /*
        public void playerPlaysCardRaw(String roomId, WebSocketSession webSocketSession, String userIdentifier, int cardNumber) {
             ...
             Player player = room.getPlayers().get(webSocketSession.getId());
             ...
             sendErrorToUserSession(webSocketSession, ...)
        */

        // 它使用会话进行错误报告。
        // 如果我传递 null，它可能会崩溃或跳过错误报告。
        // 我将在 `GameLogicService` 中重载 `playerPlaysCardRaw` 以接受 `sessionId` 字符串。

        gameLogicService.playerPlaysCard(roomId, sessionId, null, cardNumber);

        return ApiResponse.success(Map.of());
    }

    @PostMapping("/take-row")
    public ApiResponse<Map<String, Object>> takeRow(@RequestBody Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        int rowIndex = (Integer) payload.get("rowIndex");

        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            return ApiResponse.success("Room not found", null);
        }

        String sessionId = null;
        for (var entry : room.getPlayers().entrySet()) {
            if (entry.getValue().getUserId() != null && entry.getValue().getUserId().equals(user.getId())) {
                sessionId = entry.getKey();
                break;
            }
        }

        if (sessionId == null) {
            return ApiResponse.success("User not in room or no active session found. Please connect via WebSocket.", null);
        }

        gameLogicService.playerChoosesRow(roomId, sessionId, user.getId(), rowIndex);

        return ApiResponse.success(Map.of());
    }

    @PostMapping("/state")
    public ApiResponse<GameRoomDTO> getGameState(@RequestBody Map<String, String> payload) {
        String roomId = payload.get("roomId");
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            return ApiResponse.success("Room not found", null);
        }

        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(GameRoomDTO.from(room, String.valueOf(user.getId())));
    }
}

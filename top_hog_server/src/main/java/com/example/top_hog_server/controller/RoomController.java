package com.example.top_hog_server.controller;

import com.example.top_hog_server.model.GameRoom;
import com.example.top_hog_server.model.Player;
import com.example.top_hog_server.payload.dto.request.AddBotsRequest;
import com.example.top_hog_server.payload.dto.request.CreatePveRoomRequest;
import com.example.top_hog_server.payload.dto.response.ApiResponse;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import com.example.top_hog_server.service.GameLogicService;
import com.example.top_hog_server.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理游戏房间相关的 HTTP 请求的 REST 控制器。
 * 负责房间的创建、加入、离开、准备以及开始游戏等操作。
 */
@RestController
@RequestMapping("/api/room")
public class RoomController {

    private final GameRoomService gameRoomService;
    private final GameLogicService gameLogicService;

    @Autowired
    public RoomController(GameRoomService gameRoomService, GameLogicService gameLogicService) {
        this.gameRoomService = gameRoomService;
        this.gameLogicService = gameLogicService;
    }

    /**
     * 创建一个新的游戏房间。
     *
     * @param payload 包含创建房间所需信息的 Map，例如 roomName, maxPlayers 等
     * @return 包含新创建的 GameRoom 对象的 ApiResponse
     */
    @PostMapping("/create")
    public ApiResponse<GameRoom> createRoom(@RequestBody Map<String, Object> payload) {
        GameRoom room = gameRoomService.createRoom(payload);
        return ApiResponse.success(room);
    }

    /**
     * 创建一个人机对战房间 (PvE)。
     * 此接口作为快速创建并填充机器人的快捷方式。
     *
     * @param payload 包含机器人数量等信息的请求对象
     * @return 包含新创建的 PvE GameRoom 对象的 ApiResponse
     */
    @PostMapping("/create-pve")
    public ApiResponse<GameRoom> createPveRoom(@RequestBody CreatePveRoomRequest payload) {
        // 1. 创建一个普通房间
        Map<String, Object> roomPayload = new HashMap<>();
        roomPayload.put("roomName", "PvE Room");
        roomPayload.put("maxPlayers", 6); // 默认大小
        GameRoom room = gameRoomService.createRoom(roomPayload);

        // 2. 添加请求数量的机器人
        int botCount = payload.getBotCount();
        if (botCount > 0) {
            gameLogicService.addBotsToRoom(room.getRoomId(), botCount);
        }

        return ApiResponse.success(room);
    }

    /**
     * 向指定房间添加机器人。
     * 只有房主可以执行此操作。
     *
     * @param payload 包含房间ID和机器人数量的请求对象
     * @return 包含操作结果消息和当前玩家列表的 ApiResponse
     */
    @PostMapping("/add-bots")
    public ApiResponse<Map<String, Object>> addBots(@RequestBody AddBotsRequest payload) {
        String roomId = payload.getRoomId();
        int botCount = payload.getBotCount();

        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            return ApiResponse.error(404, "Room not found");
        }

        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getId().equals(room.getOwnerId())) {
            return ApiResponse.error(403, "Only the room owner can add bots");
        }

        gameLogicService.addBotsToRoom(roomId, botCount);
        return ApiResponse.success(Map.of(
                "message", "Bots added successfully",
                "currentPlayers", room.getCurrentPlayers(),
                "players", room.getPlayers().values()
        ));
    }

    /**
     * 加入现有的游戏房间。
     *
     * @param payload 包含房间ID (roomId) 和可选密码 (password) 的 Map
     * @return 包含加入的 GameRoom 对象的 ApiResponse
     */
    @PostMapping("/join")
    public ApiResponse<GameRoom> joinRoom(@RequestBody Map<String, String> payload) {
        String roomId = payload.get("roomId");
        String password = payload.getOrDefault("password", "");
        GameRoom room = gameRoomService.joinRoom(roomId, password);
        return ApiResponse.success(room);
    }

    /**
     * 快速匹配并加入有空余位置的房间。
     * 自动查找第一个有空位的公开房间并加入。
     *
     * @return 包含加入的 GameRoom 对象的 ApiResponse
     */
    @PostMapping("/quick-match")
    public ApiResponse<GameRoom> quickMatch() {
        GameRoom room = gameRoomService.quickMatch();
        return ApiResponse.success(room);
    }

    /**
     * 获取所有当前可用的游戏房间列表。
     * 支持分页和仅显示可用房间的过滤。
     *
     * @param payload 包含分页参数 (page, pageSize) 和过滤条件 (onlyAvailable) 的 Map
     * @return 包含房间列表和总数的 ApiResponse
     */
    @PostMapping("/list")
    public ApiResponse<Map<String, Object>> listRooms(@RequestBody Map<String, Object> payload) {
        int page = (Integer) payload.getOrDefault("page", 1);
        int pageSize = (Integer) payload.getOrDefault("pageSize", 20);
        boolean onlyAvailable = (Boolean) payload.getOrDefault("onlyAvailable", false);

        List<GameRoom> list = gameRoomService.listRooms(page, pageSize, onlyAvailable);

        return ApiResponse.success(Map.of("list", list, "total", list.size()));
    }

    /**
     * 离开当前所在的游戏房间。
     *
     * @param payload 包含房间ID (roomId) 的 Map
     * @return 空数据的成功 ApiResponse
     */
    @PostMapping("/leave")
    public ApiResponse<Map<String, Object>> leaveRoom(@RequestBody Map<String, String> payload) {
        String roomId = payload.get("roomId");
        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 使用 GameLogicService 进行带状态检查的离开操作
        gameLogicService.playerRequestsLeave(roomId, user.getId());

        return ApiResponse.success(Map.of());
    }

    /**
     * 房主移除指定的玩家。
     *
     * @param payload 包含房间ID (roomId) 和目标用户ID (targetUserId) 的 Map
     * @return 操作结果
     */
    @PostMapping("/kick")
    public ApiResponse<Map<String, Object>> kickPlayer(@RequestBody Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        if (roomId == null) {
            return ApiResponse.error(400, "Missing roomId");
        }

        Object targetIdObj = payload.get("targetUserId");
        if (targetIdObj == null) {
            return ApiResponse.error(400, "Missing targetUserId");
        }

        Long targetUserId;
        try {
            if (targetIdObj instanceof Integer) {
                targetUserId = ((Integer) targetIdObj).longValue();
            } else if (targetIdObj instanceof String) {
                targetUserId = Long.valueOf((String) targetIdObj);
            } else if (targetIdObj instanceof Long) {
                targetUserId = (Long) targetIdObj;
            } else {
                return ApiResponse.error(400, "Invalid targetUserId format");
            }
        } catch (NumberFormatException e) {
            return ApiResponse.error(400, "Invalid targetUserId format");
        }

        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            gameLogicService.kickPlayer(roomId, user.getId(), targetUserId);
            return ApiResponse.success(Map.of("message", "Player kicked successfully"));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * 切换玩家在房间中的准备状态。
     * 玩家需要在 WebSocket 连接建立后才能进行此操作。
     *
     * @param payload 包含房间ID (roomId) 的 Map
     * @return 空数据的成功 ApiResponse，如果失败则返回错误信息
     */
    @PostMapping("/ready")
    public ApiResponse<Map<String, Object>> togglePlayerReadyStatus(@RequestBody Map<String, String> payload) {
        String roomId = payload.get("roomId");
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            return ApiResponse.error(404, "Room not found");
        }

        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String sessionId = getSessionIdForCurrentUser(room, user);

        if (sessionId == null) {
             return ApiResponse.error(400, "User not connected via WebSocket");
        }

        // 调用业务逻辑服务切换准备状态，传入正确的用户ID
        gameLogicService.togglePlayerReadyStatus(roomId, sessionId, user.getId());

        return ApiResponse.success(Map.of());
    }

    /**
     * 开始游戏。
     * 只有房主可以开始游戏，且所有玩家必须已准备。
     *
     * @param payload 包含房间ID (roomId) 的 Map
     * @return 空数据的成功 ApiResponse，如果失败则返回错误信息
     */
    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> start(@RequestBody Map<String, String> payload) {
        String roomId = payload.get("roomId");
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
             return ApiResponse.error(404, "Room not found");
        }

        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String sessionId = getSessionIdForCurrentUser(room, user);

        if (sessionId == null) {
             return ApiResponse.error(400, "User not connected via WebSocket");
        }

        gameLogicService.startGame(roomId, sessionId);
        return ApiResponse.success(Map.of());
    }

    /**
     * 获取当前认证用户在指定房间中的 WebSocket 会话 ID。
     *
     * @param room 游戏房间对象
     * @param user 当前认证的用户详情
     * @return 用户的 WebSocket 会话 ID，如果未找到则返回 null
     */
    private String getSessionIdForCurrentUser(GameRoom room, UserDetailsImpl user) {
        for (Map.Entry<String, Player> entry : room.getPlayers().entrySet()) {
            if (entry.getValue().getUserId() != null && entry.getValue().getUserId().equals(user.getId())) {
                return entry.getKey();
            }
        }
        return null;
    }
}

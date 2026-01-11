package com.example.top_hog_server.service;

import com.example.top_hog_server.exception.BusinessException;
import com.example.top_hog_server.exception.ErrorCode;
import com.example.top_hog_server.model.GameRoom;
import com.example.top_hog_server.model.GameState;
import com.example.top_hog_server.model.Player;
import com.example.top_hog_server.repository.GameRoomRepository;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GameRoomService {

    private final ConcurrentHashMap<String, GameRoom> activeRooms = new ConcurrentHashMap<>();
    private final GameRoomRepository gameRoomRepository;

    @Autowired
    public GameRoomService(GameRoomRepository gameRoomRepository) {
        this.gameRoomRepository = gameRoomRepository;
        // Load active rooms from DB if needed
        List<GameRoom> rooms = gameRoomRepository.findByGameState(GameState.WAITING);
        for(GameRoom r : rooms) {
            activeRooms.put(r.getRoomId(), r);
        }
        rooms = gameRoomRepository.findByGameState(GameState.PLAYING);
        for(GameRoom r : rooms) {
            activeRooms.put(r.getRoomId(), r);
        }
    }

    public GameRoom createRoom(Map<String, Object> payload) {
        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String roomId = UUID.randomUUID().toString().substring(0, 4).toUpperCase(); // Short ID
        String roomName = (String) payload.getOrDefault("roomName", user.getNickname() + "的房间");

        GameRoom room = new GameRoom(roomId, roomName);
        room.setOwnerId(user.getId());
        room.setMaxPlayers((Integer) payload.getOrDefault("maxPlayers", 6));
        room.setMaxRounds((Integer) payload.getOrDefault("maxRounds", 3));
        room.setTargetScore((Integer) payload.getOrDefault("targetScore", 66));
        room.setPrivate((Boolean) payload.getOrDefault("isPrivate", false));
        if (room.isPrivate()) {
            room.setPassword((String) payload.getOrDefault("password", ""));
        }
        room.setGameState(GameState.WAITING);
        
        // 设置游戏类型
        String gameTypeCode = (String) payload.getOrDefault("gameType", com.example.top_hog_server.model.GameType.TOP_HOG.getCode());
        room.setGameType(com.example.top_hog_server.model.GameType.fromCode(gameTypeCode));

        // Do not join creator automatically in HTTP request.
        // The client must connect via WebSocket to join properly.
        // The `isHost` logic will be handled when the player actually joins via WebSocket or HTTP Join logic if it returns a player.
        // According to requirements: "创建房间的用户自动成为房主，其 `isHost` 字段应为 `true`"
        // But since we are not adding the player to the room object here (waiting for WS), we can't set it on a player object.
        // However, the `GameRoom` object is returned. It has an empty `players` map.
        // So the frontend will see an empty players map.
        // If the frontend expects the creator to be in `players`, we might need to add them.
        // Memory says: "To prevent 'ghost players,' users are added to the active in-memory game state only upon establishing a WebSocket connection, not during the HTTP join request."
        // So we adhere to that. The players map will be empty.
        // The frontend creates the room, gets the ID, connects WS, then sends "joinRoom" or similar, or the backend auto-joins on connect if possible.

        activeRooms.put(roomId, room);
        gameRoomRepository.save(room);

        return room;
    }

    public synchronized GameRoom joinRoom(String roomId, String password) {
        GameRoom room = activeRooms.get(roomId);
        if (room == null) {
            // Try load from DB
             Optional<GameRoom> r = gameRoomRepository.findById(roomId);
             if(r.isPresent()) {
                 room = r.get();
                 activeRooms.put(roomId, room);
             } else {
                 throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Room not found");
             }
        }

        if (room.isPrivate() && !room.getPassword().equals(password)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Incorrect password");
        }

        // Do not add player to memory map here.
        // Just return the room info. The client will connect via WebSocket.
        // If we want to prevent overfill, we check count here.
        if (room.getPlayers().size() >= room.getMaxPlayers()) {
             throw new BusinessException(ErrorCode.ACCESS_DENIED, "Room is full");
        }

        return room;
    }

    public List<GameRoom> listRooms(int page, int pageSize, boolean onlyAvailable) {
        // Simple pagination on memory list
        List<GameRoom> list = new ArrayList<>(activeRooms.values());
        if (onlyAvailable) {
            list = list.stream()
                .filter(r -> r.getGameState() == GameState.WAITING && r.getCurrentPlayers() < r.getMaxPlayers())
                .collect(Collectors.toList());
        }

        // Sort by created time? roomId is random.
        // Let's just return list for now.
        int start = Math.min((page - 1) * pageSize, list.size());
        int end = Math.min(start + pageSize, list.size());
        return list.subList(start, end);
    }

    public synchronized void leaveRoom(String roomId) {
        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        GameRoom room = activeRooms.get(roomId);
        if (room != null) {
            room.removePlayer(String.valueOf(user.getId()));
            if (room.getPlayers().isEmpty()) {
                activeRooms.remove(roomId);
                gameRoomRepository.deleteById(roomId);
            }
        }
    }


    public void startGame(String roomId) {
        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        GameRoom room = activeRooms.get(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Room not found");
        }

        if (!room.getOwnerId().equals(user.getId())) {
             throw new BusinessException(ErrorCode.ACCESS_DENIED, "Only owner can start game");
        }

        // Logic to start game (deal cards, etc.)
        // Calling GameLogicService?
        // Since GameLogicService is autowired in Controller, maybe I should put logic there or inject it here.
        // Ideally GameRoomService handles Room management, GameLogicService handles Game.
        // Starting game is a transition.
    }

    public GameRoom getRoom(String roomId) {
        return activeRooms.get(roomId);
    }

    public void removeRoom(String roomId) {
        activeRooms.remove(roomId);
        gameRoomRepository.deleteById(roomId);
    }

    public Collection<GameRoom> getAllActiveRoomsInMemory() {
        return activeRooms.values();
    }

    public List<GameRoom> getAllAvailableRooms() {
         return activeRooms.values().stream()
                .filter(room -> room.getGameState() == GameState.WAITING &&
                        room.getPlayers().size() < room.getMaxPlayers())
                .collect(Collectors.toList());
    }

    /**
     * 快速匹配：自动加入第一个有空位的公开房间
     * 如果没有可用房间，则抛出异常
     */
    public GameRoom quickMatch() {
        // 获取所有可用的公开房间（非私密房间且有空位）
        List<GameRoom> availableRooms = activeRooms.values().stream()
                .filter(room -> room.getGameState() == GameState.WAITING &&
                        room.getPlayers().size() < room.getMaxPlayers() &&
                        !room.isPrivate())
                .collect(Collectors.toList());

        if (availableRooms.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "No available rooms for quick match");
        }

        // 选择第一个可用房间
        GameRoom room = availableRooms.get(0);
        
        // 加入该房间（不需要密码，因为是公开房间）
        return joinRoom(room.getRoomId(), "");
    }
}

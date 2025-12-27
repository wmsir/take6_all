package com.example.take6server.service;

import com.example.take6server.exception.BusinessException;
import com.example.take6server.exception.ErrorCode;
import com.example.take6server.model.GameRoom;
import com.example.take6server.model.GameState;
import com.example.take6server.model.Player;
import com.example.take6server.repository.GameRoomRepository;
import com.example.take6server.security.services.UserDetailsImpl;
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

    public GameRoom joinRoom(String roomId, String password) {
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

    public void leaveRoom(String roomId) {
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
}

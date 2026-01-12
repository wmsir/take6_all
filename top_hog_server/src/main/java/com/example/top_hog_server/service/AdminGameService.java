package com.example.top_hog_server.service;

import com.example.top_hog_server.exception.BusinessException;
import com.example.top_hog_server.exception.ErrorCode;
import com.example.top_hog_server.model.GameHistory;
import com.example.top_hog_server.model.GameRoom;
import com.example.top_hog_server.model.GameState;
import com.example.top_hog_server.repository.GameHistoryRepository;
import com.example.top_hog_server.repository.GameRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GM管理服务 - 游戏管理
 */
@Service
@Slf4j
public class AdminGameService {

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    /**
     * 获取所有活跃房间列表
     */
    public Map<String, Object> getActiveRooms() {
        Collection<GameRoom> rooms = gameRoomService.getAllActiveRoomsInMemory();

        List<Map<String, Object>> roomList = rooms.stream()
                .map(this::buildRoomInfo)
                .sorted((a, b) -> {
                    LocalDateTime timeA = (LocalDateTime) a.get("createdAt");
                    LocalDateTime timeB = (LocalDateTime) b.get("createdAt");
                    return timeB.compareTo(timeA);
                })
                .collect(Collectors.toList());

        // 统计
        long waitingRooms = rooms.stream()
                .filter(r -> r.getGameState() == GameState.WAITING)
                .count();
        long playingRooms = rooms.stream()
                .filter(r -> r.getGameState() == GameState.PLAYING)
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("rooms", roomList);
        result.put("total", rooms.size());
        result.put("waitingRooms", waitingRooms);
        result.put("playingRooms", playingRooms);

        return result;
    }

    /**
     * 获取房间详情
     */
    public Map<String, Object> getRoomDetail(String roomId) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "房间不存在");
        }

        Map<String, Object> detail = buildRoomInfo(room);

        // 添加玩家详细信息
        detail.put("players", room.getPlayers());

        return detail;
    }

    /**
     * 强制解散房间
     */
    public void dismissRoom(String roomId, String reason) {
        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "房间不存在");
        }

        // 移除房间
        gameRoomService.removeRoom(roomId);

        log.info("GM强制解散房间: roomId={}, reason={}", roomId, reason);
    }

    /**
     * 获取游戏对局记录
     */
    public Map<String, Object> getGameHistory(Long userId, String roomId,
            Integer page, Integer pageSize) {
        List<GameHistory> allHistory;

        if (userId != null) {
            allHistory = gameHistoryRepository.findByUserIdOrderByCreatedAtDesc(
                    userId, PageRequest.of(0, 1000));
        } else if (roomId != null) {
            allHistory = gameHistoryRepository.findAll().stream()
                    .filter(h -> Objects.equals(h.getRoomId(), roomId))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        } else {
            allHistory = gameHistoryRepository.findAll().stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(1000)
                    .collect(Collectors.toList());
        }

        // 分页
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, allHistory.size());
        List<GameHistory> pagedHistory = allHistory.subList(start, end);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pagedHistory);
        result.put("total", allHistory.size());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return result;
    }

    /**
     * 获取游戏统计数据
     */
    public Map<String, Object> getGameStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        List<GameHistory> history;

        if (startTime != null && endTime != null) {
            history = gameHistoryRepository.findAll().stream()
                    .filter(h -> h.getCreatedAt().toLocalDateTime().isAfter(startTime)
                            && h.getCreatedAt().toLocalDateTime().isBefore(endTime))
                    .collect(Collectors.toList());
        } else {
            history = gameHistoryRepository.findAll();
        }

        // 总游戏场次
        long totalGames = history.stream()
                .map(GameHistory::getRoomId)
                .distinct()
                .count();

        // 总参与人次
        long totalPlayers = history.size();

        // 平均每局人数
        double avgPlayersPerGame = totalGames > 0 ? (double) totalPlayers / totalGames : 0;

        // 平均分数
        double avgScore = history.stream()
                .mapToInt(GameHistory::getScore)
                .average()
                .orElse(0);

        // 按游戏类型统计 (暂未记录游戏类型)
        Map<String, Long> gamesByType = new HashMap<>();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGames", totalGames);
        stats.put("totalPlayers", totalPlayers);
        stats.put("avgPlayersPerGame", avgPlayersPerGame);
        stats.put("avgScore", avgScore);
        stats.put("gamesByType", gamesByType);

        return stats;
    }

    /**
     * 构建房间信息
     */
    private Map<String, Object> buildRoomInfo(GameRoom room) {
        Map<String, Object> info = new HashMap<>();
        info.put("roomId", room.getRoomId());
        info.put("roomName", room.getRoomName());
        info.put("ownerId", room.getOwnerId());
        info.put("gameType", room.getGameType());
        info.put("gameState", room.getGameState());
        info.put("currentPlayers", room.getCurrentPlayers());
        info.put("maxPlayers", room.getMaxPlayers());
        info.put("currentRound", room.getCurrentRound());
        info.put("maxRounds", room.getMaxRounds());
        info.put("isPrivate", room.isPrivate());
        info.put("createdAt", java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(room.getCreatedAtTimestamp()), java.time.ZoneId.systemDefault()));

        return info;
    }
}

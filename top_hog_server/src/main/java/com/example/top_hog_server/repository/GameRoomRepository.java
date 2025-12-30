package com.example.top_hog_server.repository;

import com.example.top_hog_server.model.GameRoom;
import com.example.top_hog_server.model.GameState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * GameRoom 实体的 JPA Repository 接口。
 * 提供对 GameRoom 表的 CRUD 操作。
 * GameRoom 的主键类型是 String (roomId)。
 */
@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, String> {

    /**
     * 根据游戏状态查找游戏房间列表。
     * @param gameState 游戏状态
     * @return 符合该状态的游戏房间列表
     */
    List<GameRoom> findByGameState(GameState gameState);

    /**
     * 根据房间名称（部分匹配，忽略大小写）查找游戏房间列表。
     * @param namePart 房间名称的一部分
     * @return 包含该名称部分的游戏房间列表
     */
    List<GameRoom> findByRoomNameContainingIgnoreCase(String namePart);
}
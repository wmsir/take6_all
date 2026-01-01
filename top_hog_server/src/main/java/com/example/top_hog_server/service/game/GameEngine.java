package com.example.top_hog_server.service.game;

import com.example.top_hog_server.model.*;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

/**
 * 游戏引擎接口
 * 所有游戏类型都需要实现此接口
 * 提供统一的游戏逻辑处理方式
 */
public interface GameEngine {
    
    /**
     * 获取游戏类型
     */
    GameType getGameType();
    
    /**
     * 初始化游戏房间
     * @param room 游戏房间
     */
    void initializeGame(GameRoom room);
    
    /**
     * 开始新一轮游戏
     * @param room 游戏房间
     */
    void startNewRound(GameRoom room);
    
    /**
     * 处理玩家动作
     * @param room 游戏房间
     * @param session 玩家会话
     * @param action 动作类型
     * @param data 动作数据
     * @return 是否处理成功
     */
    boolean handlePlayerAction(GameRoom room, WebSocketSession session, String action, Map<String, Object> data);
    
    /**
     * 检查游戏是否结束
     * @param room 游戏房间
     * @return 是否已结束
     */
    boolean isGameOver(GameRoom room);
    
    /**
     * 计算最终得分
     * @param room 游戏房间
     * @return 玩家得分映射
     */
    Map<String, Integer> calculateFinalScores(GameRoom room);
    
    /**
     * 获取游戏状态（用于序列化发送给客户端）
     * @param room 游戏房间
     * @param forPlayer 为特定玩家准备的状态（可能需要隐藏某些信息）
     * @return 游戏状态数据
     */
    Map<String, Object> getGameStateForPlayer(GameRoom room, Player forPlayer);
    
    /**
     * 清理游戏资源
     * @param room 游戏房间
     */
    void cleanupGame(GameRoom room);
    
    /**
     * 验证玩家动作是否合法
     * @param room 游戏房间
     * @param player 玩家
     * @param action 动作类型
     * @param data 动作数据
     * @return 是否合法
     */
    boolean validatePlayerAction(GameRoom room, Player player, String action, Map<String, Object> data);
    
    /**
     * 处理玩家断线
     * @param room 游戏房间
     * @param player 断线玩家
     */
    void handlePlayerDisconnect(GameRoom room, Player player);
    
    /**
     * 处理玩家重连
     * @param room 游戏房间
     * @param player 重连玩家
     */
    void handlePlayerReconnect(GameRoom room, Player player);
    
    /**
     * 获取游戏配置
     * @return 游戏配置信息
     */
    GameConfiguration getGameConfiguration();
}

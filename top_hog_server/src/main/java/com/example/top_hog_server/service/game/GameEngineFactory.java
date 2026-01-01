package com.example.top_hog_server.service.game;

import com.example.top_hog_server.model.GameType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏引擎工厂
 * 负责管理和提供不同类型的游戏引擎实例
 */
@Service
public class GameEngineFactory {
    
    private final Map<GameType, GameEngine> engineMap = new HashMap<>();
    
    /**
     * 构造函数，自动注入所有GameEngine实现
     */
    @Autowired
    public GameEngineFactory(List<GameEngine> engines) {
        for (GameEngine engine : engines) {
            engineMap.put(engine.getGameType(), engine);
        }
    }
    
    /**
     * 根据游戏类型获取对应的游戏引擎
     * @param gameType 游戏类型
     * @return 游戏引擎实例
     * @throws IllegalArgumentException 如果游戏类型不支持
     */
    public GameEngine getEngine(GameType gameType) {
        GameEngine engine = engineMap.get(gameType);
        if (engine == null) {
            throw new IllegalArgumentException("不支持的游戏类型: " + gameType);
        }
        return engine;
    }
    
    /**
     * 检查是否支持指定的游戏类型
     * @param gameType 游戏类型
     * @return 是否支持
     */
    public boolean isSupported(GameType gameType) {
        return engineMap.containsKey(gameType);
    }
    
    /**
     * 获取所有支持的游戏类型
     * @return 游戏类型列表
     */
    public List<GameType> getSupportedGameTypes() {
        return List.copyOf(engineMap.keySet());
    }
}

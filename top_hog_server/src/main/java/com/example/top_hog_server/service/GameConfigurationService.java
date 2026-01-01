package com.example.top_hog_server.service;

import com.example.top_hog_server.model.GameConfiguration;
import com.example.top_hog_server.model.GameType;
import com.example.top_hog_server.repository.GameConfigurationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

/**
 * 游戏配置服务
 * 管理游戏配置的CRUD操作和初始化
 */
@Service
public class GameConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GameConfigurationService.class);
    
    private final GameConfigurationRepository configRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public GameConfigurationService(GameConfigurationRepository configRepository) {
        this.configRepository = configRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 初始化默认游戏配置
     * 如果数据库中没有配置，则创建默认配置
     */
    @PostConstruct
    @Transactional
    public void initializeDefaultConfigurations() {
        logger.info("初始化游戏配置...");
        
        // 检查是否已有TOP_HOG配置
        if (!configRepository.existsByGameTypeCode(GameType.TOP_HOG.getCode())) {
            GameConfiguration topHogConfig = new GameConfiguration();
            topHogConfig.setGameTypeCode(GameType.TOP_HOG.getCode());
            topHogConfig.setDisplayName(GameType.TOP_HOG.getDisplayName());
            topHogConfig.setDescription(GameType.TOP_HOG.getDescription());
            topHogConfig.setMinPlayers(2);
            topHogConfig.setMaxPlayers(10);
            topHogConfig.setEnabled(true);
            topHogConfig.setDisplayOrder(1);
            topHogConfig.setRulesDescription(
                "游戏目标：避免收集牛头卡牌，拥有最少牛头数的玩家获胜。\n" +
                "游戏规则：\n" +
                "1. 每位玩家获得10张手牌\n" +
                "2. 同时选择一张牌打出\n" +
                "3. 按数字大小依次放置到4个牌列中\n" +
                "4. 当牌列满6张时，下一个玩家需要收走该列并获得相应牛头数\n" +
                "5. 游戏进行多轮，累计牛头数最少者获胜"
            );
            
            // 游戏特定配置（JSON格式）
            String specificConfig = "{\n" +
                "  \"deckSize\": 104,\n" +
                "  \"handSize\": 10,\n" +
                "  \"rowCount\": 4,\n" +
                "  \"maxCardsPerRow\": 5,\n" +
                "  \"defaultMaxRounds\": 3,\n" +
                "  \"defaultTargetScore\": 66,\n" +
                "  \"playerChoiceTimeoutMs\": 30000\n" +
                "}";
            topHogConfig.setGameSpecificConfig(specificConfig);
            
            configRepository.save(topHogConfig);
            logger.info("已创建TOP_HOG默认配置");
        }
        
        logger.info("游戏配置初始化完成");
    }
    
    /**
     * 获取所有启用的游戏配置
     */
    public List<GameConfiguration> getAllEnabledGames() {
        return configRepository.findByEnabledTrueOrderByDisplayOrder();
    }
    
    /**
     * 根据游戏类型代码获取配置
     */
    public Optional<GameConfiguration> getGameConfiguration(String gameTypeCode) {
        return configRepository.findByGameTypeCode(gameTypeCode);
    }
    
    /**
     * 创建或更新游戏配置
     */
    @Transactional
    public GameConfiguration saveOrUpdateConfiguration(GameConfiguration config) {
        return configRepository.save(config);
    }
    
    /**
     * 启用或禁用游戏
     */
    @Transactional
    public boolean setGameEnabled(String gameTypeCode, boolean enabled) {
        Optional<GameConfiguration> configOpt = configRepository.findByGameTypeCode(gameTypeCode);
        if (configOpt.isPresent()) {
            GameConfiguration config = configOpt.get();
            config.setEnabled(enabled);
            configRepository.save(config);
            return true;
        }
        return false;
    }
    
    /**
     * 删除游戏配置
     */
    @Transactional
    public boolean deleteConfiguration(String gameTypeCode) {
        Optional<GameConfiguration> configOpt = configRepository.findByGameTypeCode(gameTypeCode);
        if (configOpt.isPresent()) {
            configRepository.delete(configOpt.get());
            return true;
        }
        return false;
    }
    
    /**
     * 重新加载配置（用于热更新）
     */
    public void reloadConfigurations() {
        logger.info("重新加载游戏配置...");
        // 触发缓存刷新或其他必要操作
        List<GameConfiguration> configs = configRepository.findAll();
        logger.info("已加载 {} 个游戏配置", configs.size());
    }
}

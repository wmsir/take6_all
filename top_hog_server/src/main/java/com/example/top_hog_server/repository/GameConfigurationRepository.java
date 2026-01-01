package com.example.top_hog_server.repository;

import com.example.top_hog_server.model.GameConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 游戏配置数据访问层
 */
@Repository
public interface GameConfigurationRepository extends JpaRepository<GameConfiguration, Long> {
    
    /**
     * 根据游戏类型代码查找配置
     */
    Optional<GameConfiguration> findByGameTypeCode(String gameTypeCode);
    
    /**
     * 查找所有启用的游戏配置
     */
    List<GameConfiguration> findByEnabledTrueOrderByDisplayOrder();
    
    /**
     * 检查游戏类型代码是否存在
     */
    boolean existsByGameTypeCode(String gameTypeCode);
}

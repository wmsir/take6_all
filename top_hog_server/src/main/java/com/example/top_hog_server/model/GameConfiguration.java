package com.example.top_hog_server.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 游戏配置实体
 * 存储每种游戏的配置信息，支持热更新
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "game_configurations")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 游戏类型代码（唯一标识）
     */
    @Column(unique = true, nullable = false)
    private String gameTypeCode;
    
    /**
     * 游戏显示名称
     */
    @Column(nullable = false)
    private String displayName;
    
    /**
     * 游戏描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 最小玩家数
     */
    private int minPlayers = 2;
    
    /**
     * 最大玩家数
     */
    private int maxPlayers = 10;
    
    /**
     * 是否启用该游戏
     */
    private boolean enabled = true;
    
    /**
     * 游戏图标URL
     */
    private String iconUrl;
    
    /**
     * 游戏特定配置（JSON格式存储）
     */
    @Column(columnDefinition = "TEXT")
    private String gameSpecificConfig;
    
    /**
     * 游戏规则说明
     */
    @Column(columnDefinition = "TEXT")
    private String rulesDescription;
    
    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 排序顺序（用于前端显示）
     */
    private int displayOrder = 0;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

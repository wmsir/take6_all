package com.example.top_hog_server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 成就定义实体
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "achievement")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 成就名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 成就描述
     */
    @Column(nullable = false)
    private String description;

    /**
     * 成就类型: GAMES_PLAYED(游戏场次), GAMES_WON(胜利场次), TOTAL_SCORE(总分), RECHARGE(充值)
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * 目标值 (如: 达到100场)
     */
    @Column(nullable = false)
    private Long targetValue;

    /**
     * 奖励类型: COINS, DIAMONDS
     */
    @Column(nullable = false, length = 20)
    private String rewardType;

    /**
     * 奖励数量
     */
    @Column(nullable = false)
    private Integer rewardAmount;

    /**
     * 图标URL
     */
    @Column(length = 255)
    private String iconUrl;

    /**
     * 排序权重
     */
    private Integer displayOrder = 0;

    public Achievement(String name, String description, String type, Long targetValue, String rewardType,
            Integer rewardAmount) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.targetValue = targetValue;
        this.rewardType = rewardType;
        this.rewardAmount = rewardAmount;
    }
}

package com.example.top_hog_server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每日任务定义
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "daily_task")
public class DailyTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 任务名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 任务描述
     */
    @Column(nullable = false)
    private String description;

    /**
     * 任务类型: LOGIN(登录), GAME_PLAY(对局), GAME_WIN(胜利), SHARE(分享)
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * 目标值
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
     * 排序权重
     */
    private Integer displayOrder = 0;

    public DailyTask(String name, String description, String type, Long targetValue, String rewardType,
            Integer rewardAmount) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.targetValue = targetValue;
        this.rewardType = rewardType;
        this.rewardAmount = rewardAmount;
    }
}

package com.example.top_hog_server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户成就进度实体
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "user_achievement", indexes = {
        @Index(name = "idx_user_achievement_user_id", columnList = "user_id"),
        @Index(name = "idx_user_achievement_achievement_id", columnList = "achievement_id")
})
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "achievement_id", nullable = false)
    private Long achievementId;

    /**
     * 当前进度值
     */
    @Column(nullable = false)
    private Long currentValue = 0L;

    /**
     * 是否达成
     */
    @Column(nullable = false)
    private Boolean isCompleted = false;

    /**
     * 是否已领取奖励
     */
    @Column(nullable = false)
    private Boolean isRewardClaimed = false;

    /**
     * 达成时间
     */
    private LocalDateTime completedAt;

    /**
     * 领取奖励时间
     */
    private LocalDateTime claimedAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserAchievement(Long userId, Long achievementId) {
        this.userId = userId;
        this.achievementId = achievementId;
        this.currentValue = 0L;
        this.updatedAt = LocalDateTime.now();
    }
}

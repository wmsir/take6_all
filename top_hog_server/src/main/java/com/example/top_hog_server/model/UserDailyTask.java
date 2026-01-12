package com.example.top_hog_server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日任务进度
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "user_daily_task", indexes = {
        @Index(name = "idx_udt_user_date", columnList = "user_id, task_date")
})
public class UserDailyTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /**
     * 任务日期
     */
    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    /**
     * 当前进度
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

    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserDailyTask(Long userId, Long taskId, LocalDate taskDate) {
        this.userId = userId;
        this.taskId = taskId;
        this.taskDate = taskDate;
        this.currentValue = 0L;
        this.updatedAt = LocalDateTime.now();
    }
}

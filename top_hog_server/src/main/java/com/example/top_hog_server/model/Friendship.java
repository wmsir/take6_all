package com.example.top_hog_server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友关系实体
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "friendship", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_friend_id", columnList = "friend_id"),
        @Index(name = "idx_status", columnList = "status")
})
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    /**
     * 状态: PENDING(待确认), ACCEPTED(已接受), REJECTED(已拒绝), BLOCKED(已屏蔽)
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /**
     * 备注名
     */
    @Column(name = "remark", length = 50)
    private String remark;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Friendship(Long userId, Long friendId) {
        this.userId = userId;
        this.friendId = friendId;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}

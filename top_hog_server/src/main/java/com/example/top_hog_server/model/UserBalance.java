package com.example.top_hog_server.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户余额实体
 */
@Entity
@Table(name = "user_balance")
@Data
public class UserBalance {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long coins = 0L;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long diamonds = 0L;

    @Column(name = "total_recharge", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal totalRecharge = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

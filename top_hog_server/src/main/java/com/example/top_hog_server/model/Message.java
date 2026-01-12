package com.example.top_hog_server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 站内信实体
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "message", indexes = {
        @Index(name = "idx_receiver_id", columnList = "receiver_id"),
        @Index(name = "idx_type", columnList = "type"),
        @Index(name = "idx_is_read", columnList = "is_read"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 消息类型: SYSTEM(系统公告), PERSONAL(个人消息), REWARD(奖励通知), FRIEND(好友消息)
     */
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    /**
     * 发送者ID (系统消息为null)
     */
    @Column(name = "sender_id")
    private Long senderId;

    /**
     * 接收者ID (系统公告为null,表示全员)
     */
    @Column(name = "receiver_id")
    private Long receiverId;

    /**
     * 消息标题
     */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 消息内容
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 是否已读
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * 关联数据(JSON格式,如奖励详情)
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    /**
     * 过期时间
     */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 阅读时间
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Message(String type, String title, String content) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }
}

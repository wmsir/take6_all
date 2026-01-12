package com.example.top_hog_server.repository;

import com.example.top_hog_server.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息Repository
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 查找用户的所有消息
     */
    List<Message> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    /**
     * 查找用户未读消息
     */
    List<Message> findByReceiverIdAndIsReadOrderByCreatedAtDesc(Long receiverId, Boolean isRead);

    /**
     * 查找系统公告(receiverId为null)
     */
    List<Message> findByReceiverIdIsNullAndTypeOrderByCreatedAtDesc(String type);

    /**
     * 查找特定类型的消息
     */
    List<Message> findByReceiverIdAndTypeOrderByCreatedAtDesc(Long receiverId, String type);

    /**
     * 统计未读消息数
     */
    long countByReceiverIdAndIsRead(Long receiverId, Boolean isRead);

    /**
     * 删除过期消息
     */
    void deleteByExpireAtBefore(LocalDateTime now);
}

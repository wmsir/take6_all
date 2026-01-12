package com.example.top_hog_server.service;

import com.example.top_hog_server.exception.BusinessException;
import com.example.top_hog_server.exception.ErrorCode;
import com.example.top_hog_server.model.Message;
import com.example.top_hog_server.model.User;
import com.example.top_hog_server.repository.MessageRepository;
import com.example.top_hog_server.repository.UserRepository;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息推送服务
 */
@Service
@Slf4j
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 发送系统公告(全员)
     */
    @Transactional
    public void sendSystemAnnouncement(String title, String content, Integer expireDays) {
        Message message = new Message("SYSTEM", title, content);
        message.setReceiverId(null); // null表示全员

        if (expireDays != null && expireDays > 0) {
            message.setExpireAt(LocalDateTime.now().plusDays(expireDays));
        }

        messageRepository.save(message);
        log.info("发送系统公告: {}", title);
    }

    /**
     * 发送个人消息
     */
    @Transactional
    public void sendPersonalMessage(Long receiverId, String title, String content) {
        // 验证接收者存在
        userRepository.findById(receiverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "接收者不存在"));

        Message message = new Message("PERSONAL", title, content);
        message.setReceiverId(receiverId);

        messageRepository.save(message);
        log.info("发送个人消息给用户{}: {}", receiverId, title);
    }

    /**
     * 发送奖励通知
     */
    @Transactional
    public void sendRewardNotification(Long receiverId, String title, String content, String extraData) {
        Message message = new Message("REWARD", title, content);
        message.setReceiverId(receiverId);
        message.setExtraData(extraData);

        messageRepository.save(message);
        log.info("发送奖励通知给用户{}: {}", receiverId, title);
    }

    /**
     * 发送好友消息
     */
    @Transactional
    public void sendFriendMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "发送者不存在"));

        Message message = new Message("FRIEND", "好友消息", content);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);

        messageRepository.save(message);
        log.info("用户{}发送好友消息给用户{}", senderId, receiverId);
    }

    /**
     * 获取用户消息列表
     */
    public Map<String, Object> getUserMessages(String type, Boolean isRead) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        List<Message> messages;

        if (type != null && !type.isEmpty()) {
            messages = messageRepository.findByReceiverIdAndTypeOrderByCreatedAtDesc(userId, type);
        } else if (isRead != null) {
            messages = messageRepository.findByReceiverIdAndIsReadOrderByCreatedAtDesc(userId, isRead);
        } else {
            messages = messageRepository.findByReceiverIdOrderByCreatedAtDesc(userId);
        }

        // 获取系统公告
        List<Message> systemMessages = messageRepository.findByReceiverIdIsNullAndTypeOrderByCreatedAtDesc("SYSTEM");

        // 合并个人消息和系统公告
        List<Message> allMessages = new ArrayList<>();
        allMessages.addAll(messages);
        allMessages.addAll(systemMessages);

        // 按时间排序
        allMessages.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 构建返回数据
        List<Map<String, Object>> messageList = allMessages.stream()
                .map(this::buildMessageInfo)
                .collect(Collectors.toList());

        // 统计未读数
        long unreadCount = messageRepository.countByReceiverIdAndIsRead(userId, false);
        long systemUnreadCount = systemMessages.stream().filter(m -> !m.getIsRead()).count();

        Map<String, Object> result = new HashMap<>();
        result.put("messages", messageList);
        result.put("total", allMessages.size());
        result.put("unreadCount", unreadCount + systemUnreadCount);

        return result;
    }

    /**
     * 标记消息为已读
     */
    @Transactional
    public void markAsRead(Long messageId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "消息不存在"));

        // 验证权限(系统公告或个人消息)
        if (message.getReceiverId() != null && !message.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "无权操作此消息");
        }

        if (!message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);
        }
    }

    /**
     * 标记所有消息为已读
     */
    @Transactional
    public void markAllAsRead() {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        List<Message> unreadMessages = messageRepository.findByReceiverIdAndIsReadOrderByCreatedAtDesc(userId, false);

        for (Message message : unreadMessages) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
        }

        messageRepository.saveAll(unreadMessages);
        log.info("用户{}标记所有消息为已读", userId);
    }

    /**
     * 删除消息
     */
    @Transactional
    public void deleteMessage(Long messageId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "消息不存在"));

        // 验证权限
        if (message.getReceiverId() != null && !message.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "无权删除此消息");
        }

        messageRepository.delete(message);
    }

    /**
     * 获取未读消息数
     */
    public long getUnreadCount() {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        long personalUnread = messageRepository.countByReceiverIdAndIsRead(userId, false);
        long systemUnread = messageRepository.findByReceiverIdIsNullAndTypeOrderByCreatedAtDesc("SYSTEM")
                .stream().filter(m -> !m.getIsRead()).count();

        return personalUnread + systemUnread;
    }

    /**
     * 清理过期消息(定时任务)
     */
    @Transactional
    public void cleanExpiredMessages() {
        messageRepository.deleteByExpireAtBefore(LocalDateTime.now());
        log.info("清理过期消息完成");
    }

    /**
     * 构建消息信息
     */
    private Map<String, Object> buildMessageInfo(Message message) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", message.getId());
        info.put("type", message.getType());
        info.put("title", message.getTitle());
        info.put("content", message.getContent());
        info.put("isRead", message.getIsRead());
        info.put("extraData", message.getExtraData());
        info.put("createdAt", message.getCreatedAt());
        info.put("readAt", message.getReadAt());

        // 如果有发送者,获取发送者信息
        if (message.getSenderId() != null) {
            User sender = userRepository.findById(message.getSenderId()).orElse(null);
            if (sender != null) {
                Map<String, Object> senderInfo = new HashMap<>();
                senderInfo.put("id", sender.getId());
                senderInfo.put("nickname", sender.getNickname());
                senderInfo.put("avatarUrl", sender.getAvatarUrl());
                info.put("sender", senderInfo);
            }
        }

        return info;
    }
}

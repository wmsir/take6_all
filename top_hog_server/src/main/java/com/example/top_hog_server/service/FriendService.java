package com.example.top_hog_server.service;

import com.example.top_hog_server.exception.BusinessException;
import com.example.top_hog_server.exception.ErrorCode;
import com.example.top_hog_server.model.Friendship;
import com.example.top_hog_server.model.User;
import com.example.top_hog_server.repository.FriendshipRepository;
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
 * 好友服务
 */
@Service
@Slf4j
public class FriendService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 添加好友
     */
    @Transactional
    public void addFriend(Long friendId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        // 不能添加自己为好友
        if (userId.equals(friendId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "不能添加自己为好友");
        }

        // 检查好友是否存在
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        // 检查是否已经是好友
        if (friendshipRepository.existsByUserIdAndFriendIdAndStatus(userId, friendId, "ACCEPTED")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "已经是好友了");
        }

        // 检查是否已发送过请求
        Optional<Friendship> existing = friendshipRepository.findByUserIdAndFriendId(userId, friendId);
        if (existing.isPresent() && "PENDING".equals(existing.get().getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "已发送过好友请求");
        }

        // 创建好友请求
        Friendship friendship = new Friendship(userId, friendId);
        friendshipRepository.save(friendship);

        log.info("用户{}向用户{}发送好友请求", userId, friendId);
    }

    /**
     * 接受好友请求
     */
    @Transactional
    public void acceptFriend(Long requestId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        Friendship request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "好友请求不存在"));

        // 验证是否是发给当前用户的请求
        if (!request.getFriendId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "无权操作此请求");
        }

        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "请求已处理");
        }

        // 更新请求状态
        request.setStatus("ACCEPTED");
        request.setUpdatedAt(LocalDateTime.now());
        friendshipRepository.save(request);

        // 创建反向好友关系
        Friendship reverseFriendship = new Friendship(userId, request.getUserId());
        reverseFriendship.setStatus("ACCEPTED");
        friendshipRepository.save(reverseFriendship);

        log.info("用户{}接受了用户{}的好友请求", userId, request.getUserId());
    }

    /**
     * 拒绝好友请求
     */
    @Transactional
    public void rejectFriend(Long requestId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        Friendship request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "好友请求不存在"));

        if (!request.getFriendId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "无权操作此请求");
        }

        request.setStatus("REJECTED");
        request.setUpdatedAt(LocalDateTime.now());
        friendshipRepository.save(request);

        log.info("用户{}拒绝了用户{}的好友请求", userId, request.getUserId());
    }

    /**
     * 删除好友
     */
    @Transactional
    public void deleteFriend(Long friendId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        // 删除双向好友关系
        friendshipRepository.findByUserIdAndFriendId(userId, friendId)
                .ifPresent(friendshipRepository::delete);

        friendshipRepository.findByUserIdAndFriendId(friendId, userId)
                .ifPresent(friendshipRepository::delete);

        log.info("用户{}删除了好友{}", userId, friendId);
    }

    /**
     * 获取好友列表
     */
    public List<Map<String, Object>> getFriendList() {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, "ACCEPTED");

        return friendships.stream()
                .map(friendship -> {
                    User friend = userRepository.findById(friendship.getFriendId()).orElse(null);
                    if (friend == null)
                        return null;

                    Map<String, Object> friendInfo = new HashMap<>();
                    friendInfo.put("friendshipId", friendship.getId());
                    friendInfo.put("userId", friend.getId());
                    friendInfo.put("nickname", friend.getNickname());
                    friendInfo.put("avatarUrl", friend.getAvatarUrl());
                    friendInfo.put("isVip", friend.isVip());
                    friendInfo.put("remark", friendship.getRemark());
                    friendInfo.put("createdAt", friendship.getCreatedAt());

                    return friendInfo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取好友请求列表
     */
    public List<Map<String, Object>> getFriendRequests() {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        List<Friendship> requests = friendshipRepository.findByFriendIdAndStatus(userId, "PENDING");

        return requests.stream()
                .map(request -> {
                    User requester = userRepository.findById(request.getUserId()).orElse(null);
                    if (requester == null)
                        return null;

                    Map<String, Object> requestInfo = new HashMap<>();
                    requestInfo.put("requestId", request.getId());
                    requestInfo.put("userId", requester.getId());
                    requestInfo.put("nickname", requester.getNickname());
                    requestInfo.put("avatarUrl", requester.getAvatarUrl());
                    requestInfo.put("createdAt", request.getCreatedAt());

                    return requestInfo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 设置好友备注
     */
    @Transactional
    public void setFriendRemark(Long friendId, String remark) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        Friendship friendship = friendshipRepository.findByUserIdAndFriendId(userId, friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "好友关系不存在"));

        friendship.setRemark(remark);
        friendship.setUpdatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        log.info("用户{}设置好友{}的备注为: {}", userId, friendId, remark);
    }
}

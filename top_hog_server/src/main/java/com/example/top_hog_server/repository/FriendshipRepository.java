package com.example.top_hog_server.repository;

import com.example.top_hog_server.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 好友关系Repository
 */
@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /**
     * 查找用户的所有好友关系
     */
    List<Friendship> findByUserIdAndStatus(Long userId, String status);

    /**
     * 查找用户的所有好友关系(包括待确认)
     */
    List<Friendship> findByUserId(Long userId);

    /**
     * 查找好友请求
     */
    List<Friendship> findByFriendIdAndStatus(Long friendId, String status);

    /**
     * 查找特定好友关系
     */
    Optional<Friendship> findByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 检查是否已是好友
     */
    boolean existsByUserIdAndFriendIdAndStatus(Long userId, Long friendId, String status);
}

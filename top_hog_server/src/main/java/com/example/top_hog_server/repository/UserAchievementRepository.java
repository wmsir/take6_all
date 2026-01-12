package com.example.top_hog_server.repository;

import com.example.top_hog_server.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    /**
     * 查找用户的特定成就进度
     */
    Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);

    /**
     * 查找用户的所有成就进度
     */
    List<UserAchievement> findByUserId(Long userId);

    /**
     * 查找用户已完成但未领取奖励的成就
     */
    List<UserAchievement> findByUserIdAndIsCompletedTrueAndIsRewardClaimedFalse(Long userId);
}

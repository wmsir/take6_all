package com.example.top_hog_server.service;

import com.example.top_hog_server.exception.BusinessException;
import com.example.top_hog_server.exception.ErrorCode;
import com.example.top_hog_server.model.Achievement;
import com.example.top_hog_server.model.UserAchievement;
import com.example.top_hog_server.repository.AchievementRepository;
import com.example.top_hog_server.repository.UserAchievementRepository;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成就系统服务
 */
@Service
@Slf4j
public class AchievementService {

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MessageService messageService;

    /**
     * 初始化默认成就 (通常在系统启动或通过管理接口调用)
     */
    @Transactional
    public void initDefaultAchievements() {
        if (achievementRepository.count() > 0)
            return;

        List<Achievement> defaults = Arrays.asList(
                new Achievement("初出茅庐", "完成1场游戏", "GAMES_PLAYED", 1L, "COINS", 100),
                new Achievement("渐入佳境", "完成10场游戏", "GAMES_PLAYED", 10L, "COINS", 500),
                new Achievement("游戏达人", "完成100场游戏", "GAMES_PLAYED", 100L, "DIAMONDS", 50),
                new Achievement("首战告捷", "获得1场胜利", "GAMES_WON", 1L, "COINS", 200),
                new Achievement("常胜将军", "获得10场胜利", "GAMES_WON", 10L, "DIAMONDS", 20),
                new Achievement("独孤求败", "获得50场胜利", "GAMES_WON", 50L, "DIAMONDS", 100),
                new Achievement("小试牛刀", "累计获得1000分", "TOTAL_SCORE", 1000L, "COINS", 300),
                new Achievement("得分机器", "累计获得10000分", "TOTAL_SCORE", 10000L, "DIAMONDS", 30));

        achievementRepository.saveAll(defaults);
        log.info("初始化默认成就完成");
    }

    /**
     * 更新成就进度
     * 
     * @param userId    用户ID
     * @param type      成就类型 (GAMES_PLAYED, GAMES_WON, TOTAL_SCORE)
     * @param increment 增加的数值
     */
    @Async
    @Transactional
    public void updateProgress(Long userId, String type, long increment) {
        List<Achievement> achievements = achievementRepository.findByType(type);
        if (achievements.isEmpty())
            return;

        for (Achievement achievement : achievements) {
            UserAchievement userAchievement = userAchievementRepository
                    .findByUserIdAndAchievementId(userId, achievement.getId())
                    .orElse(new UserAchievement(userId, achievement.getId()));

            // 如果已经完成，跳过
            if (userAchievement.getIsCompleted())
                continue;

            // 更新进度
            long newValue = userAchievement.getCurrentValue() + increment;
            userAchievement.setCurrentValue(newValue);
            userAchievement.setUpdatedAt(LocalDateTime.now());

            // 检查是否达成
            if (newValue >= achievement.getTargetValue()) {
                userAchievement.setIsCompleted(true);
                userAchievement.setCompletedAt(LocalDateTime.now());

                // 发送站内信通知
                messageService.sendRewardNotification(
                        userId,
                        "成就达成: " + achievement.getName(),
                        "恭喜您达成成就【" + achievement.getName() + "】，快去领取奖励吧！",
                        null);

                log.info("用户{}达成成就: {}", userId, achievement.getName());
            }

            userAchievementRepository.save(userAchievement);
        }
    }

    /**
     * 获取用户成就列表（包含进度）
     */
    public List<Map<String, Object>> getUserAchievements() {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        List<Achievement> allAchievements = achievementRepository.findAllByOrderByDisplayOrderAsc();
        List<UserAchievement> userProgress = userAchievementRepository.findByUserId(userId);

        Map<Long, UserAchievement> progressMap = userProgress.stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, ua -> ua));

        return allAchievements.stream().map(achievement -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", achievement.getId());
            info.put("name", achievement.getName());
            info.put("description", achievement.getDescription());
            info.put("iconUrl", achievement.getIconUrl());
            info.put("targetValue", achievement.getTargetValue());
            info.put("rewardType", achievement.getRewardType());
            info.put("rewardAmount", achievement.getRewardAmount());

            UserAchievement progress = progressMap.get(achievement.getId());
            if (progress != null) {
                info.put("currentValue", progress.getCurrentValue());
                info.put("isCompleted", progress.getIsCompleted());
                info.put("isRewardClaimed", progress.getIsRewardClaimed());
                info.put("completedAt", progress.getCompletedAt());
            } else {
                info.put("currentValue", 0);
                info.put("isCompleted", false);
                info.put("isRewardClaimed", false);
            }

            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 领取成就奖励
     */
    @Transactional
    public Map<String, Object> claimReward(Long achievementId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        UserAchievement userAchievement = userAchievementRepository
                .findByUserIdAndAchievementId(userId, achievementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "未找到该成就进度"));

        if (!userAchievement.getIsCompleted()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "成就尚未达成");
        }

        if (userAchievement.getIsRewardClaimed()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "奖励已领取");
        }

        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "成就配置不存在"));

        // 发放奖励
        if ("COINS".equals(achievement.getRewardType())) {
            paymentService.addCoins(userId, achievement.getRewardAmount(), "成就奖励: " + achievement.getName());
        } else if ("DIAMONDS".equals(achievement.getRewardType())) {
            paymentService.addDiamonds(userId, achievement.getRewardAmount(), "成就奖励: " + achievement.getName());
        }

        // 更新状态
        userAchievement.setIsRewardClaimed(true);
        userAchievement.setClaimedAt(LocalDateTime.now());
        userAchievementRepository.save(userAchievement);

        Map<String, Object> result = new HashMap<>();
        result.put("rewardType", achievement.getRewardType());
        result.put("rewardAmount", achievement.getRewardAmount());

        return result;
    }
}

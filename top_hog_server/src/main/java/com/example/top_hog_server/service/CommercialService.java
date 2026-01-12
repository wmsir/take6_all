package com.example.top_hog_server.service;

import com.example.top_hog_server.exception.BusinessException;
import com.example.top_hog_server.exception.ErrorCode;
import com.example.top_hog_server.model.User;
import com.example.top_hog_server.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 商业化相关服务 (广告、引导等)
 */
@Service
@Slf4j
public class CommercialService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentService paymentService;

    // 广告奖励配置
    private static final int AD_REWARD_COINS = 50;
    private static final int AD_LIMIT_PER_DAY = 10; // 每日限制次数 (简化版暂时只在内存/日志体现，或者暂不限制)

    // 新手引导奖励
    private static final int GUIDE_REWARD_COINS = 100;

    /**
     * 发放广告奖励
     */
    @Transactional
    public Map<String, Object> rewardAd(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        // TODO: 校验广告观看凭证 (signature等)，这里简化为直接发放
        // TODO: 检查每日观看次数限制 (需要 Redis 或数据库记录)

        // 发放金币
        paymentService.addCoins(userId, AD_REWARD_COINS, "观看广告奖励");

        log.info("用户观看广告获得奖励: userId={}, coins={}", userId, AD_REWARD_COINS);

        Map<String, Object> result = new HashMap<>();
        result.put("rewardCoins", AD_REWARD_COINS);
        result.put("currentCoins", -1); // 前端通常会重新拉取余额，或者这里查询后返回。为简化暂不返回最新余额

        return result;
    }

    /**
     * 完成新手引导
     */
    @Transactional
    public Map<String, Object> finishGuide(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        if (user.isGuideCompleted()) {
            // 已经完成过，不再重复发放奖励，直接返回成功
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("rewarded", false);
            return result;
        }

        // 更新状态
        user.setIsGuideCompleted(true);
        userRepository.save(user);

        // 发放新手奖励
        paymentService.addCoins(userId, GUIDE_REWARD_COINS, "完成新手引导奖励");

        log.info("用户完成新手引导: userId={}, reward={}", userId, GUIDE_REWARD_COINS);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("rewarded", true);
        result.put("rewardCoins", GUIDE_REWARD_COINS);

        return result;
    }
}

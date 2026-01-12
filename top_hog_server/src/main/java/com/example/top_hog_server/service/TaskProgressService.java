package com.example.top_hog_server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 任务进度统一处理服务
 * 负责接收业务事件并分发给成就系统和每日任务系统
 */
@Service
@Slf4j
public class TaskProgressService {

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private DailyTaskService dailyTaskService;

    /**
     * 处理游戏结束事件
     * 
     * @param userId 用户ID
     * @param isWin  是否胜利
     * @param score  获得分数
     */
    @Async
    public void onGameFinished(Long userId, boolean isWin, int score) {
        log.info("处理游戏结束任务进度: userId={}, isWin={}, score={}", userId, isWin, score);

        try {
            // 更新成就进度
            achievementService.updateProgress(userId, "GAMES_PLAYED", 1);
            achievementService.updateProgress(userId, "TOTAL_SCORE", score);
            if (isWin) {
                achievementService.updateProgress(userId, "GAMES_WON", 1);
            }

            // 更新每日任务进度
            dailyTaskService.updateProgress(userId, "GAME_PLAY", 1);
            if (isWin) {
                dailyTaskService.updateProgress(userId, "GAME_WIN", 1);
            }
        } catch (Exception e) {
            log.error("更新游戏相关任务进度失败", e);
        }
    }

    /**
     * 处理登录事件
     */
    @Async
    public void onLogin(Long userId) {
        log.info("处理登录任务进度: userId={}", userId);
        try {
            dailyTaskService.updateProgress(userId, "LOGIN", 1);
        } catch (Exception e) {
            log.error("更新登录任务进度失败", e);
        }
    }

    /**
     * 处理充值事件
     */
    @Async
    public void onRecharge(Long userId, long actualAmount) {
        // 预留: 未来可能有的充值相关任务或成就
    }
}

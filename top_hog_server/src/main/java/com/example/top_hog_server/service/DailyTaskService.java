package com.example.top_hog_server.service;

import com.example.top_hog_server.exception.BusinessException;
import com.example.top_hog_server.exception.ErrorCode;
import com.example.top_hog_server.model.DailyTask;
import com.example.top_hog_server.model.UserDailyTask;
import com.example.top_hog_server.repository.DailyTaskRepository;
import com.example.top_hog_server.repository.UserDailyTaskRepository;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 每日任务服务
 */
@Service
@Slf4j
public class DailyTaskService {

    @Autowired
    private DailyTaskRepository dailyTaskRepository;

    @Autowired
    private UserDailyTaskRepository userDailyTaskRepository;

    @Autowired
    private PaymentService paymentService;

    /**
     * 初始化默认每日任务
     */
    @Transactional
    public void initDefaultTasks() {
        if (dailyTaskRepository.count() > 0)
            return;

        List<DailyTask> defaults = Arrays.asList(
                new DailyTask("每日签到", "登录游戏", "LOGIN", 1L, "COINS", 50),
                new DailyTask("小试身手", "完成3场游戏", "GAME_PLAY", 3L, "COINS", 100),
                new DailyTask("越战越勇", "完成10场游戏", "GAME_PLAY", 10L, "COINS", 300),
                new DailyTask("旗开得胜", "获得1场胜利", "GAME_WIN", 1L, "COINS", 150),
                new DailyTask("荣耀时刻", "获得3场胜利", "GAME_WIN", 3L, "DIAMONDS", 10),
                new DailyTask("分享快乐", "分享游戏给好友", "SHARE", 1L, "COINS", 100));

        dailyTaskRepository.saveAll(defaults);
        log.info("初始化默认每日任务完成");
    }

    /**
     * 获取今日任务列表
     */
    public List<Map<String, Object>> getTodayTasks() {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();
        LocalDate today = LocalDate.now();

        List<DailyTask> allTasks = dailyTaskRepository.findAllByOrderByDisplayOrderAsc();

        // 确保今日任务进度记录已存在
        ensureDailyTasksCreated(userId, today, allTasks);

        List<UserDailyTask> userTasks = userDailyTaskRepository.findByUserIdAndTaskDate(userId, today);
        Map<Long, UserDailyTask> taskMap = userTasks.stream()
                .collect(Collectors.toMap(UserDailyTask::getTaskId, t -> t));

        return allTasks.stream().map(task -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", task.getId());
            info.put("name", task.getName());
            info.put("description", task.getDescription());
            info.put("type", task.getType());
            info.put("targetValue", task.getTargetValue());
            info.put("rewardType", task.getRewardType());
            info.put("rewardAmount", task.getRewardAmount());

            UserDailyTask progress = taskMap.get(task.getId());
            if (progress != null) {
                info.put("currentValue", progress.getCurrentValue());
                info.put("isCompleted", progress.getIsCompleted());
                info.put("isRewardClaimed", progress.getIsRewardClaimed());
            } else {
                // Should not happen if ensureDailyTasksCreated works correctly
                info.put("currentValue", 0);
                info.put("isCompleted", false);
                info.put("isRewardClaimed", false);
            }

            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 更新每日任务进度
     */
    @Async
    @Transactional
    public void updateProgress(Long userId, String type, long increment) {
        LocalDate today = LocalDate.now();
        List<DailyTask> tasks = dailyTaskRepository.findByType(type);
        if (tasks.isEmpty())
            return;

        // 确保记录存在
        ensureDailyTasksCreated(userId, today, dailyTaskRepository.findAll());

        for (DailyTask task : tasks) {
            Optional<UserDailyTask> progressOpt = userDailyTaskRepository
                    .findByUserIdAndTaskIdAndTaskDate(userId, task.getId(), today);

            if (progressOpt.isPresent()) {
                UserDailyTask progress = progressOpt.get();
                if (progress.getIsCompleted())
                    continue;

                long newValue = progress.getCurrentValue() + increment;
                // 对于LOGIN和SHARE类型,通常一次完成,但也支持累积逻辑
                progress.setCurrentValue(Math.min(newValue, task.getTargetValue())); // 不超过目标值

                if (newValue >= task.getTargetValue()) {
                    progress.setIsCompleted(true);
                    progress.setCurrentValue(task.getTargetValue());
                    log.info("用户{}完成每日任务: {}", userId, task.getName());
                }

                progress.setUpdatedAt(LocalDateTime.now());
                userDailyTaskRepository.save(progress);
            }
        }
    }

    /**
     * 领取任务奖励
     */
    @Transactional
    public Map<String, Object> claimReward(Long taskId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();
        LocalDate today = LocalDate.now();

        UserDailyTask progress = userDailyTaskRepository
                .findByUserIdAndTaskIdAndTaskDate(userId, taskId, today)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "未找到任务进度"));

        if (!progress.getIsCompleted()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "任务未完成");
        }

        if (progress.getIsRewardClaimed()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "奖励已领取");
        }

        DailyTask task = dailyTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "任务不存在"));

        // 发放奖励
        if ("COINS".equals(task.getRewardType())) {
            paymentService.addCoins(userId, task.getRewardAmount(), "每日任务奖励: " + task.getName());
        } else if ("DIAMONDS".equals(task.getRewardType())) {
            paymentService.addDiamonds(userId, task.getRewardAmount(), "每日任务奖励: " + task.getName());
        }

        progress.setIsRewardClaimed(true);
        progress.setUpdatedAt(LocalDateTime.now());
        userDailyTaskRepository.save(progress);

        Map<String, Object> result = new HashMap<>();
        result.put("rewardType", task.getRewardType());
        result.put("rewardAmount", task.getRewardAmount());

        return result;
    }

    /**
     * 确保用户今日的任务记录已创建
     */
    private void ensureDailyTasksCreated(Long userId, LocalDate date, List<DailyTask> allTasks) {
        List<UserDailyTask> existing = userDailyTaskRepository.findByUserIdAndTaskDate(userId, date);
        if (existing.size() == allTasks.size())
            return;

        Set<Long> existingTaskIds = existing.stream()
                .map(UserDailyTask::getTaskId)
                .collect(Collectors.toSet());

        List<UserDailyTask> newRecords = new ArrayList<>();
        for (DailyTask task : allTasks) {
            if (!existingTaskIds.contains(task.getId())) {
                newRecords.add(new UserDailyTask(userId, task.getId(), date));
            }
        }

        if (!newRecords.isEmpty()) {
            userDailyTaskRepository.saveAll(newRecords);
        }
    }
}

package com.example.top_hog_server.service;

import com.example.top_hog_server.model.GameHistory;
import com.example.top_hog_server.model.Order;
import com.example.top_hog_server.model.User;
import com.example.top_hog_server.repository.GameHistoryRepository;
import com.example.top_hog_server.repository.OrderRepository;
import com.example.top_hog_server.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据分析服务
 */
@Service
@Slf4j
public class AnalyticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 获取核心指标
     */
    public Map<String, Object> getCoreMetrics(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // DAU - 日活跃用户数
        long dau = getDailyActiveUsers(startOfDay, endOfDay);

        // MAU - 月活跃用户数
        LocalDateTime monthStart = date.withDayOfMonth(1).atStartOfDay();
        long mau = getMonthlyActiveUsers(monthStart, endOfDay);

        // 新增用户数
        long newUsers = getNewUsers(startOfDay, endOfDay);

        // 留存率
        Map<String, Double> retention = getRetentionRate(date);

        // ARPU - 人均收入
        BigDecimal arpu = getARPU(startOfDay, endOfDay, dau);

        // 付费率
        double payRate = getPayRate(startOfDay, endOfDay);

        // 付费用户数
        long payingUsers = getPayingUsers(startOfDay, endOfDay);

        // ARPPU - 付费用户平均收入
        BigDecimal arppu = getARPPU(startOfDay, endOfDay, payingUsers);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("date", date);
        metrics.put("dau", dau);
        metrics.put("mau", mau);
        metrics.put("newUsers", newUsers);
        metrics.put("retention", retention);
        metrics.put("arpu", arpu);
        metrics.put("payRate", payRate);
        metrics.put("payingUsers", payingUsers);
        metrics.put("arppu", arppu);

        return metrics;
    }

    /**
     * 获取用户行为分析
     */
    public Map<String, Object> getUserBehavior(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        // 活跃用户趋势
        Map<LocalDate, Long> dailyActiveUsers = new LinkedHashMap<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            long dau = getDailyActiveUsers(current.atStartOfDay(), current.atTime(LocalTime.MAX));
            dailyActiveUsers.put(current, dau);
            current = current.plusDays(1);
        }

        // 新增用户趋势
        Map<LocalDate, Long> dailyNewUsers = new LinkedHashMap<>();
        current = startDate;
        while (!current.isAfter(endDate)) {
            long newUsers = getNewUsers(current.atStartOfDay(), current.atTime(LocalTime.MAX));
            dailyNewUsers.put(current, newUsers);
            current = current.plusDays(1);
        }

        // 用户游戏频率分布
        Map<String, Long> gameFrequency = getUserGameFrequency(startTime, endTime);

        // 用户充值行为
        Map<String, Object> rechargePattern = getUserRechargePattern(startTime, endTime);

        Map<String, Object> behavior = new HashMap<>();
        behavior.put("dailyActiveUsers", dailyActiveUsers);
        behavior.put("dailyNewUsers", dailyNewUsers);
        behavior.put("gameFrequency", gameFrequency);
        behavior.put("rechargePattern", rechargePattern);

        return behavior;
    }

    /**
     * 获取游戏数据统计
     */
    public Map<String, Object> getGameData(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        List<GameHistory> history = gameHistoryRepository.findAll().stream()
                .filter(h -> isInTimeRange(toLocalDateTime(h.getCreatedAt()), startTime, endTime))
                .collect(Collectors.toList());

        // 游戏场次统计
        long totalGames = history.stream()
                .map(GameHistory::getRoomId)
                .distinct()
                .count();

        // 每日游戏场次
        Map<LocalDate, Long> dailyGames = history.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getCreatedAt().toLocalDateTime().toLocalDate(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(GameHistory::getRoomId, Collectors.toSet()),
                                s -> (long) s.size())));

        // 平均游戏时长(简化计算)
        double avgDuration = 10.0; // 假设平均10分钟

        // 游戏类型偏好 (暂不支持游戏类型区分，返回空)
        Map<String, Long> gameTypePreference = new HashMap<>();

        // 胜率分布(简化:按排名统计)
        Map<Integer, Long> rankDistribution = history.stream()
                .collect(Collectors.groupingBy(GameHistory::getRank, Collectors.counting()));

        Map<String, Object> gameData = new HashMap<>();
        gameData.put("totalGames", totalGames);
        gameData.put("dailyGames", dailyGames);
        gameData.put("avgDuration", avgDuration);
        gameData.put("gameTypePreference", gameTypePreference);
        gameData.put("rankDistribution", rankDistribution);

        return gameData;
    }

    /**
     * 获取综合报表
     */
    public Map<String, Object> getComprehensiveReport(LocalDate date) {
        Map<String, Object> report = new HashMap<>();

        // 核心指标
        report.put("coreMetrics", getCoreMetrics(date));

        // 用户行为(最近7天)
        LocalDate weekAgo = date.minusDays(6);
        report.put("userBehavior", getUserBehavior(weekAgo, date));

        // 游戏数据(最近7天)
        report.put("gameData", getGameData(weekAgo, date));

        return report;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 获取日活跃用户数
     */
    private long getDailyActiveUsers(LocalDateTime start, LocalDateTime end) {
        return gameHistoryRepository.findAll().stream()
                .filter(h -> isInTimeRange(toLocalDateTime(h.getCreatedAt()), start, end))
                .map(GameHistory::getUserId)
                .distinct()
                .count();
    }

    /**
     * 获取月活跃用户数
     */
    private long getMonthlyActiveUsers(LocalDateTime start, LocalDateTime end) {
        return gameHistoryRepository.findAll().stream()
                .filter(h -> isInTimeRange(toLocalDateTime(h.getCreatedAt()), start, end))
                .map(GameHistory::getUserId)
                .distinct()
                .count();
    }

    /**
     * 获取新增用户数
     */
    private long getNewUsers(LocalDateTime start, LocalDateTime end) {
        return userRepository.findAll().stream()
                .filter(u -> isInTimeRange(toLocalDateTime(u.getCreatedAt()), start, end))
                .count();
    }

    /**
     * 获取留存率
     */
    private Map<String, Double> getRetentionRate(LocalDate date) {
        // 简化实现:计算前一天注册用户的次日留存
        LocalDate yesterday = date.minusDays(1);
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime yesterdayEnd = yesterday.atTime(LocalTime.MAX);

        // 前一天注册的用户
        List<Long> newUserIds = userRepository.findAll().stream()
                .filter(u -> isInTimeRange(toLocalDateTime(u.getCreatedAt()), yesterdayStart, yesterdayEnd))
                .map(User::getId)
                .collect(Collectors.toList());

        if (newUserIds.isEmpty()) {
            Map<String, Double> retention = new HashMap<>();
            retention.put("day1", 0.0);
            return retention;
        }

        // 这些用户中今天活跃的
        LocalDateTime todayStart = date.atStartOfDay();
        LocalDateTime todayEnd = date.atTime(LocalTime.MAX);

        long activeToday = gameHistoryRepository.findAll().stream()
                .filter(h -> isInTimeRange(toLocalDateTime(h.getCreatedAt()), todayStart, todayEnd))
                .map(GameHistory::getUserId)
                .filter(newUserIds::contains)
                .distinct()
                .count();

        double day1Retention = (double) activeToday / newUserIds.size() * 100;

        Map<String, Double> retention = new HashMap<>();
        retention.put("day1", Math.round(day1Retention * 100.0) / 100.0);

        return retention;
    }

    /**
     * 获取ARPU
     */
    private BigDecimal getARPU(LocalDateTime start, LocalDateTime end, long activeUsers) {
        if (activeUsers == 0)
            return BigDecimal.ZERO;

        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .filter(o -> isInTimeRange(o.getCreatedAt(), start, end))
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalRevenue.divide(BigDecimal.valueOf(activeUsers), 2, RoundingMode.HALF_UP);
    }

    /**
     * 获取付费率
     */
    private double getPayRate(LocalDateTime start, LocalDateTime end) {
        long activeUsers = getDailyActiveUsers(start, end);
        if (activeUsers == 0)
            return 0.0;

        long payingUsers = getPayingUsers(start, end);

        return Math.round((double) payingUsers / activeUsers * 10000.0) / 100.0;
    }

    /**
     * 获取付费用户数
     */
    private long getPayingUsers(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findAll().stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .filter(o -> isInTimeRange(o.getCreatedAt(), start, end))
                .map(Order::getUserId)
                .distinct()
                .count();
    }

    /**
     * 获取ARPPU
     */
    private BigDecimal getARPPU(LocalDateTime start, LocalDateTime end, long payingUsers) {
        if (payingUsers == 0)
            return BigDecimal.ZERO;

        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .filter(o -> isInTimeRange(o.getCreatedAt(), start, end))
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalRevenue.divide(BigDecimal.valueOf(payingUsers), 2, RoundingMode.HALF_UP);
    }

    /**
     * 获取用户游戏频率分布
     */
    private Map<String, Long> getUserGameFrequency(LocalDateTime start, LocalDateTime end) {
        Map<Long, Long> userGameCount = gameHistoryRepository.findAll().stream()
                .filter(h -> isInTimeRange(toLocalDateTime(h.getCreatedAt()), start, end))
                .collect(Collectors.groupingBy(GameHistory::getUserId, Collectors.counting()));

        Map<String, Long> frequency = new HashMap<>();
        frequency.put("1-5局", userGameCount.values().stream().filter(c -> c >= 1 && c <= 5).count());
        frequency.put("6-10局", userGameCount.values().stream().filter(c -> c >= 6 && c <= 10).count());
        frequency.put("11-20局", userGameCount.values().stream().filter(c -> c >= 11 && c <= 20).count());
        frequency.put("20局以上", userGameCount.values().stream().filter(c -> c > 20).count());

        return frequency;
    }

    /**
     * 获取用户充值行为
     */
    private Map<String, Object> getUserRechargePattern(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .filter(o -> isInTimeRange(o.getCreatedAt(), start, end))
                .collect(Collectors.toList());

        // 首充用户数
        long firstTimeRecharge = orders.stream()
                .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()))
                .values().stream()
                .filter(count -> count == 1)
                .count();

        // 复购用户数
        long repeatRecharge = orders.stream()
                .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()))
                .values().stream()
                .filter(count -> count > 1)
                .count();

        // 平均充值金额
        BigDecimal avgAmount = orders.isEmpty() ? BigDecimal.ZERO
                : orders.stream()
                        .map(Order::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);

        Map<String, Object> pattern = new HashMap<>();
        pattern.put("firstTimeRecharge", firstTimeRecharge);
        pattern.put("repeatRecharge", repeatRecharge);
        pattern.put("avgAmount", avgAmount);

        return pattern;
    }

    /**
     * 判断时间是否在范围内
     */
    private boolean isInTimeRange(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        if (time == null)
            return false;
        return !time.isBefore(start) && !time.isAfter(end);
    }

    /**
     * Timestamp转LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}

package com.example.top_hog_server.service;

import com.example.top_hog_server.model.GameHistory;
import com.example.top_hog_server.model.User;
import com.example.top_hog_server.repository.GameHistoryRepository;
import com.example.top_hog_server.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排行榜服务
 */
@Service
@Slf4j
public class LeaderboardService {

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取全服排行榜
     * 
     * @param type   TOTAL_SCORE(总分), WIN_RATE(胜率), TOTAL_WINS(胜场)
     * @param period ALL(全部), WEEKLY(本周), MONTHLY(本月)
     * @param limit  数量
     */
    public List<Map<String, Object>> getLeaderboard(String type, String period, int limit) {
        // 计算时间范围
        LocalDateTime startTime = null;
        if ("WEEKLY".equalsIgnoreCase(period)) {
            startTime = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1).atStartOfDay();
        } else if ("MONTHLY".equalsIgnoreCase(period)) {
            startTime = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        }

        // 获取所有相关历史记录
        List<GameHistory> history;
        if (startTime != null) {
            // 这里简单处理,实际量大应在DB层面聚合
            LocalDateTime finalStartTime = startTime;
            history = gameHistoryRepository.findAll().stream()
                    .filter(h -> h.getCreatedAt().toLocalDateTime().isAfter(finalStartTime))
                    .collect(Collectors.toList());
        } else {
            history = gameHistoryRepository.findAll();
        }

        // 分组统计
        Map<Long, Map<String, Object>> userStats = new HashMap<>();
        for (GameHistory h : history) {
            userStats.computeIfAbsent(h.getUserId(), k -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("userId", k);
                stats.put("totalScore", 0);
                stats.put("games", 0);
                stats.put("wins", 0);
                return stats;
            });

            Map<String, Object> stats = userStats.get(h.getUserId());
            stats.put("totalScore", (Integer) stats.get("totalScore") + h.getScore());
            stats.put("games", (Integer) stats.get("games") + 1);
            if (h.getRank() == 1) {
                stats.put("wins", (Integer) stats.get("wins") + 1);
            }
        }

        // 计算胜率和排序列表
        List<Map<String, Object>> ranking = new ArrayList<>(userStats.values());
        for (Map<String, Object> stats : ranking) {
            int games = (Integer) stats.get("games");
            int wins = (Integer) stats.get("wins");
            double winRate = games > 0 ? (double) wins / games * 100 : 0;
            stats.put("winRate", winRate);
        }

        // 排序
        Comparator<Map<String, Object>> comparator;
        if ("WIN_RATE".equalsIgnoreCase(type)) {
            comparator = (a, b) -> {
                // 胜率相同比场次、再比积分
                int c = Double.compare((Double) b.get("winRate"), (Double) a.get("winRate"));
                if (c != 0)
                    return c;
                return Integer.compare((Integer) b.get("games"), (Integer) a.get("games"));
            };
            // 过滤掉场次太少的
            ranking = ranking.stream().filter(s -> (Integer) s.get("games") >= 5).collect(Collectors.toList());
        } else if ("TOTAL_WINS".equalsIgnoreCase(type)) {
            comparator = (a, b) -> Integer.compare((Integer) b.get("wins"), (Integer) a.get("wins"));
        } else { // TOTAL_SCORE
            comparator = (a, b) -> Integer.compare((Integer) b.get("totalScore"), (Integer) a.get("totalScore"));
        }

        ranking.sort(comparator);

        // 截取前N名并补充用户信息
        List<Map<String, Object>> result = ranking.stream()
                .limit(limit)
                .collect(Collectors.toList());

        // 批量查询用户以优化性能
        Set<Long> userIds = result.stream().map(m -> (Long) m.get("userId")).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        for (int i = 0; i < result.size(); i++) {
            Map<String, Object> item = result.get(i);
            item.put("rank", i + 1);
            User user = userMap.get(item.get("userId"));
            if (user != null) {
                item.put("nickname", user.getNickname());
                item.put("avatarUrl", user.getAvatarUrl());
                item.put("isVip", user.isVip());
            }
        }

        return result;
    }
}

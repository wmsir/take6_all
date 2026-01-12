package com.example.top_hog_server.service;

import com.example.top_hog_server.model.Friendship;
import com.example.top_hog_server.model.GameHistory;
import com.example.top_hog_server.model.User;
import com.example.top_hog_server.repository.FriendshipRepository;
import com.example.top_hog_server.repository.GameHistoryRepository;
import com.example.top_hog_server.repository.UserRepository;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 社交扩展服务 - 好友排行榜等
 */
@Service
@Slf4j
public class SocialService {

        @Autowired
        private FriendshipRepository friendshipRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private GameHistoryRepository gameHistoryRepository;

        /**
         * 获取好友排行榜
         */
        public Map<String, Object> getFriendRanking(String rankType) {
                UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                                .getAuthentication().getPrincipal();
                Long userId = currentUser.getId();

                // 获取好友列表
                List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, "ACCEPTED");
                List<Long> friendIds = friendships.stream()
                                .map(Friendship::getFriendId)
                                .collect(Collectors.toList());

                // 添加自己
                friendIds.add(userId);

                // 获取好友的游戏记录
                List<GameHistory> allHistory = gameHistoryRepository.findAll();

                // 按好友分组统计
                Map<Long, Map<String, Object>> friendStats = new HashMap<>();

                for (Long friendId : friendIds) {
                        List<GameHistory> friendHistory = allHistory.stream()
                                        .filter(h -> h.getUserId().equals(friendId))
                                        .collect(Collectors.toList());

                        Map<String, Object> stats = new HashMap<>();
                        stats.put("userId", friendId);
                        stats.put("totalGames", friendHistory.size());
                        stats.put("totalScore", friendHistory.stream().mapToInt(GameHistory::getScore).sum());
                        stats.put("avgScore", friendHistory.isEmpty() ? 0
                                        : friendHistory.stream().mapToInt(GameHistory::getScore).average().orElse(0));
                        stats.put("wins", friendHistory.stream().filter(h -> h.getRank() == 1).count());
                        stats.put("winRate",
                                        friendHistory.isEmpty() ? 0
                                                        : (double) friendHistory.stream().filter(h -> h.getRank() == 1)
                                                                        .count()
                                                                        / friendHistory.size() * 100);

                        friendStats.put(friendId, stats);
                }

                // 排序
                List<Map<String, Object>> ranking = new ArrayList<>(friendStats.values());

                switch (rankType != null ? rankType : "totalScore") {
                        case "totalGames":
                                ranking.sort((a, b) -> Integer.compare(
                                                (Integer) b.get("totalGames"),
                                                (Integer) a.get("totalGames")));
                                break;
                        case "winRate":
                                ranking.sort((a, b) -> Double.compare(
                                                (Double) b.get("winRate"),
                                                (Double) a.get("winRate")));
                                break;
                        case "avgScore":
                                ranking.sort((a, b) -> Double.compare(
                                                (Double) b.get("avgScore"),
                                                (Double) a.get("avgScore")));
                                break;
                        default: // totalScore
                                ranking.sort((a, b) -> Integer.compare(
                                                (Integer) b.get("totalScore"),
                                                (Integer) a.get("totalScore")));
                }

                // 添加排名和用户信息
                for (int i = 0; i < ranking.size(); i++) {
                        Map<String, Object> item = ranking.get(i);
                        item.put("rank", i + 1);

                        Long friendId = (Long) item.get("userId");
                        User user = userRepository.findById(friendId).orElse(null);
                        if (user != null) {
                                item.put("nickname", user.getNickname());
                                item.put("avatarUrl", user.getAvatarUrl());
                                item.put("isVip", user.isVip());
                                item.put("isMe", friendId.equals(userId));
                        }
                }

                Map<String, Object> result = new HashMap<>();
                result.put("ranking", ranking);
                result.put("total", ranking.size());
                result.put("rankType", rankType != null ? rankType : "totalScore");

                return result;
        }

        /**
         * 获取好友游戏记录对比
         */
        public Map<String, Object> compareFriendStats(Long friendId) {
                UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                                .getAuthentication().getPrincipal();
                Long userId = currentUser.getId();

                // 验证是否是好友
                boolean isFriend = friendshipRepository.existsByUserIdAndFriendIdAndStatus(userId, friendId,
                                "ACCEPTED");
                if (!isFriend && !userId.equals(friendId)) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("error", "只能查看好友的数据");
                        return result;
                }

                // 获取双方游戏记录
                List<GameHistory> myHistory = gameHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, null);
                List<GameHistory> friendHistory = gameHistoryRepository.findByUserIdOrderByCreatedAtDesc(friendId,
                                null);

                Map<String, Object> myStats = calculateStats(myHistory);
                Map<String, Object> friendStats = calculateStats(friendHistory);

                // 获取用户信息
                User friend = userRepository.findById(friendId).orElse(null);

                Map<String, Object> result = new HashMap<>();
                result.put("myStats", myStats);
                result.put("friendStats", friendStats);
                if (friend != null) {
                        Map<String, Object> friendInfo = new HashMap<>();
                        friendInfo.put("id", friend.getId());
                        friendInfo.put("nickname", friend.getNickname());
                        friendInfo.put("avatarUrl", friend.getAvatarUrl());
                        result.put("friendInfo", friendInfo);
                }

                return result;
        }

        /**
         * 搜索用户(用于添加好友)
         */
        public List<Map<String, Object>> searchUsers(String keyword) {
                List<User> users = userRepository.findAll().stream()
                                .filter(user -> (user.getNickname() != null && user.getNickname().contains(keyword)) ||
                                                (user.getUsername() != null && user.getUsername().contains(keyword)) ||
                                                user.getId().toString().equals(keyword))
                                .limit(20)
                                .collect(Collectors.toList());

                return users.stream()
                                .map(user -> {
                                        Map<String, Object> info = new HashMap<>();
                                        info.put("id", user.getId());
                                        info.put("nickname", user.getNickname());
                                        info.put("avatarUrl", user.getAvatarUrl());
                                        info.put("isVip", user.isVip());
                                        return info;
                                })
                                .collect(Collectors.toList());
        }

        /**
         * 计算统计数据
         */
        private Map<String, Object> calculateStats(List<GameHistory> history) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalGames", history.size());
                stats.put("totalScore", history.stream().mapToInt(GameHistory::getScore).sum());
                stats.put("avgScore",
                                history.isEmpty() ? 0
                                                : history.stream().mapToInt(GameHistory::getScore).average().orElse(0));
                stats.put("wins", history.stream().filter(h -> h.getRank() == 1).count());
                stats.put("winRate", history.isEmpty() ? 0
                                : (double) history.stream().filter(h -> h.getRank() == 1).count() / history.size()
                                                * 100);
                stats.put("bestScore", history.stream().mapToInt(GameHistory::getScore).max().orElse(0));
                stats.put("worstScore", history.stream().mapToInt(GameHistory::getScore).min().orElse(0));

                return stats;
        }
}

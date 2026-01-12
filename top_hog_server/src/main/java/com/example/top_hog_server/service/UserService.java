package com.example.top_hog_server.service;

import com.example.top_hog_server.exception.BusinessException;
import com.example.top_hog_server.exception.ErrorCode;
import com.example.top_hog_server.model.GameHistory;
import com.example.top_hog_server.model.User;
import com.example.top_hog_server.payload.dto.request.UserUpdateRequest;
import com.example.top_hog_server.repository.GameHistoryRepository;
import com.example.top_hog_server.repository.UserRepository;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    @Autowired
    private OssService ossService;

    @Autowired
    private ContentSecurityService contentSecurityService;

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }

    public Map<String, Object> getUserInfo() {
        User user = getCurrentUser();
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("nickname", user.getNickname());
        info.put("avatarUrl", user.getAvatarUrl());
        info.put("phone", user.getPhone());
        info.put("registerTime", user.getRegisterTime());
        // stats
        long totalGames = gameHistoryRepository.countByUserId(user.getId());
        info.put("totalScore", totalGames); // Requirements say totalScore, but implies stats. Let's put total games for
                                            // now or calculate actual score sum.
        return info;
    }

    public Map<String, Object> getUserStats() {
        User user = getCurrentUser();
        Long userId = user.getId();

        long totalGames = gameHistoryRepository.countByUserId(userId);
        Double avgScore = gameHistoryRepository.findAvgScoreByUserId(userId);
        Integer maxBullScore = gameHistoryRepository.findMaxScoreByUserId(userId); // Max bulls is bad
        Integer minBullScore = gameHistoryRepository.findMinScoreByUserId(userId); // Min bulls is good

        // Mock win rate and streak for now as it requires complex query on history
        int winRate = 50;
        int maxStreak = 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGames", totalGames);
        stats.put("winRate", winRate);
        stats.put("avgScore", avgScore != null ? avgScore : 0);
        stats.put("maxStreak", maxStreak);
        stats.put("maxBullScore", maxBullScore != null ? maxBullScore : 0);
        stats.put("minBullScore", minBullScore != null ? minBullScore : 0);
        stats.put("commonOpponents", "Mock Opponent");

        return stats;
    }

    public Map<String, Object> getUserHistory(int limit) {
        User user = getCurrentUser();
        List<GameHistory> historyList = gameHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId(),
                PageRequest.of(0, limit));

        List<Map<String, Object>> mappedList = historyList.stream().map(h -> {
            Map<String, Object> item = new HashMap<>();
            item.put("score", h.getScore());
            item.put("avgScore", h.getRoomAvgScore());
            item.put("roomId", h.getRoomId());
            item.put("createdAt", h.getCreatedAt());
            item.put("rank", h.getRank());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", mappedList);
        result.put("total", gameHistoryRepository.countByUserId(user.getId()));
        return result;
    }

    public Map<String, Object> updateUser(UserUpdateRequest request) {
        User user = getCurrentUser();

        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            String nickname = request.getNickname().trim();
            // 内容安全检测
            if (!contentSecurityService.checkText(nickname, user.getOpenid())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "昵称包含违规内容,请修改后重试");
            }
            user.setNickname(nickname);
        }

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            user.setEmail(request.getEmail().trim());
        }

        userRepository.save(user);

        return getUserInfo();
    }

    public Map<String, Object> uploadAvatar(MultipartFile file) {
        User user = getCurrentUser();
        String avatarUrl = ossService.uploadFile(file);

        // 图片内容安全检测
        if (!contentSecurityService.checkImage(avatarUrl, user.getOpenid())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "头像包含违规内容,请更换后重试");
        }

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return getUserInfo();
    }
}

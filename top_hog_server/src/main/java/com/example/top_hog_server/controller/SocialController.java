package com.example.top_hog_server.controller;

import com.example.top_hog_server.service.SocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 社交功能控制器
 */
@RestController
@RequestMapping("/api/social")
@Tag(name = "社交功能", description = "好友排行榜等社交功能")
@Slf4j
public class SocialController {

    @Autowired
    private SocialService socialService;

    /**
     * 获取好友排行榜
     */
    @GetMapping("/friend-ranking")
    @Operation(summary = "获取好友排行榜")
    public ResponseEntity<?> getFriendRanking(
            @RequestParam(required = false, defaultValue = "totalScore") String rankType) {

        try {
            Map<String, Object> result = socialService.getFriendRanking(rankType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取好友排行榜失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 对比好友数据
     */
    @GetMapping("/compare/{friendId}")
    @Operation(summary = "对比好友游戏数据")
    public ResponseEntity<?> compareFriendStats(@PathVariable Long friendId) {
        try {
            Map<String, Object> result = socialService.compareFriendStats(friendId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("对比好友数据失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search-users")
    @Operation(summary = "搜索用户(用于添加好友)")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        try {
            List<Map<String, Object>> users = socialService.searchUsers(keyword);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", users);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

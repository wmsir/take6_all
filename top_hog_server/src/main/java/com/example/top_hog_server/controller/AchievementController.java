package com.example.top_hog_server.controller;

import com.example.top_hog_server.service.AchievementService;
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
 * 成就系统控制器
 */
@RestController
@RequestMapping("/api/achievements")
@Tag(name = "成就系统", description = "成就与奖励相关接口")
@Slf4j
public class AchievementController {

    @Autowired
    private AchievementService achievementService;

    /**
     * 获取我的成就列表
     */
    @GetMapping
    @Operation(summary = "获取我的成就列表")
    public ResponseEntity<?> getMyAchievements() {
        try {
            List<Map<String, Object>> achievements = achievementService.getUserAchievements();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", achievements);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取成就列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 领取成就奖励
     */
    @PostMapping("/{achievementId}/claim")
    @Operation(summary = "领取成就奖励")
    public ResponseEntity<?> claimReward(@PathVariable Long achievementId) {
        try {
            Map<String, Object> reward = achievementService.claimReward(achievementId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "领取成功");
            response.put("data", reward);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("领取奖励失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 初始化默认成就(仅管理员或测试用)
     */
    @PostMapping("/init-defaults")
    @Operation(summary = "初始化默认成就")
    public ResponseEntity<?> initDefaults() {
        try {
            achievementService.initDefaultAchievements();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "默认成就初始化完成");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

package com.example.top_hog_server.controller;

import com.example.top_hog_server.service.DailyTaskService;
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
 * 每日任务控制器
 */
@RestController
@RequestMapping("/api/daily-tasks")
@Tag(name = "每日任务", description = "每日任务接口")
@Slf4j
public class DailyTaskController {

    @Autowired
    private DailyTaskService dailyTaskService;

    @GetMapping
    @Operation(summary = "获取今日任务列表")
    public ResponseEntity<?> getTodayTasks() {
        try {
            List<Map<String, Object>> tasks = dailyTaskService.getTodayTasks();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tasks);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取每日任务失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{taskId}/claim")
    @Operation(summary = "领取任务奖励")
    public ResponseEntity<?> claimReward(@PathVariable Long taskId) {
        try {
            Map<String, Object> reward = dailyTaskService.claimReward(taskId);

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

    @PostMapping("/init-defaults")
    @Operation(summary = "初始化默认每日任务")
    public ResponseEntity<?> initDefaults() {
        try {
            dailyTaskService.initDefaultTasks();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "初始化完成");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

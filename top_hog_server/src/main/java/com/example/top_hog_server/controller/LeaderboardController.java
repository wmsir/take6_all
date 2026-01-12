package com.example.top_hog_server.controller;

import com.example.top_hog_server.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboards")
@Tag(name = "排行榜", description = "全服排行榜接口")
@Slf4j
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping
    @Operation(summary = "获取排行榜")
    public ResponseEntity<?> getLeaderboard(
            @RequestParam(defaultValue = "TOTAL_SCORE") String type,
            @RequestParam(defaultValue = "ALL") String period,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            List<Map<String, Object>> ranking = leaderboardService.getLeaderboard(type, period, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ranking);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取排行榜失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

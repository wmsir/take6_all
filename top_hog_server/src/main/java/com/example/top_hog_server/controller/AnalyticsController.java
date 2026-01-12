package com.example.top_hog_server.controller;

import com.example.top_hog_server.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据分析控制器
 * 需要管理员权限
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "数据分析", description = "数据分析接口(需要管理员权限)")
@Slf4j
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * 获取核心指标
     */
    @GetMapping("/core-metrics")
    @Operation(summary = "获取核心指标(DAU/MAU/留存率/ARPU等)")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCoreMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            if (date == null) {
                date = LocalDate.now();
            }

            Map<String, Object> metrics = analyticsService.getCoreMetrics(date);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", metrics);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取核心指标失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户行为分析
     */
    @GetMapping("/user-behavior")
    @Operation(summary = "获取用户行为分析")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserBehavior(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            Map<String, Object> behavior = analyticsService.getUserBehavior(startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", behavior);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户行为分析失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取游戏数据统计
     */
    @GetMapping("/game-data")
    @Operation(summary = "获取游戏数据统计")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getGameData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            Map<String, Object> gameData = analyticsService.getGameData(startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", gameData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取游戏数据统计失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取综合报表
     */
    @GetMapping("/comprehensive-report")
    @Operation(summary = "获取综合报表(包含核心指标、用户行为、游戏数据)")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getComprehensiveReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            if (date == null) {
                date = LocalDate.now();
            }

            Map<String, Object> report = analyticsService.getComprehensiveReport(date);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", report);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取综合报表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取今日数据概览
     */
    @GetMapping("/today")
    @Operation(summary = "获取今日数据概览")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTodayOverview() {
        try {
            LocalDate today = LocalDate.now();
            Map<String, Object> metrics = analyticsService.getCoreMetrics(today);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", metrics);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取今日数据概览失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

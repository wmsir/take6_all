package com.example.top_hog_server.controller;

import com.example.top_hog_server.service.AdminGameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GM管理控制器 - 游戏管理
 * 需要管理员权限
 */
@RestController
@RequestMapping("/api/admin/games")
@Tag(name = "GM游戏管理", description = "GM后台游戏管理接口(需要管理员权限)")
@Slf4j
public class AdminGameController {

    @Autowired
    private AdminGameService adminGameService;

    /**
     * 获取所有活跃房间
     */
    @GetMapping("/rooms")
    @Operation(summary = "获取所有活跃房间")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getActiveRooms() {
        try {
            Map<String, Object> result = adminGameService.getActiveRooms();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取活跃房间失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取房间详情
     */
    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "获取房间详情")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRoomDetail(@PathVariable String roomId) {
        try {
            Map<String, Object> detail = adminGameService.getRoomDetail(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", detail);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取房间详情失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 强制解散房间
     */
    @DeleteMapping("/rooms/{roomId}")
    @Operation(summary = "强制解散房间")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> dismissRoom(
            @PathVariable String roomId,
            @RequestBody(required = false) Map<String, String> request) {

        try {
            String reason = request != null ? request.getOrDefault("reason", "GM操作") : "GM操作";
            adminGameService.dismissRoom(roomId, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "房间已解散");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("解散房间失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取游戏对局记录
     */
    @GetMapping("/history")
    @Operation(summary = "获取游戏对局记录")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getGameHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String roomId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        try {
            Map<String, Object> result = adminGameService.getGameHistory(
                    userId, roomId, page, pageSize);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取游戏记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取游戏统计数据
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取游戏统计数据")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getGameStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        try {
            Map<String, Object> stats = adminGameService.getGameStatistics(startTime, endTime);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取游戏统计失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

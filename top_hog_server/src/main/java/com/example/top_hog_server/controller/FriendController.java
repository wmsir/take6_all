package com.example.top_hog_server.controller;

import com.example.top_hog_server.service.FriendService;
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
 * 好友控制器
 */
@RestController
@RequestMapping("/api/friends")
@Tag(name = "好友管理", description = "好友相关接口")
@Slf4j
public class FriendController {

    @Autowired
    private FriendService friendService;

    /**
     * 添加好友
     */
    @PostMapping("/add")
    @Operation(summary = "添加好友")
    public ResponseEntity<?> addFriend(@RequestBody Map<String, Long> request) {
        try {
            Long friendId = request.get("friendId");
            friendService.addFriend(friendId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "好友请求已发送");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("添加好友失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 接受好友请求
     */
    @PostMapping("/accept/{requestId}")
    @Operation(summary = "接受好友请求")
    public ResponseEntity<?> acceptFriend(@PathVariable Long requestId) {
        try {
            friendService.acceptFriend(requestId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已接受好友请求");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("接受好友请求失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 拒绝好友请求
     */
    @PostMapping("/reject/{requestId}")
    @Operation(summary = "拒绝好友请求")
    public ResponseEntity<?> rejectFriend(@PathVariable Long requestId) {
        try {
            friendService.rejectFriend(requestId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已拒绝好友请求");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("拒绝好友请求失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    @Operation(summary = "删除好友")
    public ResponseEntity<?> deleteFriend(@PathVariable Long friendId) {
        try {
            friendService.deleteFriend(friendId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已删除好友");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("删除好友失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取好友列表
     */
    @GetMapping
    @Operation(summary = "获取好友列表")
    public ResponseEntity<?> getFriendList() {
        try {
            List<Map<String, Object>> friends = friendService.getFriendList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", friends);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取好友列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取好友请求列表
     */
    @GetMapping("/requests")
    @Operation(summary = "获取好友请求列表")
    public ResponseEntity<?> getFriendRequests() {
        try {
            List<Map<String, Object>> requests = friendService.getFriendRequests();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", requests);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取好友请求失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 设置好友备注
     */
    @PutMapping("/{friendId}/remark")
    @Operation(summary = "设置好友备注")
    public ResponseEntity<?> setFriendRemark(
            @PathVariable Long friendId,
            @RequestBody Map<String, String> request) {

        try {
            String remark = request.get("remark");
            friendService.setFriendRemark(friendId, remark);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "备注设置成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("设置好友备注失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

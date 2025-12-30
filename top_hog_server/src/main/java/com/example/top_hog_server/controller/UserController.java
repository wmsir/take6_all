package com.example.top_hog_server.controller;

import com.example.top_hog_server.payload.dto.request.UserUpdateRequest;
import com.example.top_hog_server.payload.dto.response.ApiResponse;
import com.example.top_hog_server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/update")
    public ApiResponse<Map<String, Object>> updateUser(@RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.updateUser(request));
    }

    @PostMapping("/avatar")
    public ApiResponse<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(userService.uploadAvatar(file));
    }

    @PostMapping("/info")
    public ApiResponse<Map<String, Object>> getUserInfo() {
        return ApiResponse.success(userService.getUserInfo());
    }

    @PostMapping("/stats")
    public ApiResponse<Map<String, Object>> getUserStats(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(userService.getUserStats());
    }

    @PostMapping("/history")
    public ApiResponse<Map<String, Object>> getUserHistory(@RequestBody Map<String, Integer> body) {
        int limit = body.getOrDefault("limit", 10);
        return ApiResponse.success(userService.getUserHistory(limit));
    }
}

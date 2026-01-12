package com.example.top_hog_server.controller;

import com.example.top_hog_server.payload.dto.response.ApiResponse;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import com.example.top_hog_server.service.CommercialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商业化控制器
 * 处理广告变现和新手引导相关请求
 */
@RestController
@RequestMapping("/api/commercial")
public class CommercialController {

    @Autowired
    private CommercialService commercialService;

    /**
     * 观看广告完成，领取奖励
     */
    @PostMapping("/ad/reward")
    public ApiResponse<?> rewardAd(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Map<String, Object> result = commercialService.rewardAd(userDetails.getId());
        return ApiResponse.success(result);
    }

    /**
     * 完成新手引导
     */
    @PostMapping("/guide/finish")
    public ApiResponse<?> finishGuide(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Map<String, Object> result = commercialService.finishGuide(userDetails.getId());
        return ApiResponse.success(result);
    }
}

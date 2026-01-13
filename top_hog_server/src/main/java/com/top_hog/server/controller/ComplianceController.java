package com.top_hog.server.controller;

import com.top_hog.server.entity.RealNameInfo;
import com.top_hog.server.repository.RealNameInfoRepository;
import com.top_hog.server.service.ComplianceService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    @Autowired
    private ComplianceService complianceService;

    @Autowired
    private RealNameInfoRepository realNameInfoRepository;

    @PostMapping("/realname")
    public ResponseEntity<?> submitRealName(@RequestBody RealNameRequest request) {
        try {
            boolean success = complianceService.submitRealNameAuth(request.getUserId(), request.getRealName(),
                    request.getIdCard());
            return ResponseEntity.ok(success);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam Long userId) {
        RealNameInfo info = realNameInfoRepository.findByUserId(userId).orElse(null);
        if (info == null) {
            return ResponseEntity.ok(new ComplianceStatusResponse(false, false));
        }
        return ResponseEntity.ok(new ComplianceStatusResponse(true, info.getIsAdult()));
    }

    @Data
    public static class RealNameRequest {
        private Long userId;
        private String realName;
        private String idCard;
    }

    @Data
    public static class ComplianceStatusResponse {
        private boolean isVerified;
        private boolean isAdult;

        public ComplianceStatusResponse(boolean isVerified, boolean isAdult) {
            this.isVerified = isVerified;
            this.isAdult = isAdult;
        }
    }
}

package com.example.top_hog_server.payload.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WechatLoginResponse {
    private String id;
    private String nickname;
    private String avatarUrl;
    private String token;
    private String phone;
    private Long registerTime;
}

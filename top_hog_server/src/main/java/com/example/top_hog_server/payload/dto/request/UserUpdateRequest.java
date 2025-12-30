package com.example.top_hog_server.payload.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String email;
}

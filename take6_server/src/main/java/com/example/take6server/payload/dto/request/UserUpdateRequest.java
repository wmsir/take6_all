package com.example.take6server.payload.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String email;
}

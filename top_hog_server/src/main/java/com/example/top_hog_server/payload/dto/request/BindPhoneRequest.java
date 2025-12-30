package com.example.top_hog_server.payload.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindPhoneRequest {
    @NotBlank
    private String phone;

    @NotBlank
    private String code;
}

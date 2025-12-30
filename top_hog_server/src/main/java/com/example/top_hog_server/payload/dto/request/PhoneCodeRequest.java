package com.example.top_hog_server.payload.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneCodeRequest {
    @NotBlank
    private String phone;
}

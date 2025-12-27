package com.example.take6server.payload.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindPhoneRequest {
    @NotBlank
    private String phone;

    @NotBlank
    private String code;
}

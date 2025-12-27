package com.example.take6server.payload.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneCodeRequest {
    @NotBlank
    private String phone;
}

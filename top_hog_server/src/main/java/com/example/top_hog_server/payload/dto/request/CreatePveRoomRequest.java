package com.example.top_hog_server.payload.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePveRoomRequest {
    @NotNull(message = "Bot count is required")
    private Integer botCount;
}

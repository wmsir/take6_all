package com.example.take6server.payload.dto.response;

import com.example.take6server.model.Player;
import com.example.take6server.model.Card;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PlayerDTO {
    private String sessionId;
    private Long userId; // Added userId to DTO as per requirements (id/userId)
    private String nickname;
    private String displayName;
    private String avatarUrl;
    private int score;
    private boolean hasPlayed;
    private boolean isCurrentTurn;

    @JsonProperty("isHost")
    private boolean isHost;

    @JsonProperty("isReady")
    private boolean isReady;

    @JsonProperty("isRobot")
    private boolean isRobot;

    private List<Card> hand; // Only present if self

    public static PlayerDTO from(Player player, boolean isSelf) {
        PlayerDTO dto = new PlayerDTO();
        dto.setSessionId(player.getSessionId());
        dto.setUserId(player.getUserId());
        dto.setNickname(player.getDisplayName()); // Assuming displayName is nickname
        dto.setDisplayName(player.getDisplayName());
        dto.setAvatarUrl(player.getAvatarUrl());
        dto.setScore(player.getScore());
        dto.setHost(player.isHost());
        dto.setReady(player.isReady());
        dto.setRobot(player.isRobot());

        // dto.setHasPlayed(player.isHasPlayed()); // Player model doesn't have this field yet?
        // dto.setIsCurrentTurn(player.isCurrentTurn());

        if (isSelf) {
            dto.setHand(player.getHand());
        }
        return dto;
    }
}

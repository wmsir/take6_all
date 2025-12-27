package com.example.take6server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@Entity
@Table(name = "game_history")
public class GameHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "room_id")
    private String roomId;

    private int score; // Bullheads

    @Column(name = "`rank`")
    private int rank;

    @Column(name = "room_avg_score")
    private Double roomAvgScore;

    @Column(name = "created_at", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    public GameHistory(Long userId, String roomId, int score, int rank, Double roomAvgScore) {
        this.userId = userId;
        this.roomId = roomId;
        this.score = score;
        this.rank = rank;
        this.roomAvgScore = roomAvgScore;
    }
}

package com.example.take6server.repository;

import com.example.take6server.model.GameHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
    List<GameHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COUNT(g) FROM GameHistory g WHERE g.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(g.score) FROM GameHistory g WHERE g.userId = :userId")
    Double findAvgScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT MAX(g.score) FROM GameHistory g WHERE g.userId = :userId")
    Integer findMaxScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT MIN(g.score) FROM GameHistory g WHERE g.userId = :userId")
    Integer findMinScoreByUserId(@Param("userId") Long userId);

    // Win rate calculation might need logic based on rank or comparing to avg score.
    // For now, let's just fetch history.
}

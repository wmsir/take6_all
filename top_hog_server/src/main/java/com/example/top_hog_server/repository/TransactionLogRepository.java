package com.example.top_hog_server.repository;

import com.example.top_hog_server.model.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    List<TransactionLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<TransactionLog> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);
}

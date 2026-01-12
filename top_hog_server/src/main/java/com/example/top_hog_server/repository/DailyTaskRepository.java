package com.example.top_hog_server.repository;

import com.example.top_hog_server.model.DailyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyTaskRepository extends JpaRepository<DailyTask, Long> {

    List<DailyTask> findAllByOrderByDisplayOrderAsc();

    List<DailyTask> findByType(String type);
}

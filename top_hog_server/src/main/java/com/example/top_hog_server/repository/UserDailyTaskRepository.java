package com.example.top_hog_server.repository;

import com.example.top_hog_server.model.UserDailyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDailyTaskRepository extends JpaRepository<UserDailyTask, Long> {

    Optional<UserDailyTask> findByUserIdAndTaskIdAndTaskDate(Long userId, Long taskId, LocalDate taskDate);

    List<UserDailyTask> findByUserIdAndTaskDate(Long userId, LocalDate taskDate);

    /**
     * 删除旧的任务进度记录(清理用)
     */
    void deleteByTaskDateBefore(LocalDate date);
}

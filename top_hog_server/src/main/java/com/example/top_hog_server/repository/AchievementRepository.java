package com.example.top_hog_server.repository;

import com.example.top_hog_server.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    /**
     * 获取所有启用的成就，按排序权重排序
     */
    List<Achievement> findAllByOrderByDisplayOrderAsc();

    /**
     * 根据类型查找成就
     */
    List<Achievement> findByType(String type);
}

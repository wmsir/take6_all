package com.top_hog.server.repository;

import com.top_hog.server.entity.RealNameInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RealNameInfoRepository extends JpaRepository<RealNameInfo, Long> {
    Optional<RealNameInfo> findByUserId(Long userId);

    boolean existsByIdCard(String idCard);
}

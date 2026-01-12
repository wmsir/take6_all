package com.example.top_hog_server.repository;

import com.example.top_hog_server.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatusOrderBySortOrderAsc(String status);

    List<Product> findByTypeAndStatusOrderBySortOrderAsc(String type, String status);
}

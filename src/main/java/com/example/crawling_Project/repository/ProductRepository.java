package com.example.crawling_Project.repository;

import com.example.crawling_Project.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByExternalId(String externalId);
}

package com.example.crawling_Project.service;

import com.example.crawling_Project.model.Product;
import com.example.crawling_Project.model.RetailShop;
import com.example.crawling_Project.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void addRetailShopToProduct(Long productId, RetailShop retailShop) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.addRetailShop(retailShop);
        productRepository.save(product);
    }
}


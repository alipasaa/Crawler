package com.example.crawling_Project.service;

import com.example.crawling_Project.repository.RetailShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class RetailShopService {

    private final RetailShopRepository retailShopRepository;

    @Autowired
    public RetailShopService(RetailShopRepository retailShopRepository) {
        this.retailShopRepository = retailShopRepository;
    }
}


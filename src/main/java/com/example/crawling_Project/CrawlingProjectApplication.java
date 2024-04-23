package com.example.crawling_Project;

import com.example.crawling_Project.service.ProductScraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CrawlingProjectApplication implements CommandLineRunner {

	@Autowired
	private ProductScraper productScraper;

	public static void main(String[] args) {
		SpringApplication.run(CrawlingProjectApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		productScraper.scrapeAndSaveProducts(args[0]);
	}
}

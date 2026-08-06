package com.example.portfolio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PortfolioApplication {

	private static final Logger log = LoggerFactory.getLogger(PortfolioApplication.class);

	public static void main(String[] args) {
		log.info("Starting Portfolio Management System application");
		SpringApplication.run(PortfolioApplication.class, args);
		log.info("Portfolio Management System application started successfully");
	}

}

package com.example.orderservice_choreography;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrderserviceChoreographyApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderserviceChoreographyApplication.class, args);
	}

}

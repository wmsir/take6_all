package com.example.take6server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Take6serverWebsockApplication {

	public static void main(String[] args) {
		SpringApplication.run(Take6serverWebsockApplication.class, args);
	}

}



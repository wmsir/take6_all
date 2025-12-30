package com.example.top_hog_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TopHogServerWebsockApplication {

	public static void main(String[] args) {
		SpringApplication.run(TopHogServerWebsockApplication.class, args);
	}

}



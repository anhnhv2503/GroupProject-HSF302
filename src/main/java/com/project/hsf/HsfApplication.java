package com.project.hsf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HsfApplication {

	public static void main(String[] args) {
		SpringApplication.run(HsfApplication.class, args);
	}

}

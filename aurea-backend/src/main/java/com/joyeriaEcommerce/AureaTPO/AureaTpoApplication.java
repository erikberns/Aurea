package com.joyeriaEcommerce.AureaTPO;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AureaTpoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AureaTpoApplication.class, args);
	}

}

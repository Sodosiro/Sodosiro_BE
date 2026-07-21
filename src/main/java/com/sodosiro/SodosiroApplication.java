package com.sodosiro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SodosiroApplication {

	public static void main(String[] args) {
		SpringApplication.run(SodosiroApplication.class, args);
	}

}

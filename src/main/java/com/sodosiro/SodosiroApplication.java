package com.sodosiro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
public class SodosiroApplication {

	public static void main(String[] args) {
		SpringApplication.run(SodosiroApplication.class, args);
	}

}

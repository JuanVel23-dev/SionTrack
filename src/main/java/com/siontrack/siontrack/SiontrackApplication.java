package com.siontrack.siontrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SiontrackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SiontrackApplication.class, args);
	}

}

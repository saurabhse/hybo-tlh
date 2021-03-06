package com.hack17.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan("com.hack17.hybo")
@ComponentScan("com.hack17.poc.util")
@EnableScheduling
public class TaxHarvestingPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaxHarvestingPocApplication.class, args);
	}
}

package com.neobank.log_ai_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling               // turns on the @Scheduled job in LogAnalysisScheduler
@ConfigurationPropertiesScan    // picks up GeminiProperties / LokiProperties / SummarizerProperties below
public class LogAiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LogAiServiceApplication.class, args);
	}

}

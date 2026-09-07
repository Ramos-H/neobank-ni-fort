package com.neobank.log_ai_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LogAiServiceApplicationTests {

	@Test
	void contextLoads() {
		// If this fails, it's almost always a @ConfigurationProperties binding
		// problem (check application.yaml keys) or a missing bean wiring —
		// not the AI/Loki calls themselves, which only fire on the schedule.
	}

}

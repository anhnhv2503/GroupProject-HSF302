package com.project.hsf;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Checks that the whole Spring context wires up: a missing bean or a wrong @Value fails here rather
 * than at runtime. Uses the test profile (H2 + dummy keys) so no SQL Server is needed.
 */
@SpringBootTest
@ActiveProfiles("test")
class HsfApplicationTests {

	@Test
	void contextLoads() {
	}

}

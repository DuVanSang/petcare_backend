package com.petcare.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"app.vaccine.seed-enabled=false",
		"app.reminder.scheduler-enabled=false"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}

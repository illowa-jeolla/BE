package com.example.travel;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class TravelApplicationTests {
	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void usesJdbcBatchRepositoryWithFlywayMetadataSchema() {
		assertThat(jobRepository.getClass().getName())
				.doesNotContain("ResourcelessJobRepository");
		Integer metadataTableCount = jdbcTemplate.queryForObject("""
				select count(*)
				from information_schema.tables
				where lower(table_name) = 'batch_job_instance'
				""", Integer.class);
		assertThat(metadataTableCount).isEqualTo(1);
	}

}

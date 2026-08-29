package com.pricing.backend;

import com.pricing.backend.fee.FeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FeeProductTypeMigrationTests {

	@Autowired
	private FeeRepository feeRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		feeRepository.deleteAll();
	}

	@Test
	void usesTwoColumnsForFeeProductTypes() {
		assertThat(jdbcTemplate.queryForList("""
				select column_name
				from information_schema.columns
				where table_name = 'FEE_PRODUCT_TYPES'
				order by ordinal_position
				""", String.class))
				.containsExactly("FEE_ID", "PRODUCT_TYPE");
	}
}

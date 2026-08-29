package com.pricing.backend;

import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccountAttributeProductTypeMigrationTests {

	@Autowired
	private AccountAttributeRepository accountAttributeRepository;

	@Autowired
	private EligibilityReasonRepository eligibilityReasonRepository;

	@Autowired
	private PricingPlanRepository pricingPlanRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		pricingPlanRepository.deleteAll();
		eligibilityReasonRepository.deleteAll();
		accountAttributeRepository.deleteAll();
	}

	@Test
	void usesTwoColumnsForAccountAttributeProductTypes() {
		assertThat(jdbcTemplate.queryForList("""
				select column_name
				from information_schema.columns
				where table_name = 'ACCOUNT_ATTRIBUTE_PRODUCT_TYPES'
				order by ordinal_position
				""", String.class))
				.containsExactly("ATTRIBUTE_ID", "PRODUCT_TYPE");
	}
}

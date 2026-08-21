package com.pricing.backend;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

import javax.sql.DataSource;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.generated.model.AttributeType;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

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
	private DataSource dataSource;

	@BeforeEach
	void setUp() {
		pricingPlanRepository.deleteAll();
		eligibilityReasonRepository.deleteAll();
		accountAttributeRepository.deleteAll();
	}

	@Test
	void backfillsOnlyAccountAttributesWithoutProductTypes() throws SQLException {
		AccountAttributeEntity emptyAttribute = saveAttribute("ATTR0001", List.of());
		AccountAttributeEntity scopedAttribute = saveAttribute("ATTR0002", List.of(ProductType.DEPOSIT));

		try (Connection connection = dataSource.getConnection()) {
			new ResourceDatabasePopulator(new ClassPathResource(
					"db/migration/V22__backfill_empty_account_attribute_product_types.sql"))
					.populate(connection);
		}

		assertThat(accountAttributeRepository.findById(emptyAttribute.getId()).orElseThrow()
				.getProductTypes())
				.containsExactly(ProductType.DEPOSIT, ProductType.CD, ProductType.CREDIT);
		assertThat(accountAttributeRepository.findById(scopedAttribute.getId()).orElseThrow()
				.getProductTypes())
				.containsExactly(ProductType.DEPOSIT);
	}

	private AccountAttributeEntity saveAttribute(String code, List<ProductType> productTypes) {
		return accountAttributeRepository.save(AccountAttributeEntity.builder()
				.attributeCode(code)
				.attributeName("Attribute " + code)
				.attributeType(AttributeType.INTEGER)
				.productTypes(productTypes)
				.updatedBy("test")
				.updatedOn(OffsetDateTime.now())
				.build());
	}
}

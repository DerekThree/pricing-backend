package com.pricing.backend;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

import javax.sql.DataSource;

import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.FeeType;
import com.pricing.backend.generated.model.ProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FeeProductTypeMigrationTests {

	@Autowired
	private FeeRepository feeRepository;

	@Autowired
	private DataSource dataSource;

	@BeforeEach
	void setUp() {
		feeRepository.deleteAll();
	}

	@Test
	void backfillsOnlyFeesWithoutProductTypes() throws SQLException {
		FeeEntity emptyFee = saveFee("FEE00001", List.of());
		FeeEntity scopedFee = saveFee("FEE00002", List.of(ProductType.DEPOSIT));

		try (Connection connection = dataSource.getConnection()) {
			new ResourceDatabasePopulator(
					new ClassPathResource("db/migration/V20__backfill_empty_fee_product_types.sql"))
					.populate(connection);
		}

		assertThat(feeRepository.findById(emptyFee.getId()).orElseThrow().getProductTypes())
				.containsExactly(ProductType.DEPOSIT, ProductType.CD, ProductType.CREDIT);
		assertThat(feeRepository.findById(scopedFee.getId()).orElseThrow().getProductTypes())
				.containsExactly(ProductType.DEPOSIT);
	}

	private FeeEntity saveFee(String code, List<ProductType> productTypes) {
		return feeRepository.save(FeeEntity.builder()
				.feeCode(code)
				.feeName("Fee " + code)
				.feeType(FeeType.FLAT)
				.productTypes(productTypes)
				.updatedBy("test")
				.updatedOn(OffsetDateTime.now())
				.build());
	}
}

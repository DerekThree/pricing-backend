package com.pricing.backend;

import java.time.OffsetDateTime;
import java.util.List;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.branch.BranchRepository;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.AttributeType;
import com.pricing.backend.generated.model.FeeType;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import com.pricing.backend.product.ProductEntity;
import com.pricing.backend.product.ProductRepository;
import com.pricing.backend.region.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimulatorOptionsApiTests {

	private static final OffsetDateTime UPDATED_ON =
			OffsetDateTime.parse("2026-06-06T09:00:00+08:00");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private FeeRepository feeRepository;

	@Autowired
	private AccountAttributeRepository accountAttributeRepository;

	@Autowired
	private PricingPlanRepository pricingPlanRepository;

	@Autowired
	private EligibilityReasonRepository eligibilityReasonRepository;

	@Autowired
	private RegionRepository regionRepository;

	@BeforeEach
	void setUp() {
		pricingPlanRepository.deleteAll();
		eligibilityReasonRepository.deleteAll();
		accountAttributeRepository.deleteAll();
		feeRepository.deleteAll();
		regionRepository.deleteAll();
		productRepository.deleteAll();
		branchRepository.deleteAll();
	}

	@Test
	void returnsEveryPersistedOptionWithAllContractFields() throws Exception {
		saveProduct("PROD0002", "Certificate", ProductType.CD);
		ProductEntity deposit = saveProduct("PROD0001", "Premier Checking", ProductType.DEPOSIT);
		saveBranch("BR000002", "Austin Central");
		BranchEntity chicago = saveBranch("BR000001", "Chicago 104th");
		saveFee("FEE00002", "Early Withdrawal", FeeType.PERCENT, ProductType.CD);
		FeeEntity maintenance = saveFee(
				"FEE00001", "Monthly Maintenance", FeeType.FLAT,
				ProductType.DEPOSIT, ProductType.CD);
		saveAttribute("ATTR0002", "Term Months", AttributeType.INTEGER, ProductType.CD);
		AccountAttributeEntity balance = saveAttribute(
				"ATTR0001", "Average Balance", AttributeType.DECIMAL,
				ProductType.DEPOSIT, ProductType.CD);

		mockMvc.perform(get("/simulator/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.products.length()").value(2))
				.andExpect(jsonPath("$.products[0].id").value(deposit.getId()))
				.andExpect(jsonPath("$.products[0].code").value("PROD0001"))
				.andExpect(jsonPath("$.products[0].name").value("Premier Checking"))
				.andExpect(jsonPath("$.products[0].type").value("DEPOSIT"))
				.andExpect(jsonPath("$.products[1].code").value("PROD0002"))
				.andExpect(jsonPath("$.products[1].name").value("Certificate"))
				.andExpect(jsonPath("$.products[1].type").value("CD"))
				.andExpect(jsonPath("$.branches.length()").value(2))
				.andExpect(jsonPath("$.branches[0].id").value(chicago.getId()))
				.andExpect(jsonPath("$.branches[0].code").value("BR000001"))
				.andExpect(jsonPath("$.branches[0].name").value("Chicago 104th"))
				.andExpect(jsonPath("$.branches[1].code").value("BR000002"))
				.andExpect(jsonPath("$.branches[1].name").value("Austin Central"))
				.andExpect(jsonPath("$.fees.length()").value(2))
				.andExpect(jsonPath("$.fees[0].id").value(maintenance.getId()))
				.andExpect(jsonPath("$.fees[0].code").value("FEE00001"))
				.andExpect(jsonPath("$.fees[0].name").value("Monthly Maintenance"))
				.andExpect(jsonPath("$.fees[0].type").value("FLAT"))
				.andExpect(jsonPath("$.fees[0].productTypes")
						.value(containsInAnyOrder("DEPOSIT", "CD")))
				.andExpect(jsonPath("$.fees[1].code").value("FEE00002"))
				.andExpect(jsonPath("$.fees[1].name").value("Early Withdrawal"))
				.andExpect(jsonPath("$.fees[1].type").value("PERCENT"))
				.andExpect(jsonPath("$.fees[1].productTypes").value(contains("CD")))
				.andExpect(jsonPath("$.attributes.length()").value(2))
				.andExpect(jsonPath("$.attributes[0].id").value(balance.getId()))
				.andExpect(jsonPath("$.attributes[0].code").value("ATTR0001"))
				.andExpect(jsonPath("$.attributes[0].name").value("Average Balance"))
				.andExpect(jsonPath("$.attributes[0].type").value("DECIMAL"))
				.andExpect(jsonPath("$.attributes[0].productTypes")
						.value(containsInAnyOrder("DEPOSIT", "CD")))
				.andExpect(jsonPath("$.attributes[1].code").value("ATTR0002"))
				.andExpect(jsonPath("$.attributes[1].name").value("Term Months"))
				.andExpect(jsonPath("$.attributes[1].type").value("INTEGER"))
				.andExpect(jsonPath("$.attributes[1].productTypes").value(contains("CD")));
	}

	@Test
	void returnsFourPresentEmptyArraysWhenNoOptionsExist() throws Exception {
		mockMvc.perform(get("/simulator/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.products").value(empty()))
				.andExpect(jsonPath("$.branches").value(empty()))
				.andExpect(jsonPath("$.fees").value(empty()))
				.andExpect(jsonPath("$.attributes").value(empty()));
	}

	private ProductEntity saveProduct(String code, String name, ProductType type) {
		return productRepository.save(ProductEntity.builder()
				.productCode(code)
				.productName(name)
				.productType(type)
				.updatedOn(UPDATED_ON)
				.updatedBy("Derek Ochal")
				.build());
	}

	private BranchEntity saveBranch(String code, String name) {
		return branchRepository.save(BranchEntity.builder()
				.branchCode(code)
				.branchName(name)
				.state("IL")
				.zipCode("60459")
				.updatedOn(UPDATED_ON)
				.updatedBy("Derek Ochal")
				.build());
	}

	private FeeEntity saveFee(String code, String name, FeeType type, ProductType... productTypes) {
		return feeRepository.save(FeeEntity.builder()
				.feeCode(code)
				.feeName(name)
				.feeType(type)
				.productTypes(List.of(productTypes))
				.updatedOn(UPDATED_ON)
				.updatedBy("Derek Ochal")
				.build());
	}

	private AccountAttributeEntity saveAttribute(
			String code, String name, AttributeType type, ProductType... productTypes) {
		return accountAttributeRepository.save(AccountAttributeEntity.builder()
				.attributeCode(code)
				.attributeName(name)
				.attributeType(type)
				.productTypes(List.of(productTypes))
				.updatedOn(UPDATED_ON)
				.updatedBy("Derek Ochal")
				.build());
	}
}

package com.pricing.backend;

import java.time.OffsetDateTime;

import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeeApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FeeRepository feeRepository;

	@Autowired
	private PricingPlanRepository pricingPlanRepository;

	@BeforeEach
	void setUp() {
		pricingPlanRepository.deleteAll();
		feeRepository.deleteAll();
	}

	@Test
	void createsFeeAndListsIt() throws Exception {
		mockMvc.perform(post("/fees")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "feeCode": "FEE00001",
								  "feeName": "Monthly Maintenance Fee",
								  "feeType": "FLAT",
								  "productTypes": ["DEPOSIT"],
								  "updatedBy": "Derek Ochal"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.feeCode").value("FEE00001"))
				.andExpect(jsonPath("$.feeName").value("Monthly Maintenance Fee"))
				.andExpect(jsonPath("$.feeType").value("FLAT"))
				.andExpect(jsonPath("$.productTypes[0]").value("DEPOSIT"));

		mockMvc.perform(get("/fees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].fee").value("FEE00001 - Monthly Maintenance Fee"))
				.andExpect(jsonPath("$[0].feeType").value("FLAT"))
				.andExpect(jsonPath("$[0].productTypes[0]").value("DEPOSIT"));
	}
}

package com.pricing.backend;

import java.time.OffsetDateTime;

import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.ProductType;
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

	@BeforeEach
	void setUp() {
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
								  "productType": "DEPOSIT",
								  "updatedBy": "Derek Ochal"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.feeCode").value("FEE00001"))
				.andExpect(jsonPath("$.feeName").value("Monthly Maintenance Fee"))
				.andExpect(jsonPath("$.productType").value("DEPOSIT"));

		mockMvc.perform(get("/fees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].fee").value("FEE00001 - Monthly Maintenance Fee"))
				.andExpect(jsonPath("$[0].productType").value("DEPOSIT"));
	}
}

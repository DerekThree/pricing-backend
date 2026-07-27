package com.pricing.backend;

import java.time.OffsetDateTime;

import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.branch.BranchRepository;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import com.pricing.backend.region.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DuplicateCodeApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private PricingPlanRepository pricingPlanRepository;

	@BeforeEach
	void setUp() {
		pricingPlanRepository.deleteAll();
		regionRepository.deleteAll();
		branchRepository.deleteAll();
	}

	@Test
	void creatingBranchWithExistingCodeReturnsConflict() throws Exception {
		branchRepository.save(new BranchEntity(
				null,
				"10000001",
				"Chicago 104th",
				"IL",
				"60459",
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		));
		mockMvc.perform(post("/branches")
						.contentType(MediaType.APPLICATION_JSON)
						.content(branchRequestJson("10000001", "Austin Central", "TX", "73301", "Jane Smith")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("same code already exists")));
	}

	@Test
	void updatingBranchWithExistingCodeReturnsConflict() throws Exception {
		BranchEntity originalBranch = branchRepository.save(new BranchEntity(
				null,
				"10000001",
				"Chicago 104th",
				"IL",
				"60459",
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		));
		branchRepository.save(new BranchEntity(
				null,
				"10000002",
				"Austin Central",
				"TX",
				"73301",
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"John Smith"
		));
		mockMvc.perform(put("/branches/{id}", originalBranch.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(branchRequestJson("10000002", "Chicago Loop", "IL", "60601", "Jane Smith")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("same code already exists")));
	}

	private String branchRequestJson(
			String branchCode, String branchName, String state, String zipCode, String updatedBy) {
		return """
				{
				  "branchCode": "%s",
				  "branchName": "%s",
				  "state": "%s",
				  "zipCode": "%s",
				  "updatedBy": "%s"
				}
				""".formatted(branchCode, branchName, state, zipCode, updatedBy);
	}
}

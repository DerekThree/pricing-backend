package com.pricing.backend;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.branch.BranchRepository;
import com.pricing.backend.region.RegionEntity;
import com.pricing.backend.region.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RegionApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private RegionRepository regionRepository;

	@BeforeEach
	void setUp() {
		regionRepository.deleteAll();
		branchRepository.deleteAll();
	}

	@Test
	void returnsRegionWithEmbeddedCoverageOptions() throws Exception {
		BranchEntity firstBranch = branchRepository.save(BranchEntity.builder()
				.branchCode("10000001")
				.branchName("Chicago 104th")
				.state("IL")
				.zipCode("60459")
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		BranchEntity secondBranch = branchRepository.save(BranchEntity.builder()
				.branchCode("10000002")
				.branchName("Austin Mopec")
				.state("TX")
				.zipCode("78759")
				.updatedBy("John Smith")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		RegionEntity midwest = regionRepository.save(RegionEntity.builder()
				.regionCode("MIDWEST1")
				.regionName("Midwest")
				.states(new LinkedHashSet<>(List.of("IL", "IN", "MI", "OH", "WI")))
				.zipCodes(new LinkedHashSet<>(List.of("60459", "60601", "46204", "48201", "53202")))
				.branches(new LinkedHashSet<>(Set.of(firstBranch.getId())))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:30:00+08:00"))
				.build());
		regionRepository.save(RegionEntity.builder()
				.regionCode("SOUTH001")
				.regionName("South")
				.states(new LinkedHashSet<>(List.of("TX", "FL", "GA")))
				.zipCodes(new LinkedHashSet<>(List.of("78759", "33101", "30301")))
				.branches(new LinkedHashSet<>(Set.of(secondBranch.getId())))
				.updatedBy("John Smith")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:30:00+08:00"))
				.build());

		mockMvc.perform(get("/regions/{id}", midwest.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(midwest.getId()))
				.andExpect(jsonPath("$.regionCode").value("MIDWEST1"))
				.andExpect(jsonPath("$.regionName").value("Midwest"))
				.andExpect(jsonPath("$.branches[0]").value(firstBranch.getId()))
				.andExpect(jsonPath("$.states", contains("IL", "IN", "MI", "OH", "WI")))
				.andExpect(jsonPath("$.zipCodes", containsInAnyOrder("60459", "60601", "46204", "48201", "53202")))
				.andExpect(jsonPath("$.options.states", contains("IL", "IN", "MI", "OH", "WI")))
				.andExpect(jsonPath("$.options.zipCodes", containsInAnyOrder("60459", "60601", "46204", "48201", "53202")))
				.andExpect(jsonPath("$.options.branches[0].id").value(firstBranch.getId()))
				.andExpect(jsonPath("$.options.branches[0].code").value("10000001"))
				.andExpect(jsonPath("$.options.branches[0].name").value("Chicago 104th"));
	}

	@Test
	void returnsOnlyUnusedCoverageOptions() throws Exception {
		BranchEntity firstBranch = branchRepository.save(BranchEntity.builder()
				.branchCode("10000001")
				.branchName("Chicago 104th")
				.state("IL")
				.zipCode("60459")
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		BranchEntity secondBranch = branchRepository.save(BranchEntity.builder()
				.branchCode("10000002")
				.branchName("Austin Mopec")
				.state("TX")
				.zipCode("78759")
				.updatedBy("John Smith")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		regionRepository.save(RegionEntity.builder()
				.regionCode("MIDWEST1")
				.regionName("Midwest")
				.states(new LinkedHashSet<>(List.of("IL")))
				.zipCodes(new LinkedHashSet<>(List.of("60459")))
				.branches(new LinkedHashSet<>(Set.of(firstBranch.getId())))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:30:00+08:00"))
				.build());

		mockMvc.perform(get("/regions/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.states", contains("TX")))
				.andExpect(jsonPath("$.zipCodes", contains("78759")))
				.andExpect(jsonPath("$.branches[0].id").value(secondBranch.getId()))
				.andExpect(jsonPath("$.branches[0].code").value("10000002"))
				.andExpect(jsonPath("$.branches[0].name").value("Austin Mopec"));
	}
}

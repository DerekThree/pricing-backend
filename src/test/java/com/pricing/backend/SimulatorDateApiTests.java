package com.pricing.backend;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.pricing.backend.generated.api.SimulatorApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "dashboard.origin=https://dashboard.example.test")
@AutoConfigureMockMvc
class SimulatorDateApiTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Clock clock;

	@BeforeEach
	void setUp() {
		when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		when(clock.instant()).thenReturn(Instant.parse("2026-08-11T00:00:00Z"));
	}

	@Test
	void readsConfiguredSystemDate() throws Exception {
		mockMvc.perform(get(SimulatorApi.PATH_GET_SIMULATOR_DATE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentDate").value("2026-08-11"));
	}

	@Test
	void setsAndRepeatedlyChangesApplicationDate() throws Exception {
		setDate("2026-09-01");
		readDate("2026-09-01");
		setDate("2026-10-15");
		readDate("2026-10-15");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://localhost:5173", "http://127.0.0.1:5174", "https://dashboard.example.test"
	})
	void permitsBrowserUpdates(String origin) throws Exception {
		mockMvc.perform(options(SimulatorApi.PATH_SET_SIMULATOR_DATE)
				.header("Origin", origin)
				.header("Access-Control-Request-Method", "PUT")
				.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", origin));
	}

	@Test
	void rejectsOtherBrowserOrigins() throws Exception {
		mockMvc.perform(put(SimulatorApi.PATH_SET_SIMULATOR_DATE)
				.header("Origin", "https://example.com")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentDate\":\"2026-09-01\"}"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	private void setDate(String date) throws Exception {
		mockMvc.perform(put(SimulatorApi.PATH_SET_SIMULATOR_DATE)
					.header("Origin", "https://dashboard.example.test")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"currentDate\":\"%s\"}".formatted(date)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentDate").value(date));
	}

	private void readDate(String date) throws Exception {
		mockMvc.perform(get(SimulatorApi.PATH_GET_SIMULATOR_DATE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentDate").value(date));
	}
}

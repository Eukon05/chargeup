package ovh.eukon05.chargeup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnergyControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_get_current_mix() throws Exception {
        LocalDate now = LocalDate.now();
        mockMvc.perform(get("/api/v1/energy/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mixes").isArray())
                .andExpect(jsonPath("$.mixes[0].date").value(now.toString()))
                .andExpect(jsonPath("$.mixes[0].sourceMix").isMap())
                .andExpect(jsonPath("$.mixes[0].cleanPerc").isNotEmpty())
                .andExpect(jsonPath("$.mixes[1].date").value(now.plusDays(1).toString()))
                .andExpect(jsonPath("$.mixes[1].sourceMix").isMap())
                .andExpect(jsonPath("$.mixes[1].cleanPerc").isNumber())
                .andExpect(jsonPath("$.mixes[2].date").value(now.plusDays(2).toString()))
                .andExpect(jsonPath("$.mixes[2].sourceMix").isMap())
                .andExpect(jsonPath("$.mixes[2].cleanPerc").isNumber());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void should_get_optimal_charge_window(int windowLength) throws Exception {
        mockMvc.perform(get("/api/v1/energy/window?windowLength=" + windowLength))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").exists())
                .andExpect(jsonPath("$.to").exists())
                .andExpect(jsonPath("$.cleanPerc").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 7})
    void should_reject_invalid_charge_window_length(int window) throws Exception {
        mockMvc.perform(get("/api/v1/energy/window?windowLength=" + window))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_reject_missing_charge_window_length() throws Exception {
        mockMvc.perform(get("/api/v1/energy/window"))
                .andExpect(status().isBadRequest());
    }
}

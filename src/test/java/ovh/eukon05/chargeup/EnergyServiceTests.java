package ovh.eukon05.chargeup;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ovh.eukon05.chargeup.client.ApiClient;
import ovh.eukon05.chargeup.dto.OptimalChargeWindowResponseDTO;
import ovh.eukon05.chargeup.model.DailyMix;
import ovh.eukon05.chargeup.model.GenerationMix;
import ovh.eukon05.chargeup.model.MixInterval;
import ovh.eukon05.chargeup.service.EnergyService;
import ovh.eukon05.chargeup.service.EnergyServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnergyServiceTests {
    private final ApiClient client = Mockito.mock(ApiClient.class);
    private final EnergyService service = new EnergyServiceImpl(client);

    @Test
    void should_return_current_mix() {
        Map<LocalDate, List<MixInterval>> testData = new HashMap<>();
        List<GenerationMix> genMixOne = List.of(new GenerationMix("wind", 50.0), new GenerationMix("solar", 30.0), new GenerationMix("coal", 20.0));
        List<GenerationMix> genMixTwo = List.of(new GenerationMix("wind", 5.0), new GenerationMix("biomass", 10.0), new GenerationMix("hydro", 40.0), new GenerationMix("gas", 45.0));
        List<MixInterval> mixes = new ArrayList<>();
        LocalDateTime start = LocalDate.now().atTime(0, 0, 0);

        mixes.add(new MixInterval(start, start.plusMinutes(30), genMixOne));
        mixes.add(new MixInterval(start.plusMinutes(30), start.plusMinutes(60), genMixTwo));
        testData.put(LocalDate.now(), mixes);

        Mockito.when(client.getIntervals(Mockito.any(), Mockito.any())).thenReturn(testData);
        DailyMix mix = service.getCurrentMix().mixes().getFirst();

        assertEquals(LocalDate.now(), mix.date());

        // These are the averages for each source in the day (og value divided by 2 since we only have two measurements in this test)
        assertEquals(27.5, mix.sourceMix().get("wind"));
        assertEquals(15.0, mix.sourceMix().get("solar"));
        assertEquals(5.0, mix.sourceMix().get("biomass"));
        assertEquals(10.0, mix.sourceMix().get("coal"));
        assertEquals(20.0, mix.sourceMix().get("hydro"));
        assertEquals(22.5, mix.sourceMix().get("gas"));

        // This is the average of all clean sources in the day
        assertEquals(67.5, mix.cleanPerc());
    }

    @Test
    void should_return_optimal_charge_window() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<MixInterval> mixes = new ArrayList<>();
        mixes.add(new MixInterval(start, start.plusMinutes(30), List.of(new GenerationMix("wind", 10.0), new GenerationMix("coal", 90.0))));
        mixes.add(new MixInterval(start.plusMinutes(30), start.plusMinutes(60), List.of(new GenerationMix("wind", 20.0), new GenerationMix("coal", 80.0))));
        mixes.add(new MixInterval(start.plusMinutes(60), start.plusMinutes(90), List.of(new GenerationMix("wind", 80.0), new GenerationMix("coal", 20.0))));
        mixes.add(new MixInterval(start.plusMinutes(90), start.plusMinutes(120), List.of(new GenerationMix("wind", 90.0), new GenerationMix("coal", 10.0))));
        mixes.add(new MixInterval(start.plusMinutes(120), start.plusMinutes(150), List.of(new GenerationMix("wind", 10.0), new GenerationMix("coal", 90.0))));
        mixes.add(new MixInterval(start.plusMinutes(150), start.plusMinutes(180), List.of(new GenerationMix("wind", 20.0), new GenerationMix("coal", 80.0))));

        Map<LocalDate, List<MixInterval>> testData = Map.of(start.toLocalDate(), mixes);
        Mockito.when(client.getIntervals(Mockito.any(), Mockito.any())).thenReturn(testData);

        OptimalChargeWindowResponseDTO result = service.getOptimalChargeWindow(1);

        // Optimal window should be from 01:00 to 02:00.
        // 01:00 - 01:30 has a 90% clean share, 01:30 - 02:00 has an 80%.
        // Average share during this window: (90 + 80) / 2 = 85
        assertEquals(start.plusMinutes(60), result.from());
        assertEquals(start.plusMinutes(120), result.to());
        assertEquals(85.0, result.cleanPerc());
    }

    @Test
    void should_throw_when_window_out_of_range() {
        assertThrows(IllegalArgumentException.class, () -> service.getOptimalChargeWindow(-1));
        assertThrows(IllegalArgumentException.class, () -> service.getOptimalChargeWindow(0));
        assertThrows(IllegalArgumentException.class, () -> service.getOptimalChargeWindow(7));
    }
}

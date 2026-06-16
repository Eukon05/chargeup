package ovh.eukon05.chargeup.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ovh.eukon05.chargeup.client.ApiClient;
import ovh.eukon05.chargeup.dto.CurrentMixResponseDTO;
import ovh.eukon05.chargeup.dto.OptimalChargeWindowResponseDTO;
import ovh.eukon05.chargeup.model.DailyMix;
import ovh.eukon05.chargeup.model.GenerationMix;
import ovh.eukon05.chargeup.model.HourlyCleanMix;
import ovh.eukon05.chargeup.model.HourlyMix;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class EnergyServiceImpl implements EnergyService {
    private static final List<String> CLEAN_SOURCES = List.of("wind", "solar", "nuclear", "biomass", "hydro");
    private final ApiClient apiClient;

    @Override
    public CurrentMixResponseDTO getCurrentMix() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(1).withSecond(0).withNano(0);
        LocalDateTime end = start.plusDays(2).withHour(23).withMinute(59).withSecond(59);

        Map<LocalDate, List<HourlyMix>> dailyMixes = apiClient.getHourlyMixes(start, end);
        List<DailyMix> result = new ArrayList<>();

        dailyMixes.forEach((date, mix) -> {
            Map<String, Double> generationAvg = mix.stream()
                    .flatMap(hourlyMix -> hourlyMix.generationMix().stream())
                    .collect(Collectors.groupingBy(GenerationMix::fuel, Collectors.averagingDouble(GenerationMix::perc)));

            double cleanPerc = 0;
            for (String source : CLEAN_SOURCES) cleanPerc += generationAvg.getOrDefault(source, 0.0);

            result.add(new DailyMix(date, generationAvg, cleanPerc));
        });

        result.sort(Comparator.comparing(DailyMix::date));
        return new CurrentMixResponseDTO(result);
    }

    @Override
    public OptimalChargeWindowResponseDTO getOptimalChargeWindow(int windowLength) {
        if (windowLength <= 0 || windowLength > 6) {
            throw new IllegalArgumentException("Invalid window length"); // as per task requirements, window between 1 and 6
        }

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusDays(2).withHour(23).withMinute(59).withSecond(59);

        Map<LocalDate, List<HourlyMix>> dailyMixes = apiClient.getHourlyMixes(start, end);
        List<HourlyCleanMix> measurements = dailyMixes.values().stream()
                .flatMap(List::stream)
                .map(hourlyMix -> new HourlyCleanMix(hourlyMix.from(), hourlyMix.to(),
                        hourlyMix.generationMix().stream()
                                .filter(genMix -> CLEAN_SOURCES.contains(genMix.fuel()))
                                .mapToDouble(GenerationMix::perc)
                                .sum())) //we calculate the sum of clean energy sources in the measurement
                .sorted(Comparator.comparing(HourlyCleanMix::from))
                .toList();

        windowLength *= 2; // The data comes in 30-minute intervals, so we have to double the window length
        double currMax = 0;
        double currCheck;
        LocalDateTime maxStart = measurements.getFirst().from();
        LocalDateTime maxEnd = measurements.get(windowLength - 1).to();

        if (windowLength > measurements.size()) {
            throw new IllegalArgumentException("Not enough measurements");
        }

        // init the sliding window with initial measurements
        for (int i = 0; i < windowLength; i++) {
            currMax += measurements.get(i).cleanPerc();
        }

        // sliding window alg - we move the window one measurement at a time and check if the new sum is greater than current
        for (int i = windowLength; i < measurements.size(); i++) {
            currCheck = currMax - measurements.get(i - windowLength).cleanPerc() + measurements.get(i).cleanPerc();
            if (currCheck > currMax) {
                currMax = currCheck;
                maxStart = measurements.get(i - windowLength + 1).from();
                maxEnd = measurements.get(i).to();
            }
        }

        return new OptimalChargeWindowResponseDTO(maxStart, maxEnd, currMax / windowLength);
    }
}

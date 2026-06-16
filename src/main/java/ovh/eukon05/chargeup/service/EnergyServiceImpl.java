package ovh.eukon05.chargeup.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ovh.eukon05.chargeup.client.ApiClient;
import ovh.eukon05.chargeup.dto.CurrentMixResponseDTO;
import ovh.eukon05.chargeup.dto.OptimalChargeWindowResponseDTO;
import ovh.eukon05.chargeup.model.CleanMixInterval;
import ovh.eukon05.chargeup.model.DailyMix;
import ovh.eukon05.chargeup.model.GenerationMix;
import ovh.eukon05.chargeup.model.MixInterval;

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

        Map<LocalDate, List<MixInterval>> intervals = apiClient.getIntervals(start, end);
        List<DailyMix> result = new ArrayList<>();

        intervals.forEach((date, mixes) -> {
            Map<String, Double> fuelSums = mixes.stream()
                    .flatMap(mixInterval -> mixInterval.generationMix().stream())
                    .collect(Collectors.groupingBy(GenerationMix::fuel, Collectors.summingDouble(GenerationMix::perc)));

            double cleanPerc = 0;
            for (Map.Entry<String, Double> entry : fuelSums.entrySet()) {
                if (CLEAN_SOURCES.contains(entry.getKey())) cleanPerc += entry.getValue();
                entry.setValue(entry.getValue() / mixes.size());
            }

            result.add(new DailyMix(date, fuelSums, cleanPerc / mixes.size()));
        });

        result.sort(Comparator.comparing(DailyMix::date));
        return new CurrentMixResponseDTO(result);
    }

    @Override
    public OptimalChargeWindowResponseDTO getOptimalChargeWindow(int windowLength) {
        if (windowLength <= 0 || windowLength > 6) {
            throw new IllegalArgumentException("Invalid window length"); // as per task requirements, window between 1 and 6
        }

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(0).withMinute(1).withSecond(0).withNano(0);
        LocalDateTime end = start.plusDays(1).withHour(23).withMinute(59).withSecond(59);

        Map<LocalDate, List<MixInterval>> intervals = apiClient.getIntervals(start, end);
        List<CleanMixInterval> cleanIntervals = intervals.values().stream()
                .flatMap(List::stream)
                .map(mixInterval -> new CleanMixInterval(mixInterval.from(), mixInterval.to(),
                        mixInterval.generationMix().stream()
                                .filter(genMix -> CLEAN_SOURCES.contains(genMix.fuel()))
                                .mapToDouble(GenerationMix::perc)
                                .sum())) //we calculate the sum of clean energy sources in the measurement
                .sorted(Comparator.comparing(CleanMixInterval::from))
                .toList();

        windowLength *= 2; // The data comes in 30-minute intervals, so we have to double the window length

        if (windowLength > cleanIntervals.size()) throw new IllegalArgumentException("Not enough measurements");

        double currMax = 0;
        LocalDateTime maxStart = cleanIntervals.getFirst().from();
        LocalDateTime maxEnd = cleanIntervals.get(windowLength - 1).to();

        // init the sliding window with initial measurements
        for (int i = 0; i < windowLength; i++) {
            currMax += cleanIntervals.get(i).cleanPerc();
        }

        double currCheck = currMax;

        // sliding window alg - we move the window one measurement at a time and check if the new sum is greater than current
        for (int i = windowLength; i < cleanIntervals.size(); i++) {
            currCheck = currCheck - cleanIntervals.get(i - windowLength).cleanPerc() + cleanIntervals.get(i).cleanPerc();
            if (currCheck > currMax) {
                currMax = currCheck;
                maxStart = cleanIntervals.get(i - windowLength + 1).from();
                maxEnd = cleanIntervals.get(i).to();
            }
        }

        return new OptimalChargeWindowResponseDTO(maxStart, maxEnd, currMax / windowLength);
    }
}

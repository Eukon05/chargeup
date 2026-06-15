package ovh.eukon05.chargeup.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ovh.eukon05.chargeup.dto.OptimalChargeWindowResponseDTO;
import ovh.eukon05.chargeup.exception.ApiFetchException;
import ovh.eukon05.chargeup.model.DailyMix;
import ovh.eukon05.chargeup.model.GenerationMix;
import ovh.eukon05.chargeup.model.HourlyCleanMix;
import ovh.eukon05.chargeup.model.HourlyMix;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class EnergyServiceImpl implements EnergyService {
    private static final String API_URL = "https://api.carbonintensity.org.uk/generation/%s/%s";
    private static final List<String> CLEAN_SOURCES = List.of("wind", "solar", "nuclear", "biomass", "hydro");
    private final ObjectMapper mapper;

    @Override
    public List<DailyMix> getCurrentMix() {
        Map<LocalDate, List<HourlyMix>> dailyMixes = getHourlyMixes(LocalDateTime.now(), LocalDateTime.now().plusDays(2));
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
        return result;
    }

    @Override
    public OptimalChargeWindowResponseDTO getOptimalChargeWindow(int windowLength) {
        if (windowLength <= 0 || windowLength > 6) {
            throw new RuntimeException("Invalid window length"); // as per task requirements, window between 1 and 6
        }

        Map<LocalDate, List<HourlyMix>> dailyMixes = getHourlyMixes(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
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
            throw new RuntimeException("Not enough measurements");
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

    private Map<LocalDate, List<HourlyMix>> getHourlyMixes(LocalDateTime start, LocalDateTime end) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(String.format(API_URL, start, end))).build();
        HttpResponse<String> res;

        try (HttpClient client = HttpClient.newHttpClient()) {
            res = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ApiFetchException();
        }

        ArrayNode dataNode = mapper.readTree(res.body()).get("data").asArray();

        return dataNode.valueStream()
                .map(node -> mapper.treeToValue(node, HourlyMix.class))
                .collect(Collectors.groupingBy(mix -> mix.from().toLocalDate(), HashMap::new, Collectors.toList()));
    }
}

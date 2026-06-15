package ovh.eukon05.chargeup.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ovh.eukon05.chargeup.dto.OptimalChargeWindowResponseDTO;
import ovh.eukon05.chargeup.exception.ApiFetchException;
import ovh.eukon05.chargeup.model.DailyMix;
import ovh.eukon05.chargeup.model.GenerationMix;
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
    private final ObjectMapper mapper;

    @Override
    public List<DailyMix> getCurrentMix() {
        Map<LocalDate, List<HourlyMix>> dailyMixes = getHourlyMixes();
        List<DailyMix> result = new ArrayList<>();

        dailyMixes.forEach((date, mix) -> {
            Map<String, Double> generationAvg = mix.stream()
                    .flatMap(hourlyMix -> hourlyMix.generationMix().stream())
                    .collect(Collectors.groupingBy(GenerationMix::fuel, Collectors.averagingDouble(GenerationMix::perc)));

            double cleanPerc = generationAvg.get("wind") + generationAvg.get("solar") + generationAvg.get("hydro") + generationAvg.get("biomass") + generationAvg.get("nuclear");
            result.add(new DailyMix(date, generationAvg, cleanPerc));
        });

        result.sort(Comparator.comparing(DailyMix::date));
        return result;
    }

    @Override
    public OptimalChargeWindowResponseDTO getOptimalChargeWindow(int windowLength) {
        return null;
    }

    private Map<LocalDate, List<HourlyMix>> getHourlyMixes() {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime endDate = today.plusDays(2);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(String.format(API_URL, today, endDate))).build();
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

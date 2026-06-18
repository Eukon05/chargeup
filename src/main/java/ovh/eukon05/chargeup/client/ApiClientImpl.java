package ovh.eukon05.chargeup.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ovh.eukon05.chargeup.exception.ApiFetchException;
import ovh.eukon05.chargeup.model.MixInterval;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiClientImpl implements ApiClient {
    private static final String API_URL = "https://api.carbonintensity.org.uk/generation/%sZ/%sZ";
    private final ObjectMapper mapper;

    public Map<LocalDate, List<MixInterval>> getIntervals(LocalDateTime start, LocalDateTime end) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(String.format(API_URL, start, end))).build();
        HttpResponse<String> res;

        try (HttpClient client = HttpClient.newHttpClient()) {
            res = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ApiFetchException();
        }

        if (res.statusCode() != 200) throw new ApiFetchException();

        ArrayNode dataNode = mapper.readTree(res.body()).get("data").asArray();

        return dataNode.valueStream()
                .map(node -> mapper.treeToValue(node, MixInterval.class))
                .collect(Collectors.groupingBy(mix -> mix.from().toLocalDate(), HashMap::new, Collectors.toList()));
    }
}

package ovh.eukon05.chargeup.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record HourlyMix(LocalDateTime from, LocalDateTime to,
                        @JsonProperty("generationmix") List<GenerationMix> generationMix) {
}

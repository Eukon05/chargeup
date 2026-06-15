package ovh.eukon05.chargeup.dto;

import ovh.eukon05.chargeup.model.DailyMix;

import java.util.List;

public record CurrentMixResponseDTO(List<DailyMix> mixes) {

}

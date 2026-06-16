package ovh.eukon05.chargeup.dto;

import java.time.LocalDateTime;

public record OptimalChargeWindowResponseDTO(LocalDateTime from, LocalDateTime to, double cleanPerc) {
}

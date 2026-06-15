package ovh.eukon05.chargeup.model;

import java.time.LocalDateTime;

public record HourlyCleanMix(LocalDateTime from, LocalDateTime to, double cleanPerc) {
}

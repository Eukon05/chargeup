package ovh.eukon05.chargeup.model;

import java.time.LocalDateTime;

public record CleanMixInterval(LocalDateTime from, LocalDateTime to, double cleanPerc) {
}

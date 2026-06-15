package ovh.eukon05.chargeup.model;

import java.time.LocalDate;
import java.util.Map;

public record DailyMix(LocalDate date, Map<String, Double> sourceMix, double cleanPerc) {
}

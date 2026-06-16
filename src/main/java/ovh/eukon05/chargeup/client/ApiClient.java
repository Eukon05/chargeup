package ovh.eukon05.chargeup.client;

import ovh.eukon05.chargeup.model.HourlyMix;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ApiClient {
    Map<LocalDate, List<HourlyMix>> getHourlyMixes(LocalDateTime start, LocalDateTime end);
}

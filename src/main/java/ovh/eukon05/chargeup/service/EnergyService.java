package ovh.eukon05.chargeup.service;

import ovh.eukon05.chargeup.dto.OptimalChargeWindowResponseDTO;
import ovh.eukon05.chargeup.model.DailyMix;

import java.util.List;

public interface EnergyService {
    List<DailyMix> getCurrentMix();

    OptimalChargeWindowResponseDTO getOptimalChargeWindow(int windowLength);
}

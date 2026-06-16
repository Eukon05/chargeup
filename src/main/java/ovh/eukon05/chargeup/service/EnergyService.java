package ovh.eukon05.chargeup.service;

import ovh.eukon05.chargeup.dto.CurrentMixResponseDTO;
import ovh.eukon05.chargeup.dto.OptimalChargeWindowResponseDTO;

public interface EnergyService {
    CurrentMixResponseDTO getCurrentMix();

    OptimalChargeWindowResponseDTO getOptimalChargeWindow(int windowLength);
}

package ovh.eukon05.chargeup.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ovh.eukon05.chargeup.dto.CurrentMixResponseDTO;
import ovh.eukon05.chargeup.dto.OptimalChargeWindowResponseDTO;
import ovh.eukon05.chargeup.service.EnergyService;

@RestController
@RequestMapping("/api/v1/energy")
@RequiredArgsConstructor
public class EnergyController {
    private final EnergyService energyService;

    @GetMapping("/current")
    public CurrentMixResponseDTO getCurrentMix() {
        return new CurrentMixResponseDTO(energyService.getCurrentMix());
    }

    @GetMapping("/window")
    public OptimalChargeWindowResponseDTO getOptimalWindow(@RequestParam int windowLength) {
        return energyService.getOptimalChargeWindow(windowLength);
    }
}

package ovh.eukon05.chargeup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ovh.eukon05.chargeup.dto.CurrentMixResponseDTO;
import ovh.eukon05.chargeup.dto.OptimalChargeWindowResponseDTO;
import ovh.eukon05.chargeup.service.EnergyService;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/energy")
@RequiredArgsConstructor
@Validated
public class EnergyController {
    private final EnergyService energyService;

    @GetMapping("/current")
    @Operation(summary = "Calculates the average daily energy sources share in the GB mix for three days, starting today")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the current energy mix",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CurrentMixResponseDTO.class))
                    }
            ),
            @ApiResponse(responseCode = "503", description = "The external API was unavailable",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
                    }
            )
    })
    public CurrentMixResponseDTO getCurrentMix() {
        return energyService.getCurrentMix();
    }

    @GetMapping("/window")
    @Operation(summary = "Calculates a predicted window of time for two days after today, when the share of clean energy sources in the mix is the highest.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully calculated the window",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OptimalChargeWindowResponseDTO.class))
                    }),
            @ApiResponse(responseCode = "503", description = "The external API was unavailable",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
                    }
            ),
            @ApiResponse(responseCode = "400", description = "The provided windowLength parameter was out of the 1-6 range",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
                    })
    })
    public OptimalChargeWindowResponseDTO getOptimalWindow(@Parameter(description = "How long the window should be, in hours") @RequestParam @Range(min = 1, max = 6) int windowLength) {
        return energyService.getOptimalChargeWindow(windowLength);
    }
}

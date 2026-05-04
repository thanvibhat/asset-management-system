package com.assetmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class MasterDataDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LocationRequest {
        @NotBlank(message = "Location name is required")
        private String name;
        private String description;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ManufacturerRequest {
        @NotBlank(message = "Manufacturer name is required")
        private String name;
        private String website;
    }
}

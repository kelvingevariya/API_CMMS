package com.grash.dto.analytics.assets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetsCosts {
    //percent
    private long rav;
    private long totalWOCosts;
    private long totalAcquisitionCost;
}

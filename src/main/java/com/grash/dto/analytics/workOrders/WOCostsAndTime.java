package com.grash.dto.analytics.workOrders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WOCostsAndTime {
    private long total;
    private long average;
    private long additionalCost;
    private long laborCost;
    private long partCost;
    private long laborTime;
}

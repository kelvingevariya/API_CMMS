package com.grash.dto;

import com.grash.model.enums.PlanFeatures;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class SubscriptionPlanPatchDTO {

    private String name;

    private long monthlyCostPerUser;

    private long yearlyCostPerUser;

    private String code;

    private Collection<PlanFeatures> features;

}

package com.grash.dto;

import com.grash.model.Currency;
import com.grash.model.enums.BusinessType;
import com.grash.model.enums.DateFormat;
import com.grash.model.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralPreferencesPatchDTO {

    private Language language;
    private Currency currency;
    private BusinessType businessType;
    private DateFormat dateFormat;
    private String timeZone;
    private boolean autoAssignWorkOrders;
    private boolean autoAssignRequests;
    private boolean disableClosedWorkOrdersNotif;
    private boolean askFeedBackOnWOClosed;
    private boolean laborCostInTotalCost;
    private boolean woUpdateForRequesters;
    private boolean simplifiedWorkOrder;

}

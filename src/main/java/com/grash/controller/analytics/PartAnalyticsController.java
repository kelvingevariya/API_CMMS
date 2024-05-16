package com.grash.controller.analytics;

import com.grash.dto.analytics.parts.PartConsumptionsByMonth;
import com.grash.dto.analytics.parts.PartStats;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.PartConsumption;
import com.grash.service.PartConsumptionService;
import com.grash.service.UserService;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/analytics/parts")
@Api(tags = "PartAnalytics")
@RequiredArgsConstructor
public class PartAnalyticsController {

    private final UserService userService;
    private final PartConsumptionService partConsumptionService;

    @GetMapping("/consumptions/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<PartStats> getPartStats(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<PartConsumption> partConsumptions = partConsumptionService.findByCompany(user.getCompany().getId());
            long totalConsumptionCost = partConsumptions.stream().mapToLong(PartConsumption::getCost).sum();
            int consumedCount = partConsumptions.stream().mapToInt(PartConsumption::getQuantity).sum();

            return Helper.withCache(PartStats.builder()
                    .consumedCount(consumedCount)
                    .totalConsumptionCost(totalConsumptionCost)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/consumptions/month")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<List<PartConsumptionsByMonth>> getPartConsumptionsByMonth(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            List<PartConsumptionsByMonth> result = new ArrayList<>();
            LocalDate firstOfMonth =
                    LocalDate.now(ZoneId.of("UTC")).withDayOfMonth(1);
            // .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
            for (int i = 0; i < 13; i++) {
                LocalDate lastOfMonth = firstOfMonth.plusMonths(1).withDayOfMonth(1).minusDays(1);
                Collection<PartConsumption> partConsumptions = partConsumptionService.findByCreatedAtBetweenAndCompany(Helper.localDateToDate(firstOfMonth), Helper.localDateToDate(lastOfMonth), user.getCompany().getId());
                long cost = partConsumptions.stream().mapToLong(PartConsumption::getCost).sum();
                result.add(PartConsumptionsByMonth.builder()
                        .cost(cost)
                        .date(Helper.localDateToDate(firstOfMonth)).build());
                firstOfMonth = firstOfMonth.minusDays(1).withDayOfMonth(1);
            }
            Collections.reverse(result);
            return Helper.withCache(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }
}

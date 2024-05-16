package com.grash.controller.analytics;

import com.grash.dto.analytics.requests.RequestStats;
import com.grash.dto.analytics.requests.RequestStatsByPriority;
import com.grash.dto.analytics.requests.RequestsByMonth;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Request;
import com.grash.model.WorkOrder;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.service.RequestService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics/requests")
@Api(tags = "RequestAnalytics")
@RequiredArgsConstructor
public class RequestAnalyticsController {

    private final UserService userService;
    private final RequestService requestService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<RequestStats> getRequestStats(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<Request> requests = requestService.findByCompany(user.getCompany().getId());
            Collection<Request> approvedRequests = requests.stream().filter(request -> request.getWorkOrder() != null).collect(Collectors.toList());
            Collection<Request> cancelledRequests = requests.stream().filter(Request::isCancelled).collect(Collectors.toList());
            Collection<Request> pendingRequests = requests.stream().filter(request -> request.getWorkOrder() == null && !request.isCancelled()).collect(Collectors.toList());
            Collection<Request> completeRequests = approvedRequests.stream().filter(request -> request.getWorkOrder().getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
            long cycleTime = WorkOrder.getAverageAge(completeRequests.stream().map(Request::getWorkOrder).collect(Collectors.toList()));
            return Helper.withCache(RequestStats.builder()
                    .approved(approvedRequests.size())
                    .pending(pendingRequests.size())
                    .cancelled(cancelledRequests.size())
                    .cycleTime(cycleTime)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/priority")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<RequestStatsByPriority> getByPriority(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<Request> requests = requestService.findByCompany(user.getCompany().getId());

            int highCounts = getCountsByPriority(Priority.HIGH, requests);
            int noneCounts = getCountsByPriority(Priority.NONE, requests);
            int lowCounts = getCountsByPriority(Priority.LOW, requests);
            int mediumCounts = getCountsByPriority(Priority.MEDIUM, requests);

            return Helper.withCache(RequestStatsByPriority.builder()
                    .high(RequestStatsByPriority.BasicStats.builder()
                            .count(highCounts)
                            .build())
                    .none(RequestStatsByPriority.BasicStats.builder()
                            .count(noneCounts)
                            .build())
                    .low(RequestStatsByPriority.BasicStats.builder()
                            .count(lowCounts)
                            .build())
                    .medium(RequestStatsByPriority.BasicStats.builder()
                            .count(mediumCounts)
                            .build())
                    .build());

        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/cycle-time/month")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<List<RequestsByMonth>> getCycleTimeByMonth(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            List<RequestsByMonth> result = new ArrayList<>();
            LocalDate firstOfMonth =
                    LocalDate.now(ZoneId.of("UTC")).withDayOfMonth(1);
            // .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
            for (int i = 0; i < 13; i++) {
                LocalDate lastOfMonth = firstOfMonth.plusMonths(1).withDayOfMonth(1).minusDays(1);
                Collection<Request> requests = requestService.findByCreatedAtBetweenAndCompany(Helper.localDateToDate(firstOfMonth), Helper.localDateToDate(lastOfMonth), user.getCompany().getId());
                Collection<Request> completeRequests = requests.stream().filter(request -> request.getWorkOrder() != null && request.getWorkOrder().getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                long cycleTime = WorkOrder.getAverageAge(completeRequests.stream().map(Request::getWorkOrder).collect(Collectors.toList()));
                result.add(RequestsByMonth.builder()
                        .cycleTime(cycleTime)
                        .date(Helper.localDateToDate(firstOfMonth)).build());
                firstOfMonth = firstOfMonth.minusDays(1).withDayOfMonth(1);
            }
            Collections.reverse(result);
            return Helper.withCache(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    private int getCountsByPriority(Priority priority, Collection<Request> requests) {
        return (int) requests.stream().filter(request -> request.getPriority().equals(priority)).count();
    }
}

package com.grash.controller.analytics;

import com.grash.dto.analytics.workOrders.*;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.abstracts.Time;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.service.*;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics/work-orders")
@Api(tags = "WorkOrderAnalytics")
@RequiredArgsConstructor
public class WOAnalyticsController {

    private final WorkOrderService workOrderService;
    private final UserService userService;
    private final LaborService laborService;
    private final WorkOrderCategoryService workOrderCategoryService;
    private final AssetService assetService;

    @GetMapping("/complete/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<WOStats> getCompleteStats(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> workOrders = workOrderService.findByCompany(user.getCompany().getId());
            Collection<WorkOrder> completedWO = workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
            int total = workOrders.size();
            int complete = completedWO.size();
            int compliant = (int) completedWO.stream().filter(WorkOrder::isCompliant).count();
            return Helper.withCache(WOStats.builder()
                    .total(total)
                    .complete(complete)
                    .compliant(compliant)
                    .avgCycleTime(WorkOrder.getAverageAge(completedWO)).build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/mobile/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<MobileWOStats> getMobileOverview(HttpServletRequest req, @RequestParam("assignedToMe") boolean assignedToMe) {
        OwnUser user = userService.whoami(req);
        Collection<WorkOrder> workOrders;
        Collection<WorkOrder> completeWorkOrders;
        if (assignedToMe) {
            Collection<WorkOrder> result = workOrderService.findByPrimaryUser(user.getId());
            workOrders = result.stream().filter(workOrder -> !workOrder.isArchived() && !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
            completeWorkOrders = result.stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
        } else {
            Collection<WorkOrder> result = workOrderService.findByCompany(user.getCompany().getId());
            workOrders = result.stream().filter(workOrder -> !workOrder.isArchived() && !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
            completeWorkOrders = result.stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
        }
        int open = (int) workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.OPEN)).count();
        int onHold = (int) workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.ON_HOLD)).count();
        int inProgress = (int) workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.IN_PROGRESS)).count();
        int complete = completeWorkOrders.size();
        int todayWO = (int) workOrders.stream().filter(workOrder -> {
            LocalTime midnight = LocalTime.MIDNIGHT;
            LocalDate today = LocalDate.now(ZoneId.of("UTC"));
            LocalDateTime todayMidnight = LocalDateTime.of(today, midnight);
            LocalDateTime tomorrowMidnight = todayMidnight.plusDays(1);
            return workOrder.getDueDate() != null && workOrder.getDueDate().after(Helper.localDateTimeToDate(todayMidnight)) && workOrder.getDueDate().before(Helper.localDateTimeToDate(tomorrowMidnight));
        }).count();
        int high = (int) workOrders.stream().filter(workOrder -> workOrder.getPriority().equals(Priority.HIGH)).count();
        return Helper.withCache(MobileWOStats.builder()
                .open(open)
                .onHold(onHold)
                .inProgress(inProgress)
                .complete(complete)
                .today(todayWO)
                .high(high).build());
    }

    @GetMapping("/mobile/complete-compliant")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<MobileWOStatsExtended> getMobileExtendedStats(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Date weekStart = Helper.localDateToDate(LocalDate.now().minusDays(7));
        Collection<WorkOrder> workOrders = workOrderService.findByCompany(user.getCompany().getId());
        Collection<WorkOrder> completeWO = workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
        Collection<WorkOrder> compliantWO = completeWO.stream().filter(WorkOrder::isCompliant).collect(Collectors.toList());
        Collection<WorkOrder> completeWOWeek = completeWO.stream().filter(workOrder -> workOrder.getCompletedOn().before(new Date()) && workOrder.getCompletedOn().after(weekStart)).collect(Collectors.toList());
        Collection<WorkOrder> compliantWOWeek = compliantWO.stream().filter(workOrder -> {
            if (workOrder.getCompletedOn() == null) {
                return true;
            } else return workOrder.getCompletedOn().after(weekStart);
        }).collect(Collectors.toList());
        return Helper.withCache(MobileWOStatsExtended.builder()
                .complete(completeWO.size())
                .completeWeek(completeWOWeek.size())
                .compliantRate(workOrders.size() == 0 ? 1 : ((double) compliantWO.size()) / workOrders.size())
                .compliantRateWeek(completeWOWeek.size() == 0 ? 1 : ((double) compliantWOWeek.size()) / completeWOWeek.size())
                .build());
    }

    @GetMapping("/incomplete/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<WOIncompleteStats> getIncompleteStats(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> workOrders = workOrderService.findByCompany(user.getCompany().getId());
            Collection<WorkOrder> incompleteWO = workOrders.stream().filter(workOrder -> !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
            int total = incompleteWO.size();
            List<Long> ages = incompleteWO.stream().map(workOrder -> Helper.getDateDiff(workOrder.getRealCreatedAt(), new Date(), TimeUnit.DAYS)).collect(Collectors.toList());
            int averageAge = ages.size() == 0 ? 0 : ages.stream().mapToInt(Long::intValue).sum() / ages.size();
            return Helper.withCache(WOIncompleteStats.builder()
                    .total(total)
                    .averageAge(averageAge)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/incomplete/priority")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<WOStatsByPriority> getIncompleteByPriority(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> workOrders = workOrderService.findByCompany(user.getCompany().getId());
            Collection<WorkOrder> incompleteWO = workOrders.stream().filter(workOrder -> !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());

            List<Integer> highValues = getCountsAndEstimatedDurationByPriority(Priority.HIGH, incompleteWO);
            List<Integer> noneValues = getCountsAndEstimatedDurationByPriority(Priority.NONE, incompleteWO);
            List<Integer> lowValues = getCountsAndEstimatedDurationByPriority(Priority.LOW, incompleteWO);
            List<Integer> mediumValues = getCountsAndEstimatedDurationByPriority(Priority.MEDIUM, incompleteWO);

            int highCounts = highValues.get(0);
            int highEstimatedDurations = highValues.get(1);
            int mediumCounts = mediumValues.get(0);
            int mediumEstimatedDurations = mediumValues.get(1);
            int lowCounts = lowValues.get(0);
            int lowEstimatedDurations = lowValues.get(1);
            int noneCounts = noneValues.get(0);
            int noneEstimatedDurations = noneValues.get(1);

            return Helper.withCache(WOStatsByPriority.builder()
                    .high(WOStatsByPriority.BasicStats.builder()
                            .count(highCounts)
                            .estimatedHours(highEstimatedDurations)
                            .build())
                    .none(WOStatsByPriority.BasicStats.builder()
                            .count(noneCounts)
                            .estimatedHours(noneEstimatedDurations)
                            .build())
                    .low(WOStatsByPriority.BasicStats.builder()
                            .count(lowCounts)
                            .estimatedHours(lowEstimatedDurations)
                            .build())
                    .medium(WOStatsByPriority.BasicStats.builder()
                            .count(mediumCounts)
                            .estimatedHours(mediumEstimatedDurations)
                            .build())
                    .build());

        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/incomplete/statuses")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<WOStatuses> getWOStatuses(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> workOrders = workOrderService.findByCompany(user.getCompany().getId());
            Collection<WorkOrder> incompleteWO = workOrders.stream().filter(workOrder -> !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());

            return Helper.withCache(WOStatuses.builder()
                    .open(getWOCountsByStatus(Status.OPEN, incompleteWO))
                    .inProgress(getWOCountsByStatus(Status.IN_PROGRESS, incompleteWO))
                    .onHold(getWOCountsByStatus(Status.ON_HOLD, incompleteWO))
                    .complete(getWOCountsByStatus(Status.COMPLETE, incompleteWO))
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/incomplete/age/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<Collection<IncompleteWOByAsset>> getIncompleteByAsset(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<Asset> assets = assetService.findByCompany(user.getCompany().getId());
            Collection<IncompleteWOByAsset> result = new ArrayList<>();
            assets.forEach(asset -> {
                Collection<WorkOrder> incompleteWO = workOrderService.findByAsset(asset.getId())
                        .stream().filter(workOrder -> !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                List<Long> ages = incompleteWO.stream().map(workOrder -> Helper.getDateDiff(workOrder.getCreatedAt(), new Date(), TimeUnit.DAYS)).collect(Collectors.toList());
                int count = incompleteWO.size();
                result.add(IncompleteWOByAsset.builder()
                        .count(count)
                        .averageAge(count == 0 ? 0 : ages.stream().mapToLong(value -> value).sum() / count)
                        .name(asset.getName())
                        .id(asset.getId())
                        .build());
            });
            return Helper.withCache(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/incomplete/age/users")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<Collection<IncompleteWOByUser>> getIncompleteByUser(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<OwnUser> users = userService.findByCompany(user.getCompany().getId());
            Collection<IncompleteWOByUser> result = new ArrayList<>();
            users.forEach(user1 -> {
                Collection<WorkOrder> incompleteWO = workOrderService.findByPrimaryUser(user1.getId())
                        .stream().filter(workOrder -> !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                List<Long> ages = incompleteWO.stream().map(workOrder -> Helper.getDateDiff(workOrder.getCreatedAt(), new Date(), TimeUnit.DAYS)).collect(Collectors.toList());
                int count = incompleteWO.size();
                result.add(IncompleteWOByUser.builder()
                        .count(count)
                        .averageAge(count == 0 ? 0 : ages.stream().mapToLong(value -> value).sum() / count)
                        .firstName(user1.getFirstName())
                        .lastName(user1.getLastName())
                        .id(user1.getId())
                        .build());
            });
            return Helper.withCache(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/hours")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<WOHours> getHours(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> workOrders = workOrderService.findByCompany(user.getCompany().getId());
            int estimated = workOrders.stream().map(WorkOrderBase::getEstimatedDuration).mapToInt(value -> value).sum();
            Collection<Labor> labors = new ArrayList<>();
            workOrders.forEach(workOrder -> labors.addAll(laborService.findByWorkOrder(workOrder.getId())));
            int actual = labors.stream().map(Labor::getDuration).mapToInt(Math::toIntExact).sum() / 3600;
            return Helper.withCache(WOHours.builder()
                    .estimated(estimated)
                    .actual(actual)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/complete/counts/primaryUser")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<Collection<WOCountByUser>> getCountsByUser(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<OwnUser> users = userService.findByCompany(user.getCompany().getId());
            Collection<WOCountByUser> results = new ArrayList<>();
            users.forEach(user1 -> {
                int count = (int) workOrderService.findByPrimaryUser(user1.getId()).stream()
                        .filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).count();
                results.add(WOCountByUser.builder()
                        .firstName(user1.getFirstName())
                        .lastName(user1.getLastName())
                        .id(user1.getId())
                        .count(count)
                        .build());
            });
            return Helper.withCache(results);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/complete/counts/completedBy")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<Collection<WOCountByUser>> getCountsByCompletedBy(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<OwnUser> users = userService.findByCompany(user.getCompany().getId());
            Collection<WOCountByUser> results = new ArrayList<>();
            users.forEach(user1 -> {
                int count = (int) workOrderService.findByCompletedBy(user1.getId()).stream()
                        .filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).count();
                results.add(WOCountByUser.builder()
                        .firstName(user1.getFirstName())
                        .lastName(user1.getLastName())
                        .id(user1.getId())
                        .count(count)
                        .build());
            });
            return Helper.withCache(results);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/complete/counts/priority")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<Map<Priority, Integer>> getCountsByPriority(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Priority[] priorities = Priority.values();
            Map<Priority, Integer> results = new HashMap<>();
            Arrays.asList(priorities).forEach(priority -> {
                int count = (int) workOrderService.findByPriorityAndCompany(priority, user.getCompany().getId()).stream()
                        .filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).count();
                results.put(priority, count);
            });
            return Helper.withCache(results);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/complete/counts/category")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<Collection<WOCountByCategory>> getCountsByCategory(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<WorkOrderCategory> categories = workOrderCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            Collection<WOCountByCategory> results = new ArrayList<>();
            categories.forEach(category -> {
                int count = (int) workOrderService.findByCategory(category.getId()).stream()
                        .filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).count();
                results.add(WOCountByCategory.builder()
                        .name(category.getName())
                        .id(category.getId())
                        .count(count)
                        .build());
            });
            return Helper.withCache(results);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/complete/counts/week")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<List<WOCountByWeek>> getCompleteByWeek(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            List<WOCountByWeek> result = new ArrayList<>();
            LocalDate previousMonday =
                    LocalDate.now(ZoneId.of("UTC"));
            // .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
            for (int i = 0; i < 5; i++) {
                Collection<WorkOrder> completeWorkOrders = workOrderService.findByCompletedOnBetweenAndCompany(Helper.localDateToDate(previousMonday.minusDays(7)), Helper.localDateToDate(previousMonday), user.getCompany().getId())
                        .stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                int compliant = (int) completeWorkOrders.stream().filter(WorkOrder::isCompliant).count();
                int reactive = (int) completeWorkOrders.stream().filter(WorkOrder::isReactive).count();
                result.add(WOCountByWeek.builder()
                        .count(completeWorkOrders.size())
                        .compliant(compliant)
                        .reactive(reactive)
                        .date(Helper.localDateToDate(previousMonday)).build());
                previousMonday = previousMonday.minusDays(7);
            }
            Collections.reverse(result);
            return Helper.withCache(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/complete/time/week")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<List<WOTimeByWeek>> getCompleteTimeByWeek(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            List<WOTimeByWeek> result = new ArrayList<>();
            LocalDate previousMonday =
                    LocalDate.now(ZoneId.of("UTC"));
            // .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
            for (int i = 0; i < 5; i++) {
                Collection<WorkOrder> completeWorkOrders = workOrderService.findByCompletedOnBetweenAndCompany(Helper.localDateToDate(previousMonday.minusDays(7)), Helper.localDateToDate(previousMonday), user.getCompany().getId())
                        .stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                Collection<WorkOrder> reactiveWorkOrders = completeWorkOrders.stream().filter(WorkOrder::isReactive).collect(Collectors.toList());

                long total = getTime(completeWorkOrders);
                long reactive = getTime(reactiveWorkOrders);
                result.add(WOTimeByWeek.builder()
                        .total(total)
                        .reactive(reactive)
                        .date(Helper.localDateToDate(previousMonday)).build());
                previousMonday = previousMonday.minusDays(7);
            }
            Collections.reverse(result);
            return Helper.withCache(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/complete/costs-time")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<WOCostsAndTime> getCompleteCostsAndTime(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> completeWorkOrders = workOrderService.findByCompany(user.getCompany().getId()).stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
            long additionalCost = workOrderService.getAdditionalCost(completeWorkOrders);
            long laborCost = workOrderService.getLaborCostAndTime(completeWorkOrders).getFirst();
            long laborTime = workOrderService.getLaborCostAndTime(completeWorkOrders).getSecond();
            long partCost = workOrderService.getPartCost(completeWorkOrders);
            long total = laborCost + partCost + additionalCost;

            return Helper.withCache(WOCostsAndTime.builder()
                    .total(total)
                    .average(completeWorkOrders.size() == 0 ? 0 : total / completeWorkOrders.size())
                    .additionalCost(additionalCost)
                    .laborCost(laborCost)
                    .partCost(partCost)
                    .laborTime(laborTime)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/complete/costs/month")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<List<WOCostsByMonth>> getCompleteCostsByMonth(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.canSeeAnalytics()) {
            List<WOCostsByMonth> result = new ArrayList<>();
            LocalDate firstOfMonth =
                    LocalDate.now(ZoneId.of("UTC")).withDayOfMonth(1);
            // .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
            for (int i = 0; i < 13; i++) {
                LocalDate lastOfMonth = firstOfMonth.plusMonths(1).withDayOfMonth(1).minusDays(1);
                Collection<WorkOrder> completeWorkOrders = workOrderService.findByCompletedOnBetweenAndCompany(Helper.localDateToDate(firstOfMonth), Helper.localDateToDate(lastOfMonth), user.getCompany().getId())
                        .stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                result.add(WOCostsByMonth.builder()
                        .additionalCost(workOrderService.getAdditionalCost(completeWorkOrders))
                        .laborCost(workOrderService.getLaborCostAndTime(completeWorkOrders).getFirst())
                        .partCost(workOrderService.getPartCost(completeWorkOrders))
                        .date(Helper.localDateToDate(firstOfMonth)).build());
                firstOfMonth = firstOfMonth.minusDays(1).withDayOfMonth(1);
            }
            Collections.reverse(result);
            return Helper.withCache(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    private List<Integer> getCountsAndEstimatedDurationByPriority(Priority priority, Collection<WorkOrder> workOrders) {
        Collection<WorkOrder> priorityWO = workOrders.stream().filter(workOrder -> workOrder.getPriority().equals(priority)).collect(Collectors.toList());
        int priorityCounts = priorityWO.size();
        int priorityEstimatedDurations = priorityWO.stream().map(WorkOrderBase::getEstimatedDuration).mapToInt(value -> value).sum();
        return Arrays.asList(priorityCounts, priorityEstimatedDurations);
    }

    private int getWOCountsByStatus(Status status, Collection<WorkOrder> workOrders) {
        Collection<WorkOrder> statusWO = workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(status)).collect(Collectors.toList());
        return statusWO.size();
    }

    private long getTime(Collection<WorkOrder> workOrders) {
        Collection<Labor> labors = new ArrayList<>();
        workOrders.forEach(workOrder -> {
            labors.addAll(laborService.findByWorkOrder(workOrder.getId()));
        });
        return labors.stream().mapToLong(Time::getDuration).sum();
    }
}

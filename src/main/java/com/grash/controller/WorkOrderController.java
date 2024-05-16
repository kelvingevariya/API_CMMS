package com.grash.controller;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.DateRange;
import com.grash.dto.SuccessResponse;
import com.grash.dto.WorkOrderPatchDTO;
import com.grash.dto.WorkOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.service.*;
import com.grash.utils.Helper;
import com.grash.utils.MultipartFileImpl;
import com.itextpdf.html2pdf.HtmlConverter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.persistence.criteria.JoinType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@RestController
@RequestMapping("/work-orders")
@Api(tags = "workOrder")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final WorkOrderMapper workOrderMapper;
    private final UserService userService;
    private final MessageSource messageSource;
    private final AssetService assetService;
    private final LocationService locationService;
    private final LaborService laborService;
    private final PartService partService;
    private final PartQuantityService partQuantityService;
    private final NotificationService notificationService;
    private final EmailService2 emailService2;
    private final TeamService teamService;
    private final TaskService taskService;
    private final RelationService relationService;
    private final AdditionalCostService additionalCostService;
    private final WorkOrderHistoryService workOrderHistoryService;
    private final SpringTemplateEngine thymeleafTemplateEngine;
    private final GCPService gcp;
    private final WorkflowService workflowService;


    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<WorkOrderShowDTO>> search(@RequestBody SearchCriteria searchCriteria, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            searchCriteria.filterCompany(user);
            if (user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS)) {
                boolean canViewOthers = user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS);
                if (!canViewOthers) {
                    searchCriteria.getFilterFields().add(FilterField.builder()
                            .field("createdBy")
                            .value(user.getId())
                            .operation("eq")
                            .values(new ArrayList<>())
                            .alternatives(Arrays.asList(
                                    FilterField.builder()
                                            .field("assignedTo")
                                            .operation("inm")
                                            .joinType(JoinType.LEFT)
                                            .value("")
                                            .values(Collections.singletonList(user.getId())).build(),
                                    FilterField.builder()
                                            .field("primaryUser")
                                            .operation("eq")
                                            .value(user.getId())
                                            .values(Collections.singletonList(user.getId())).build(),
                                    FilterField.builder()
                                            .field("team")
                                            .operation("in")
                                            .value("")
                                            .values(teamService.findByUser(user.getId()).stream().map(Team::getId).collect(Collectors.toList())).build()
                            )).build());
                }
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(workOrderService.findBySearchCriteria(searchCriteria));
    }

    @PostMapping("/events")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrderCategory not found")})
    public Collection<WorkOrderShowDTO> getByMonth(@Valid @RequestBody DateRange
                                                           dateRange, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS)) {
                //TODO Add preventive Maintenances
                return workOrderService.findByDueDateBetweenAndCompany(dateRange.getStart(), dateRange.getEnd(), user.getCompany().getId()).stream().filter(workOrder -> {
                    boolean canViewOthers = user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS);
                    return canViewOthers || workOrder.getCreatedBy().equals(user.getId()) || workOrder.isAssignedTo(user);
                }).map(workOrderMapper::toShowDto).collect(Collectors.toList());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else return workOrderService.getAll().stream().map(workOrderMapper::toShowDto).collect(Collectors.toList());
    }

    @GetMapping("/asset/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrder not found")})
    public Collection<WorkOrderShowDTO> getByAsset(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Asset> optionalAsset = assetService.findById(id);
        if (optionalAsset.isPresent()) {
            return workOrderService.findByAsset(id).stream().map(workOrderMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/location/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrder not found")})
    public Collection<WorkOrderShowDTO> getByLocation(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Location> optionalLocation = locationService.findById(id);
        if (optionalLocation.isPresent()) {
            return workOrderService.findByLocation(id).stream().map(workOrderMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrder not found")})
    public WorkOrderShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS) || savedWorkOrder.getCreatedBy().equals(user.getId()) || savedWorkOrder.isAssignedTo(user))) {
                return workOrderMapper.toShowDto(savedWorkOrder);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public WorkOrderShowDTO create(@ApiParam("WorkOrder") @Valid @RequestBody WorkOrder
                                           workOrderReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                && (workOrderReq.getSignature() == null ||
                user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.SIGNATURE))) {
            if (user.getCompany().getCompanySettings().getGeneralPreferences().isAutoAssignWorkOrders()) {
                OwnUser primaryUser = workOrderReq.getPrimaryUser();
                workOrderReq.setPrimaryUser(primaryUser == null ? user : primaryUser);
            }
            WorkOrder createdWorkOrder = workOrderService.create(workOrderReq);
            if (createdWorkOrder.getAsset() != null) {
                Asset asset = assetService.findById(createdWorkOrder.getAsset().getId()).get();
                if (asset.getStatus().equals(AssetStatus.OPERATIONAL)) {
                    assetService.triggerDownTime(asset.getId(), Helper.getLocale(user));
                }
            }
            workOrderService.notify(createdWorkOrder, Helper.getLocale(user));
            Collection<Workflow> workflows = workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_CREATED, user.getCompany().getId());
            workflows.forEach(workflow -> workflowService.runWorkOrder(workflow, createdWorkOrder));
            return workOrderMapper.toShowDto(createdWorkOrder);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/part/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrders for this part not found")})
    public Collection<WorkOrderShowDTO> getByPart(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Part> optionalPart = partService.findById(id);
        if (optionalPart.isPresent()) {
            Collection<PartQuantity> partQuantities = partQuantityService.findByPart(id).stream()
                    .filter(partQuantity -> partQuantity.getWorkOrder() != null).collect(Collectors.toList());
            Collection<WorkOrder> workOrders = partQuantities.stream().map(PartQuantity::getWorkOrder).collect(Collectors.toList());
            Collection<WorkOrder> uniqueWorkOrders = workOrders.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(WorkOrder::getId))),
                    ArrayList::new));
            return uniqueWorkOrders.stream().map(workOrderMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "WorkOrder not found")})
    public WorkOrderShowDTO patch(@ApiParam("WorkOrder") @Valid @RequestBody WorkOrderPatchDTO
                                          workOrder, @ApiParam("id") @PathVariable("id") Long id,
                                  HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (workOrder.getStatus() != Status.COMPLETE) {
            workOrder.setCompletedOn(null);
            workOrder.setCompletedBy(null);
        }
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            Status savedWorkOrderStatusBefore = savedWorkOrder.getStatus();
            if (savedWorkOrder.canBeEditedBy(user)
                    && (workOrder.getSignature() == null ||
                    user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.SIGNATURE))) {
                if (!workOrder.getStatus().equals(Status.IN_PROGRESS)) {
                    if (workOrder.getStatus().equals(Status.COMPLETE)) {
                        workOrder.setCompletedBy(user);
                        workOrder.setCompletedOn(new Date());
                        if (workOrder.getAsset() != null) {
                            Asset asset = workOrder.getAsset();
                            Collection<WorkOrder> workOrdersOfSameAsset = workOrderService.findByAsset(asset.getId());
                            if (workOrdersOfSameAsset.stream().noneMatch(workOrder1 -> !workOrder1.getId().equals(id) && !workOrder1.getStatus().equals(Status.COMPLETE))) {
                                assetService.stopDownTime(asset.getId(), Helper.getLocale(user));
                            }
                        }
                        Collection<Labor> primaryLabors = laborService.findByWorkOrder(id).stream().filter(Labor::isLogged).collect(Collectors.toList());
                        primaryLabors.forEach(laborService::stop);
                    }
                    Collection<Labor> labors = laborService.findByWorkOrder(id);
                    Collection<Labor> primaryTimes = labors.stream().filter(Labor::isLogged).collect(Collectors.toList());
                    primaryTimes.forEach(laborService::stop);
                }

                WorkOrder patchedWorkOrder = workOrderService.update(id, workOrder, user);

                if (patchedWorkOrder.getStatus().equals(Status.COMPLETE) && !savedWorkOrder.getStatus().equals(Status.COMPLETE)) {
                    Collection<Workflow> workflows = workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_CLOSED, user.getCompany().getId());
                    workflows.forEach(workflow -> workflowService.runWorkOrder(workflow, patchedWorkOrder));
                }
                if (patchedWorkOrder.isArchived() && !savedWorkOrder.isArchived()) {
                    Collection<Workflow> workflows = workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_ARCHIVED, user.getCompany().getId());
                    workflows.forEach(workflow -> workflowService.runWorkOrder(workflow, patchedWorkOrder));
                }
                if (user.getCompany().getCompanySettings().getGeneralPreferences().isWoUpdateForRequesters()
                        && savedWorkOrderStatusBefore != patchedWorkOrder.getStatus()
                        && patchedWorkOrder.getParentRequest() != null) {
                    Long requesterId = patchedWorkOrder.getParentRequest().getCreatedBy();
                    OwnUser requester = userService.findById(requesterId).get();
                    Locale locale = Helper.getLocale(user);
                    String message = messageSource.getMessage("notification_wo_request", new Object[]{patchedWorkOrder.getTitle(), messageSource.getMessage(patchedWorkOrder.getStatus().toString(), null, locale)}, locale);
                    notificationService.create(new Notification(message, userService.findById(requesterId).get(), NotificationType.WORK_ORDER, id));
                    if (requester.getUserSettings().isEmailUpdatesForRequests()) {
                        Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                            put("workOrderLink", frontendUrl + "/app/work-orders/" + id);
                            put("message", message);
                        }};
                        emailService2.sendMessageUsingThymeleafTemplate(new String[]{requester.getEmail()}, messageSource.getMessage("request_update", null, locale), mailVariables, "requester-update.html", Helper.getLocale(user));
                    }
                }
                boolean shouldNotify = !user.getCompany().getCompanySettings().getGeneralPreferences().isDisableClosedWorkOrdersNotif() || !patchedWorkOrder.getStatus().equals(Status.COMPLETE);
                if (shouldNotify)
                    workOrderService.patchNotify(savedWorkOrder, patchedWorkOrder, Helper.getLocale(user));
                return workOrderMapper.toShowDto(patchedWorkOrder);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("WorkOrder not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "WorkOrder not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (
                    user.getId().equals(savedWorkOrder.getCreatedBy()) ||
                            user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.WORK_ORDERS)) {
                workOrderService.delete(id);
                return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("WorkOrder not found", HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "/report/{id}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<?> getPDF(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req, HttpServletResponse response) throws IOException {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS) || savedWorkOrder.getCreatedBy().equals(user.getId()) || savedWorkOrder.isAssignedTo(user))) {
                Context thymeleafContext = new Context();
                thymeleafContext.setLocale(Helper.getLocale(user));
                Optional<OwnUser> creator = savedWorkOrder.getCreatedBy() == null ? Optional.empty() : userService.findById(savedWorkOrder.getCreatedBy());
                List<Pair<String, String>> tasks = taskService.findByWorkOrder(id).stream().map(task -> Pair.of(task.getTaskBase().getLabel(), translateTaskValue(task.getValue(), Helper.getLocale(user)))).collect(Collectors.toList());
                Collection<PartQuantity> partQuantities = partQuantityService.findByWorkOrder(id);
                Collection<Labor> labors = laborService.findByWorkOrder(id);
                Collection<Relation> relations = relationService.findByWorkOrder(id);
                Collection<AdditionalCost> additionalCosts = additionalCostService.findByWorkOrder(id);
                Collection<WorkOrderHistory> workOrderHistories = workOrderHistoryService.findByWorkOrder(id);
                Map<String, Object> variables = new HashMap<String, Object>() {{
                    put("companyName", user.getCompany().getName());
                    put("companyPhone", user.getCompany().getPhone());
                    put("currency", user.getCompany().getCompanySettings().getGeneralPreferences().getCurrency().getCode());
                    put("assignedTo", Helper.enumerate(savedWorkOrder.getAssignedTo().stream().map(OwnUser::getFullName).collect(Collectors.toList())));
                    put("customers", Helper.enumerate(savedWorkOrder.getCustomers().stream().map(Customer::getName).collect(Collectors.toList())));
                    put("workOrder", savedWorkOrder);
                    put("primaryUserName", savedWorkOrder.getPrimaryUser() == null ? null : savedWorkOrder.getPrimaryUser().getFullName());
                    put("createdBy", creator.<Object>map(OwnUser::getFullName).orElse(null));
                    put("tasks", tasks);
                    put("labors", labors);
                    put("relations", relations);
                    put("additionalCosts", additionalCosts);
                    put("workOrderHistories", workOrderHistories);
                    put("partQuantities", partQuantities);
                }};
                thymeleafContext.setVariables(variables);

                String reportHtml = thymeleafTemplateEngine.process("work-order-report.html", thymeleafContext);

                /* Setup Source and target I/O streams */
                ByteArrayOutputStream target = new ByteArrayOutputStream();
                /* Call convert method */
                HtmlConverter.convertToPdf(reportHtml, target);
                /* extract output as bytes */
                byte[] bytes = target.toByteArray();
                MultipartFile file = new MultipartFileImpl(bytes, "Work Order Report.pdf");
                return ResponseEntity.ok()
                        .body(new SuccessResponse(true, gcp.upload(file, "reports/" + user.getCompany().getId())));
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);

    }

    private String translateTaskValue(String value, Locale locale) {
        List<String> taskOptions = Arrays.asList("OPEN", "ON_HOLD", "IN_PROGRESS", "COMPLETE", "PASS", "FLAG", "FAIL");
        if (taskOptions.contains(value)) {
            return messageSource.getMessage(value, null, locale);
        } else return value;
    }
}

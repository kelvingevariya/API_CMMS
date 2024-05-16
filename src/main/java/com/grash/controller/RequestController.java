package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.RequestPatchDTO;
import com.grash.dto.RequestShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.dto.WorkOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RequestMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.Notification;
import com.grash.model.OwnUser;
import com.grash.model.Request;
import com.grash.model.Workflow;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.service.NotificationService;
import com.grash.service.RequestService;
import com.grash.service.UserService;
import com.grash.service.WorkflowService;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
@Api(tags = "request")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;
    private final UserService userService;
    private final WorkOrderMapper workOrderMapper;
    private final RequestMapper requestMapper;
    private final NotificationService notificationService;
    private final MessageSource messageSource;
    private final WorkflowService workflowService;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<RequestShowDTO>> search(@RequestBody SearchCriteria searchCriteria, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS)) {
                searchCriteria.filterCompany(user);
                boolean canViewOthers = user.getRole().getViewOtherPermissions().contains(PermissionEntity.REQUESTS);
                if (!canViewOthers) {
                    searchCriteria.filterCreatedBy(user);
                }
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(requestService.findBySearchCriteria(searchCriteria));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Request not found")})
    public RequestShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Request> optionalRequest = requestService.findById(id);
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.REQUESTS) || savedRequest.getCreatedBy().equals(user.getId()))) {
                return requestMapper.toShowDto(savedRequest);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public RequestShowDTO create(@ApiParam("Request") @Valid @RequestBody Request requestReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.REQUESTS)) {
            Request createdRequest = requestService.create(requestReq);
            String title = "new_request";
            String message = messageSource.getMessage("notification_new_request", null, Helper.getLocale(user));
            notificationService.createMultiple(userService.findByCompany(user.getCompany().getId()).stream()
                    .filter(user1 -> user1.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS))
                    .map(user1 -> new Notification(message, user1, NotificationType.REQUEST, createdRequest.getId())).collect(Collectors.toList()), true, title);
            Collection<Workflow> workflows = workflowService.findByMainConditionAndCompany(WFMainCondition.REQUEST_CREATED, user.getCompany().getId());
            workflows.forEach(workflow -> workflowService.runRequest(workflow, createdRequest));
            return requestMapper.toShowDto(createdRequest);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Request not found")})
    public RequestShowDTO patch(@ApiParam("Request") @Valid @RequestBody RequestPatchDTO request, @ApiParam("id") @PathVariable("id") Long id,
                                HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Request> optionalRequest = requestService.findById(id);

        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.getWorkOrder() != null) {
                throw new CustomException("Can't patch an approved request", HttpStatus.NOT_ACCEPTABLE);
            }
            if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.REQUESTS) || savedRequest.getCreatedBy().equals(user.getId())) {
                Request patchedRequest = requestService.update(id, request);
                return requestMapper.toShowDto(patchedRequest);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Request not found")})
    public WorkOrderShowDTO approve(@ApiParam("id") @PathVariable("id") Long id,
                                    HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Request> optionalRequest = requestService.findById(id);

        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.getWorkOrder() != null) {
                throw new CustomException("Request is already approved", HttpStatus.NOT_ACCEPTABLE);
            }
            Collection<Workflow> workflows = workflowService.findByMainConditionAndCompany(WFMainCondition.REQUEST_APPROVED, user.getCompany().getId());
            workflows.forEach(workflow -> workflowService.runRequest(workflow, savedRequest));
            return workOrderMapper.toShowDto(requestService.createWorkOrderFromRequest(savedRequest, user));
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Request not found")})
    public RequestShowDTO cancel(@ApiParam("id") @PathVariable("id") Long id,
                                 HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Request> optionalRequest = requestService.findById(id);

        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.getWorkOrder() != null) {
                throw new CustomException("Request is already approved", HttpStatus.NOT_ACCEPTABLE);
            }
            savedRequest.setCancelled(true);
            Collection<Workflow> workflows = workflowService.findByMainConditionAndCompany(WFMainCondition.REQUEST_REJECTED, user.getCompany().getId());
            workflows.forEach(workflow -> workflowService.runRequest(workflow, savedRequest));
            return requestMapper.toShowDto(requestService.save(savedRequest));
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Request not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Request> optionalRequest = requestService.findById(id);
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.getCreatedBy().equals(user.getId()) ||
                    user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.REQUESTS)) {
                requestService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

}

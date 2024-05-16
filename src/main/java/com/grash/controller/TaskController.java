package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.TaskBaseDTO;
import com.grash.dto.TaskPatchDTO;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.TaskType;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/tasks")
@Api(tags = "task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserService userService;
    private final TaskBaseService taskBaseService;
    private final WorkOrderService workOrderService;
    private final WorkflowService workflowService;
    private final PreventiveMaintenanceService preventiveMaintenanceService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Task not found")})
    public Task getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        Optional<Task> optionalTask = taskService.findById(id);
        if (optionalTask.isPresent()) {
            Task savedTask = optionalTask.get();
            return savedTask;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Task not found")})
    public Collection<Task> getByWorkOrder(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            return taskService.findByWorkOrder(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/preventive-maintenance/{id}")
    @PreAuthorize("permitAll()")
    public Collection<Task> getByPreventiveMaintenance(@ApiParam("id") @PathVariable("id") Long id) {
        Optional<PreventiveMaintenance> optionalPreventiveMaintenance = preventiveMaintenanceService.findById(id);
        if (optionalPreventiveMaintenance.isPresent()) {
            return taskService.findByPreventiveMaintenance(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/preventive-maintenance/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<Task> createByPreventiveMaintenance(@ApiParam("Task") @Valid @RequestBody Collection<TaskBaseDTO> taskBasesReq, @ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<PreventiveMaintenance> optionalPreventiveMaintenance = preventiveMaintenanceService.findById(id);
        if (optionalPreventiveMaintenance.isPresent() && optionalPreventiveMaintenance.get().canBeEditedBy(user)) {
            taskService.findByPreventiveMaintenance(id).forEach(task -> taskService.delete(task.getId()));
            Collection<TaskBase> taskBases = taskBasesReq.stream().map(taskBaseDTO ->
                    taskBaseService.createFromTaskBaseDTO(taskBaseDTO, user.getCompany())).collect(Collectors.toList());
            return taskBases.stream().map(taskBase -> {
                StringBuilder value = new StringBuilder();
                if (taskBase.getTaskType().equals(TaskType.SUBTASK)) {
                    value.append("OPEN");
                } else if (taskBase.getTaskType().equals(TaskType.INSPECTION)) {
                    value.append("FLAG");
                }
                Task task = new Task(taskBase, null, optionalPreventiveMaintenance.get(), value.toString());
                return taskService.create(task);
            }).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/work-order/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public Collection<Task> create(@ApiParam("Task") @Valid @RequestBody Collection<TaskBaseDTO> taskBasesReq, @ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent() && optionalWorkOrder.get().canBeEditedBy(user)) {
            taskService.findByWorkOrder(id).forEach(task -> taskService.delete(task.getId()));
            Collection<TaskBase> taskBases = taskBasesReq.stream().map(taskBaseDTO ->
                    taskBaseService.createFromTaskBaseDTO(taskBaseDTO, user.getCompany())).collect(Collectors.toList());
            return taskBases.stream().map(taskBase -> {
                StringBuilder value = new StringBuilder();
                if (taskBase.getTaskType().equals(TaskType.SUBTASK)) {
                    value.append("OPEN");
                } else if (taskBase.getTaskType().equals(TaskType.INSPECTION)) {
                    value.append("FLAG");
                }
                Task task = new Task(taskBase, optionalWorkOrder.get(), null, value.toString());
                return taskService.create(task);
            }).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Task not found")})
    public Task patch(@ApiParam("Task") @Valid @RequestBody TaskPatchDTO task, @ApiParam("id") @PathVariable("id") Long id,
                      HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Task> optionalTask = taskService.findById(id);

        if (optionalTask.isPresent()) {
            Task patchedTask = taskService.update(id, task);
            Collection<Workflow> workflows = workflowService.findByMainConditionAndCompany(WFMainCondition.TASK_UPDATED, user.getCompany().getId());
            workflows.forEach(workflow -> workflowService.runTask(workflow, patchedTask));
            return patchedTask;
        } else throw new CustomException("Task not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Task not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Task> optionalTask = taskService.findById(id);
        if (optionalTask.isPresent()) {
            taskService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Task not found", HttpStatus.NOT_FOUND);
    }

}

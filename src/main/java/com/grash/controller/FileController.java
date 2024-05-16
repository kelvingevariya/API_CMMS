package com.grash.controller;


import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.File;
import com.grash.model.OwnUser;
import com.grash.model.Task;
import com.grash.model.enums.FileType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.FileService;
import com.grash.service.GCPService;
import com.grash.service.TaskService;
import com.grash.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;


@RestController
@Api(tags = "file")
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final GCPService gcp;
    private final FileService fileService;
    private final UserService userService;
    private final TaskService taskService;

    @PostMapping(value = "/upload", produces = "application/json")
    public Collection<File> handleFileUpload(@RequestParam("files") MultipartFile[] filesReq, @RequestParam("folder") String folder, @RequestParam("hidden") String hidden, HttpServletRequest req, @RequestParam("type") FileType fileType,
                                             @RequestParam(value = "taskId", required = false) Integer taskId) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.FILES) &&
                user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.FILE)) {
            Collection<File> result = new ArrayList<>();
            Arrays.asList(filesReq).forEach(fileReq -> {
                String url = gcp.upload(fileReq, folder);
                Task task = null;
                if (taskId != null) {
                    Optional<Task> optionalTask = taskService.findById(taskId.longValue());
                    if (optionalTask.isPresent()) {
                        task = optionalTask.get();
                    }
                }
                result.add(fileService.create(new File(fileReq.getOriginalFilename(), url, fileType, task, hidden.equals("true"))));
            });
            return result;
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<File>> search(@RequestBody SearchCriteria searchCriteria, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.FILES)) {
                searchCriteria.filterCompany(user);
                boolean canViewOthers = user.getRole().getViewOtherPermissions().contains(PermissionEntity.FILES);
                if (!canViewOthers) {
                    searchCriteria.filterCreatedBy(user);
                }
                searchCriteria.getFilterFields().add(FilterField.builder()
                        .field("hidden")
                        .value(false)
                        .operation("eq")
                        .values(new ArrayList<>())
                        .alternatives(new ArrayList<>()).build());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(fileService.findBySearchCriteria(searchCriteria));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "File not found")})
    public File getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<File> optionalFile = fileService.findById(id);
        if (optionalFile.isPresent()) {
            File savedFile = optionalFile.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.FILES) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.FILES) || savedFile.getCreatedBy().equals(user.getId()))) {
                return savedFile;
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "File not found")})
    public File patch(@ApiParam("File") @Valid @RequestBody File file, @ApiParam("id") @PathVariable("id") Long id,
                      HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<File> optionalFile = fileService.findById(id);

        if (optionalFile.isPresent()) {
            File savedFile = optionalFile.get();
            if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.FILES) || savedFile.getCreatedBy().equals(user.getId())) {
                return fileService.update(file);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("File not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "File not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<File> optionalFile = fileService.findById(id);
        if (optionalFile.isPresent()) {
            File savedFile = optionalFile.get();
            if (user.getId().equals(savedFile.getCreatedBy())
                    || user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.FILES)) {
                fileService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("File not found", HttpStatus.NOT_FOUND);
    }

}

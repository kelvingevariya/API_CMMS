package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.*;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.security.CurrentUser;
import com.grash.service.RoleService;
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
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@Api(tags = "user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;
    private final UserMapper userMapper;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<OwnUser>> search(@RequestBody SearchCriteria searchCriteria, @ApiIgnore @CurrentUser OwnUser user) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                searchCriteria.filterCompany(user);
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(userService.findBySearchCriteria(searchCriteria));
    }

    @PostMapping("/invite")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "TeamCategory not found")})
    public SuccessResponse invite(@RequestBody UserInvitationDTO invitation, @ApiIgnore @CurrentUser OwnUser user) {
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
            int companyUsersCount = userService.findByCompany(user.getCompany().getId()).size();
            Optional<Role> optionalRole = roleService.findById(invitation.getRole().getId());
            if (optionalRole.isPresent() && user.getCompany().getCompanySettings().getId().equals(optionalRole.get().getCompanySettings().getId())) {
                if (companyUsersCount + invitation.getEmails().size() <= user.getCompany().getSubscription().getUsersCount() || !optionalRole.get().isPaid()) {
                    invitation.getEmails().forEach(email ->
                            userService.invite(email, optionalRole.get(), user)
                    );
                    return new SuccessResponse(true, "Users have been invited");
                } else
                    throw new CustomException("Your current subscription doesn't allow you to invite that many users", HttpStatus.NOT_ACCEPTABLE);

            } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AssetCategory not found")})
    public Collection<UserMiniDTO> getMini(@ApiIgnore @CurrentUser OwnUser user) {
        return userService.findByCompany(user.getCompany().getId()).stream().filter(OwnUser::isEnabledInSubscription).map(userMapper::toMiniDto).collect(Collectors.toList());
    }

    @GetMapping("/mini/disabled")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AssetCategory not found")})
    public Collection<UserMiniDTO> getMiniDisabled(@ApiIgnore @CurrentUser OwnUser user) {
        return userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> !user1.isEnabledInSubscription()).map(userMapper::toMiniDto).collect(Collectors.toList());
    }


    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "User not found")})
    public UserResponseDTO patch(@ApiParam("User") @Valid @RequestBody UserPatchDTO userReq,
                                 @ApiParam("id") @PathVariable("id") Long id,
                                 @ApiIgnore @CurrentUser OwnUser requester) {
        Optional<OwnUser> optionalUser = userService.findById(id);

        if (optionalUser.isPresent()) {
            OwnUser savedUser = optionalUser.get();
            if (requester.getId().equals(savedUser.getId()) ||
                    requester.getRole().getEditOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                return userMapper.toPatchDto(userService.update(id, userReq));
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("Can't get someone else's user", HttpStatus.NOT_ACCEPTABLE);
        }

    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "User not found")})
    public UserResponseDTO getById(@ApiParam("id") @PathVariable("id") Long id, @ApiIgnore @CurrentUser OwnUser user) {
        Optional<OwnUser> optionalUser = userService.findById(id);
        if (optionalUser.isPresent()) {
            OwnUser savedUser = optionalUser.get();
            if (user.getCompany().getId().equals(savedUser.getCompany().getId())) {
                return userMapper.toPatchDto(savedUser);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "User not found")})
    public UserResponseDTO patchRole(@ApiParam("id") @PathVariable("id") Long id,
                                     @RequestParam("role") Long roleId,
                                     @ApiIgnore @CurrentUser OwnUser requester) {
        Optional<OwnUser> optionalUserToPatch = userService.findById(id);
        Optional<Role> optionalRole = roleService.findById(roleId);

        if (optionalUserToPatch.isPresent() && optionalRole.isPresent()) {
            OwnUser userToPatch = optionalUserToPatch.get();
            if (requester.getRole().getEditOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                int usersCount = (int) userService.findByCompany(requester.getCompany().getId()).stream().filter(OwnUser::isEnabledInSubscriptionAndPaid).count();
                if (usersCount <= requester.getCompany().getSubscription().getUsersCount()) {
                    userToPatch.setRole(optionalRole.get());
                    return userMapper.toPatchDto(userService.save(userToPatch));
                } else
                    throw new CustomException("Company subscription users count doesn't allow this operation", HttpStatus.NOT_ACCEPTABLE);
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("User or role not found", HttpStatus.NOT_FOUND);
        }

    }
}

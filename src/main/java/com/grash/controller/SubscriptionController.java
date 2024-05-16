package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.service.SubscriptionService;
import com.grash.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subscriptions")
@Api(tags = "subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "SubscriptionCategory not found")})
    public Collection<Subscription> getAll(HttpServletRequest req) {
        return subscriptionService.getAll();
    }


    //    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ROLE_CLIENT')")
//    @ApiResponses(value = {//
//            @ApiResponse(code = 500, message = "Something went wrong"), //
//            @ApiResponse(code = 403, message = "Access denied"), //
//            @ApiResponse(code = 404, message = "Subscription not found")})
//    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
//        OwnUser user = userService.whoami(req);
//
//        Optional<Subscription> optionalSubscription = subscriptionService.findById(id);
//        if (optionalSubscription.isPresent()) {
//            Subscription savedSubscription = optionalSubscription.get();
//            if (subscriptionService.hasAccess(user, savedSubscription)) {
//                subscriptionService.delete(id);
//                return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
//                        HttpStatus.OK);
//            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
//        } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
//    }
    @PostMapping("/upgrade")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse upgrade(@RequestBody Collection<Long> usersIds,
                                   HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.isOwnsCompany()) {
            int enabledUsersCount = (int) userService.findByCompany(user.getCompany().getId()).stream().filter(OwnUser::isEnabledInSubscription).count();
            Subscription subscription = user.getCompany().getSubscription();
            int subscriptionUsersCount = subscription.getUsersCount();
            if (enabledUsersCount + usersIds.size() <= subscriptionUsersCount) {
                Collection<OwnUser> users = usersIds.stream().map(userId -> userService.findByIdAndCompany(userId, user.getCompany().getId()).get()).collect(Collectors.toList());
                if (users.stream().noneMatch(OwnUser::isEnabledInSubscription)) {
                    users.forEach(user1 -> user1.setEnabledInSubscription(true));
                    userService.saveAll(users);
                    subscription.setUpgradeNeeded(false);
                    subscriptionService.save(subscription);
                    return new SuccessResponse(true, "Users enabled successfully");
                } else throw new CustomException("There are some already enabled users", HttpStatus.NOT_ACCEPTABLE);
            } else
                throw new CustomException("The subscription users count doesn't permit this operation", HttpStatus.NOT_ACCEPTABLE);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/downgrade")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse downgrade(@RequestBody Collection<Long> usersIds,
                                     HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.isOwnsCompany()) {
            int enabledUsersCount = (int) userService.findByCompany(user.getCompany().getId()).stream().filter(OwnUser::isEnabledInSubscription).count();
            Subscription subscription = user.getCompany().getSubscription();
            int subscriptionUsersCount = user.getCompany().getSubscription().getUsersCount();
            if (enabledUsersCount - usersIds.size() <= subscriptionUsersCount) {
                Collection<OwnUser> users = usersIds.stream().map(userId ->
                                userService.findByIdAndCompany(userId, user.getCompany().getId()).get())
                        .filter(user1 -> !user1.isOwnsCompany()).collect(Collectors.toList());
                if (users.stream().allMatch(OwnUser::isEnabledInSubscription)) {
                    users.forEach(user1 -> user1.setEnabledInSubscription(false));
                    userService.saveAll(users);
                    subscription.setDowngradeNeeded(false);
                    subscriptionService.save(subscription);
                    return new SuccessResponse(true, "Users enabled successfully");
                } else throw new CustomException("There are some already disabled users", HttpStatus.NOT_ACCEPTABLE);
            } else
                throw new CustomException("The subscription users count doesn't permit this operation", HttpStatus.NOT_ACCEPTABLE);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }
}

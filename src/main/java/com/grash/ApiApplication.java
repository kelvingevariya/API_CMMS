package com.grash;

import com.grash.model.*;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

@SpringBootApplication
@RequiredArgsConstructor
public class ApiApplication implements CommandLineRunner {

    @Value("${superAdmin.role.name}")
    private String superAdminRole;

    private final RoleService roleService;
    private final CompanyService companyService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionService subscriptionService;
    private final ScheduleService scheduleService;

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    @Override
    public void run(String... args) {
        if (!roleService.findByName(superAdminRole).isPresent()) {
            Company company = companyService.create(new Company());
            roleService.create(Role.builder().
                    name(superAdminRole)
                    .companySettings(company.getCompanySettings())
                    .code(RoleCode.ADMIN)
                    .roleType(RoleType.ROLE_SUPER_ADMIN)
                    .build());
        }
        if (!subscriptionPlanService.existByCode("FREE")) {
            subscriptionPlanService.create(SubscriptionPlan.builder()
                    .code("FREE")
                    .name("Free")
                    .monthlyCostPerUser(0)
                    .yearlyCostPerUser(0).build());
        }
        if (!subscriptionPlanService.existByCode("STARTER")) {
            subscriptionPlanService.create(SubscriptionPlan.builder()
                    .code("STARTER")
                    .name("Starter").features(new HashSet<>(Arrays.asList(PlanFeatures.PREVENTIVE_MAINTENANCE,
                            PlanFeatures.CHECKLIST,
                            PlanFeatures.FILE,
                            PlanFeatures.METER,
                            PlanFeatures.ADDITIONAL_COST,
                            PlanFeatures.ADDITIONAL_TIME)))
                    .monthlyCostPerUser(20)
                    .yearlyCostPerUser(200).build());
        }
        if (!subscriptionPlanService.existByCode("PROFESSIONAL")) {
            subscriptionPlanService.create(SubscriptionPlan.builder()
                    .code("PROFESSIONAL")
                    .name("Professional")
                    .monthlyCostPerUser(40)
                    .features(new HashSet<>(Arrays.asList(PlanFeatures.PREVENTIVE_MAINTENANCE,
                            PlanFeatures.CHECKLIST,
                            PlanFeatures.FILE,
                            PlanFeatures.METER,
                            PlanFeatures.ADDITIONAL_COST,
                            PlanFeatures.ADDITIONAL_TIME,
                            PlanFeatures.REQUEST_CONFIGURATION,
                            PlanFeatures.PURCHASE_ORDER,
                            PlanFeatures.SIGNATURE,
                            PlanFeatures.ANALYTICS,
                            PlanFeatures.IMPORT_CSV
                    )))
                    .yearlyCostPerUser(400).build());
        }
        if (!subscriptionPlanService.existByCode("BUSINESS")) {
            subscriptionPlanService.create(SubscriptionPlan.builder()
                    .code("BUSINESS")
                    .name("Business")
                    .monthlyCostPerUser(80)
                    .features(new HashSet<>(Arrays.asList(PlanFeatures.values())))
                    .yearlyCostPerUser(800).build());
        }
        Collection<Schedule> schedules = scheduleService.getAll();
        schedules.forEach(scheduleService::scheduleWorkOrder);
        Collection<Subscription> subscriptions = subscriptionService.getAll();
        subscriptions.forEach(subscriptionService::scheduleEnd);
    }
}

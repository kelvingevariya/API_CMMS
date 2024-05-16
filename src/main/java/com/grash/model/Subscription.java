package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.Audit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Subscription extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private int usersCount;

    private boolean monthly;

    private boolean cancelled;

    private boolean activated;

    @JsonIgnore
    private String fastSpringId;

    @ManyToOne
    @NotNull
    private SubscriptionPlan subscriptionPlan;

    private Date startsOn;

    private Date endsOn;

    private boolean downgradeNeeded;

    private boolean upgradeNeeded;

}

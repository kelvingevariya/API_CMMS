package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.Audit;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
public class Schedule extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private boolean disabled;

    @NotNull
    private Date startsOn = new Date();

    @NotNull
    private int frequency = 1;

    private Date endsOn;

    private Integer dueDateDelay;

    @OneToOne
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PreventiveMaintenance preventiveMaintenance;

    public Schedule(PreventiveMaintenance preventiveMaintenance) {
        this.preventiveMaintenance = preventiveMaintenance;
    }

}

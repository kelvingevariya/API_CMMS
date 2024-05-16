package com.grash.model;

import com.grash.model.abstracts.Audit;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "companySettings", callSuper = false)
public class Company extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private String address;
    private String phone;
    private String website;
    private String email;

    private int employeesCount;

    @OneToOne
    private File logo;

    private String city;

    private String state;

    private String zipCode;

    @OneToOne
    private Subscription subscription;

    @OneToOne(cascade = CascadeType.ALL)
    private CompanySettings companySettings = new CompanySettings(this);

    public Company(String companyName, int employeesCount, Subscription subscription) {
        this.name = companyName;
        this.employeesCount = employeesCount;
        this.subscription = subscription;

    }
}

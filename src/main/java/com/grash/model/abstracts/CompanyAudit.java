package com.grash.model.abstracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.enums.RoleType;
import com.grash.security.CustomUserDetail;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.persistence.*;

@MappedSuperclass
@Data
public class CompanyAudit extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(nullable = false)
    private Company company;

    @PrePersist
    public void beforePersist() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return;
        OwnUser user = ((CustomUserDetail) authentication.getPrincipal()).getUser();
        Company company = user.getCompany();
        this.setCompany(company);
    }


    @PostLoad
    public void afterLoad() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            return;
        Object principal = authentication.getPrincipal();
        OwnUser user = ((CustomUserDetail) principal).getUser();
        Company company = user.getCompany();
        // check if not authorized
        if (!user.getRole().getRoleType().equals(RoleType.ROLE_SUPER_ADMIN) && !company.getId().equals(this.getCompany().getId())) {
            throw new CustomException("afterLoad:  the user (username=" + user.getUsername() + ")  is not authorized to load  this object (" + this.getClass() + ") with id" + this.id, HttpStatus.FORBIDDEN);
        }
    }

}

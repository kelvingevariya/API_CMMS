package com.grash.repository;

import com.grash.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;

public interface RequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {
    Collection<Request> findByCompany_Id(@Param("x") Long id);

    Collection<Request> findByCreatedAtBetweenAndCompany_Id(Date date1, Date date2, Long id);
}

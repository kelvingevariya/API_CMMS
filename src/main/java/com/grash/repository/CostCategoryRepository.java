package com.grash.repository;

import com.grash.model.CostCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface CostCategoryRepository extends JpaRepository<CostCategory, Long> {

    Collection<CostCategory> findByCompanySettings_Id(Long id);

    Optional<CostCategory> findByNameAndCompanySettings_Id(String name, Long id);


}

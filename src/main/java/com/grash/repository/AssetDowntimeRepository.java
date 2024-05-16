package com.grash.repository;

import com.grash.model.AssetDowntime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Date;

public interface AssetDowntimeRepository extends JpaRepository<AssetDowntime, Long> {

    Collection<AssetDowntime> findByAsset_Id(Long id);

    Collection<AssetDowntime> findByCompany_Id(Long id);

    Collection<AssetDowntime> findByStartsOnBetweenAndCompany_Id(Date date1, Date date2, Long id);
}

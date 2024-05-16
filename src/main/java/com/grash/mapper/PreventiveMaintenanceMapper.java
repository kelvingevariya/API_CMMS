package com.grash.mapper;

import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenancePostDTO;
import com.grash.dto.PreventiveMaintenanceShowDTO;
import com.grash.model.PreventiveMaintenance;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface PreventiveMaintenanceMapper {
    PreventiveMaintenance updatePreventiveMaintenance(@MappingTarget PreventiveMaintenance entity, PreventiveMaintenancePatchDTO dto);

    @Mappings({})
    PreventiveMaintenancePatchDTO toPatchDto(PreventiveMaintenance model);

    PreventiveMaintenanceShowDTO toShowDto(PreventiveMaintenance model);

    PreventiveMaintenance toModel(PreventiveMaintenancePostDTO dto);
}

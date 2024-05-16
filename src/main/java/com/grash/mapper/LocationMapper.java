package com.grash.mapper;

import com.grash.dto.LocationMiniDTO;
import com.grash.dto.LocationPatchDTO;
import com.grash.dto.LocationShowDTO;
import com.grash.model.Location;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class, VendorMapper.class, UserMapper.class, TeamMapper.class})
public interface LocationMapper {
    Location updateLocation(@MappingTarget Location entity, LocationPatchDTO dto);

    @Mappings({})
    LocationPatchDTO toPatchDto(Location model);

    LocationShowDTO toShowDto(Location model);

    LocationMiniDTO toMiniDto(Location model);
}

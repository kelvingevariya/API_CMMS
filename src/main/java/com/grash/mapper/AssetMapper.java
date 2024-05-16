package com.grash.mapper;

import com.grash.dto.AssetMiniDTO;
import com.grash.dto.AssetPatchDTO;
import com.grash.dto.AssetShowDTO;
import com.grash.model.Asset;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class, VendorMapper.class, UserMapper.class, TeamMapper.class, FileMapper.class, PartMapper.class})
public interface AssetMapper {
    Asset updateAsset(@MappingTarget Asset entity, AssetPatchDTO dto);

    @Mappings({})
    AssetPatchDTO toPatchDto(Asset model);

    AssetShowDTO toShowDto(Asset model);

    AssetMiniDTO toMiniDto(Asset model);
}

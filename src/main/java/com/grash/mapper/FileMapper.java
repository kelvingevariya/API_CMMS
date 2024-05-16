package com.grash.mapper;

import com.grash.dto.FileMiniDTO;
import com.grash.model.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface FileMapper {

    @Mappings({})
    FileMiniDTO toMiniDto(File model);
}

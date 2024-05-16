package com.grash.mapper;

import com.grash.dto.TaskPatchDTO;
import com.grash.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    Task updateTask(@MappingTarget Task entity, TaskPatchDTO dto);

    @Mappings({})
    TaskPatchDTO toPatchDto(Task model);
}

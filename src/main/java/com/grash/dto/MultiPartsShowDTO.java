package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class MultiPartsShowDTO extends AuditShowDTO {

    private Long id;

    private String name;

    private Collection<PartMiniDTO> parts;
}

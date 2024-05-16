package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PartMiniDTO {
    private Long id;
    private String name;
    private String description;
    private long cost;
}

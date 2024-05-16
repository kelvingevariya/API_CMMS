package com.grash.dto;

import com.grash.model.Company;
import com.grash.model.File;
import com.grash.model.Location;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class LocationShowDTO extends AuditShowDTO {
    private Company company;

    private Long id;

    private String name;

    private String address;

    private Long longitude;

    private Long latitude;

    private boolean hasChildren;

    private List<TeamMiniDTO> teams = new ArrayList<>();

    private Location parentLocation;

    private List<VendorMiniDTO> vendors = new ArrayList<>();

    private List<CustomerMiniDTO> customers = new ArrayList<>();

    private List<UserMiniDTO> workers = new ArrayList<>();

    private File image;

    private List<FileMiniDTO> files;

}

package com.grash.dto;

import com.grash.model.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
public class LocationPatchDTO {
    private String name;

    private String address;

    private Long longitude;

    private Long latitude;

    private Location parentLocation;

    private Collection<OwnUser> workers;

    private Collection<Team> teams;

    private Collection<Vendor> vendors;

    private Collection<Customer> customers;

    private File image;

    private List<File> files = new ArrayList<>();

}

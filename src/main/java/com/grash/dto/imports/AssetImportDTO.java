package com.grash.dto.imports;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class AssetImportDTO {

    private Long id;
    private String archived;

    private String locationName;

    private String parentAssetName;

    private String area;

    private String barCode;

    private String category;

    private String name;

    private String primaryUserEmail;


    private Double warrantyExpirationDate;

    private String additionalInfos;

    private String serialNumber;

    private Collection<String> assignedToEmails;

    private Collection<String> teamsNames;

    private String status;

    private Long acquisitionCost;

    private Collection<String> customersNames;
    
    private Collection<String> vendorsNames;
}

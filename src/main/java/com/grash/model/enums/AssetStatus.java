package com.grash.model.enums;

import java.util.Arrays;
import java.util.List;

public enum AssetStatus {
    OPERATIONAL(Arrays.asList("Operational", "Opérationnel")),
    DOWN(Arrays.asList("Down", "En panne"));
    
    private final List<String> strings;

    AssetStatus(List<String> strings) {
        this.strings = strings;
    }

    public static AssetStatus getAssetStatusFromString(String string) {
        for (AssetStatus assetStatus : AssetStatus.values()) {
            if (assetStatus.strings.stream().anyMatch(str -> str.equalsIgnoreCase(string))) {
                return assetStatus;
            }
        }
        return OPERATIONAL;
    }
}

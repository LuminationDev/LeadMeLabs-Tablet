package com.lumination.leadmelabs.models.applications.information;

import java.util.HashMap;

/**
 * A class to aggregate the different Information types, include Steam, Revive, Embedded, Custom, etc..
 */
public class InformationConstants {
    private static final HashMap<String, Information> dataMap = new HashMap<>();

    public static Information getValue(String id) {
        return dataMap.get(id);
    }

    static {
        // Add entries from Steam
        dataMap.putAll(InformationSteam.getDataMap());

        // Add entries from the Embedded applications
        dataMap.putAll(InformationEmbedded.getDataMap());
    }
}

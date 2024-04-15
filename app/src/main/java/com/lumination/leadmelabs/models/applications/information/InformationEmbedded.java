package com.lumination.leadmelabs.models.applications.information;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * A class to hold information related to Steam applications
 */
public class InformationEmbedded {
    private static final HashMap<String, Information> dataMap = new HashMap<>();

    public static HashMap<String, Information> getDataMap() {
        return dataMap;
    }

    //Add each of the experience's details below
    static {
        // Video Player
        Information videoPlayer = new Information(
                "Play video files on a Station, with full playback control from the tablet.",
                new ArrayList<>(Collections.singletonList(TagConstants.DEFAULT)),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("321914152547262211351528", videoPlayer);
    }
}

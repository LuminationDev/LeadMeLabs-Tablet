package com.lumination.leadmelabs.utilities;

import android.util.Log;

import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class dedicated to determining if extra actions need to occur when a primary Station with
 * nested stations is interacted with.
 * NOTE: This is currently only applicable with the Snowy Hydro project. A Station with no nested
 * stations is not affected in any way.
 */
public class Interlinking {
    private static final ArrayList<String> multiLaunchApplications =
            new ArrayList<>(Collections.singletonList("snowy hydro"));

    /**
     * Check if the experience that is about to be launch is supposed to be linked with another Station,
     * such as the Snowy Hydro experience where it launches on the floor and wall computers at the same
     * time.
     * @param experienceName A string of the experience that is about to launch.
     * @return A boolean of if multiple Stations will launch this title.
     */
    public static boolean multiLaunch(String experienceName) {
        return multiLaunchApplications.contains(experienceName.toLowerCase());
    }

    /**
     * For use with multiple primary stations
     * Combines the selected station ids with their nested station ids.
     *
     * @param selectedIds           The array of selected station ids.
     * @param experienceName        The name of the experience that will be launched.
     * @return A string containing the original station ids along with their nested station ids.
     */
    public static String combineStationIdsWithNested(int[] selectedIds, String experienceName) {
        return Arrays.stream(selectedIds)
                .mapToObj(id -> {
                    Station station = StationsFragment.mViewModel.getStationById(id);
                    performAssociatedExperienceActions(station, experienceName);
                    //If multiLaunch is true collect the nested Id's if false just return the primary
                    return multiLaunch(experienceName) ? collectNestedStations(station, int[].class) : new int[]{id};
                })
                .flatMapToInt(Arrays::stream)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    /**
     * Collects nested integer arrays from selectedIds based on the provided experience name.
     * Each element of the selectedIds array is mapped to a Station object using the ViewModel.
     * If the experience allows multi-launch, nested stations are collected recursively.
     *
     * @param selectedIds An array of integer IDs representing selected stations.
     * @param experienceName The name of the experience determining the collection behavior.
     * @return An array of integers representing collected nested station IDs.
     */
    public static int[] collectNestedIntArray(int[] selectedIds, String experienceName) {
        return Arrays.stream(selectedIds)
                .mapToObj(StationsFragment.mViewModel::getStationById)
                .flatMapToInt(station -> {
                    int[] ids = new int[]{station.id};
                    if (Interlinking.multiLaunch(experienceName)) {
                        ids = Interlinking.collectNestedStations(station, int[].class);
                    }
                    return Arrays.stream(ids);
                })
                .toArray();
    }

    /**
     * For use with a single primary station
     * Combines the selected station with their nested station ids.
     *
     * @param station               The currently selected Station object.
     * @param experienceName        The name of the experience that will be launched.
     * @return A string containing the original station ids along with their nested station ids.
     */
    public static String joinStations(Station station, int id, String experienceName) {
        String joinedStations = String.valueOf(id);
        performAssociatedExperienceActions(station, experienceName);

        if (multiLaunch(experienceName)) {
            joinedStations = collectNestedStations(station, String.class);
        }

        return joinedStations;
    }

    /**
     * Collects the IDs of the provided station and its nested stations into either a comma-separated string or an integer array,
     * based on the specified return type.
     *
     * @param station The main station whose ID and nested station IDs are to be collected.
     * @param returnType The class object representing the desired return type: String.class for a comma-separated string or int[].class for an integer array.
     * @return If returnType is String.class, returns a string containing the IDs of the main station and its nested stations, separated by commas.
     *         If returnType is int[].class, returns an integer array containing the IDs of the main station and its nested stations.
     * @throws IllegalArgumentException if returnType is neither String.class nor int[].class.
     */
    public static <T> T collectNestedStations(Station station, Class<T> returnType) {
        List<Integer> stations = new ArrayList<>();
        stations.add(station.getId());

        if (station.nestedStations != null) {
            stations.addAll(station.nestedStations);
        }

        if (returnType == String.class) {
            return returnType.cast(stations.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
        } else if (returnType == int[].class) {
            return returnType.cast(stations.stream()
                    .mapToInt(Integer::intValue)
                    .toArray());
        } else {
            throw new IllegalArgumentException("Unsupported return type: " + returnType.getSimpleName());
        }
    }

    /**
     * If switching the Nova Star layout check if there are associated actions that are required to
     * run on any nested stations.
     * Example: Opening the ring video or logo video for the floor Station.
     */
    public static void performAssociatedLayoutActions(Station station, String layoutOption) {
        if (station == null || Helpers.isNullOrEmpty(layoutOption)) return;

        //TODO these names are placeholders for the time being
        String action = "";
        switch (layoutOption.toLowerCase()) {
            case "fullscreen":
                action = "Fullscreen action";
                break;

            case "presentation":
                action = "Presentation action";
                break;

            case "vr":
                action = "VR action";
                break;

            default:
                return;
        }

        // Perform the action on each Nested Station
        for (int id : station.nestedStations) {
            Station nestedStation = StationsFragment.mViewModel.getStationById(id);

            Log.e("ASSOCIATED ACTION", "Station: " +  nestedStation.getId() + ", Action: " + action);
        }
    }

    /**
     * If launching an experience on a primary Station check if there are associated actions that
     * are required to run on any nested stations.
     * Example: Launching the custom rings video when launching the custom (Lumination) snowy hydro project
     */
    private static void performAssociatedExperienceActions(Station station, String experienceName) {
        if (station == null || Helpers.isNullOrEmpty(experienceName)) return;

        //TODO these names are placeholders for the time being
        String action = "";
        switch (experienceName) {
            case "snowy factory":
                action = "Snowy Factory action";
                break;

            case "example":
                action = "Example action";
                break;

            default:
                return;
        }

        // Perform the action on each Nested Station
        for (int id : station.nestedStations) {
            Station nestedStation = StationsFragment.mViewModel.getStationById(id);

            Log.e("ASSOCIATED ACTION", "Station: " +  nestedStation.getId() + ", Action: " + action);
        }
    }
}

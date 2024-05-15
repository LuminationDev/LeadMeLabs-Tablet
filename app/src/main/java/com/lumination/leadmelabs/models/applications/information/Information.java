package com.lumination.leadmelabs.models.applications.information;

import java.util.ArrayList;

/**
 * A class dedicated to holding information about a specific application. This includes the application
 * description, associated tags and the year levels it is applicable to.
 */
public class Information {
    private final String description;
    private final ArrayList<String> tags;
    private final ArrayList<String> subTags;
    private final ArrayList<String> hiddenKeywords;
    private final ArrayList<Integer> ages;

    public Information(String description, ArrayList<String> tags, ArrayList<String> subTags, ArrayList<String> hiddenKeywords, ArrayList<Integer> ages) {
        this.description = description;
        this.tags = tags;
        this.subTags = subTags;
        this.hiddenKeywords = hiddenKeywords;
        this.ages = ages;
    }

    public String getDescription() {
        return this.description;
    }

    public ArrayList<String> getTags() {
        return this.tags;
    }

    public ArrayList<String> getHiddenKeywords() {
        return this.hiddenKeywords;
    }

    public ArrayList<String> getSubTags() {
        return this.subTags;
    }

    /**
     * Transform the ages into a readable year level string i.e. 'All', '5-10', '8' or 'N/A' if the
     * age range is not applicable
     * @return A String of a readable year level(s)
     */
    public String getYearLevels(String version) {
        if (version.equals("australia")) {
            return getAustralianYearLevels();
        }

        return "N/A";
    }

    /**
     * AUSTRALIAN VERSION
     */
    private String getAustralianYearLevels() {
        int minAge = ages.stream().min(Integer::compare).orElse(0);
        int maxAge = ages.stream().max(Integer::compare).orElse(0);

        if (minAge <= 5 || maxAge >= 18) {
            return "N/A";
        }

        int minYearLevel = Math.max(1, minAge - 5);
        int maxYearLevel = Math.min(12, maxAge - 5);

        if (minYearLevel == 1 && maxYearLevel == 12) {
            return "All";
        }

        return minYearLevel + "-" + maxYearLevel;
    }
}

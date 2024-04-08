package com.lumination.leadmelabs.models.applications.information;

import java.util.ArrayList;

/**
 * A class dedicated to holding information about a specific application. This includes the application
 * description, associated tags and the year levels it is applicable to.
 */
public class Information {
    private String description;
    private ArrayList<String> tags;
    private ArrayList<String> subTags;
    private ArrayList<String> ages;

    public Information(String description, ArrayList<String> tags, ArrayList<String> subTags, ArrayList<String> ages) {
        this.description = description;
        this.tags = tags;
        this.subTags = subTags;
        this.ages = ages;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public ArrayList<String> getTags() {
        return this.tags;
    }

    public void setSubTags(ArrayList<String> subTags) {
        this.subTags = subTags;
    }

    public ArrayList<String> getSubTags() {
        return this.subTags;
    }

    public void setAges(ArrayList<String> ages) {
        this.ages = ages;
    }

    /**
     * Transform the ages into a readable year level string i.e. 'All', '5-10', '8'
     * @return A String of a readable year level(s)
     */
    public String getAges() {
        //TODO finish this transformation into year levels
        return "";
    }
}

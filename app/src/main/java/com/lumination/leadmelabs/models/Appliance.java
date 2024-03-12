package com.lumination.leadmelabs.models;

import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.utilities.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Turn into generic abstract class in the future for all the different appliances
 */
public class Appliance {
    public String type;
    public String name;
    public String room;
    public String id;
    public String value; //used for appliances
    public String displayType;
    public JSONArray stations;
    public ArrayList<Option> options;

    public ArrayList<String> description;
    public MutableLiveData<Integer> icon;
    public MutableLiveData<String> status;

    public class Option {
        public String id;
        public String name;
        public Option(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public Appliance(String type, String name, String room, String id) {
        this.type = type;
        this.name = name;
        this.room = room;
        this.id = id;

        setDescription(type);

        icon = new MutableLiveData<>(null);
        status = new MutableLiveData<>(Constants.INACTIVE);
    }

    public boolean matchesDisplayCategory(String type)
    {
        if (this.displayType != null) {
            return this.displayType.equals(type);
        }
        return this.type.equals(type);
    }

    public void setStations(JSONArray stations) {
        this.stations = stations;
    }

    public void setOptions(JSONArray options) throws JSONException {
        int length = options.length();
        this.options = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            JSONObject option = options.getJSONObject(i);
            this.options.add(new Option(option.getString("id"), option.getString("name")));
        }
    }

    /**
     * Using a supplied Id, find the index of an option.
     * @param id A string of the Id to search for.
     * @return An int of the index or -1 if it is not found.
     */
    public int getIndexByOptionId(String id) {
        if (this.options == null) {
            return -1;
        }

        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            if (option.id == null) {
                return -1;
            }

            if (option.id.equals(id)) {
                return i; // Return the index number if the id matches
            }
        }
        return -1; // Return -1 if the id is not found in any Option object
    }

    /**
     * Based on the value provided set the source status to the current value. There is a current
     * maximum of three options for a source card, the index of the option in the options array
     * determines what status the card should show for each value.
     * @param sourceId A string of the source Id
     */
    public void setSourceStatus(String sourceId) {
        if (!type.equals("sources")) return;

        int index = getIndexByOptionId(sourceId);
        //Default to the first index
        String newStatus = Constants.ACTIVE;

        switch (index) {
            case 0:
                newStatus = Constants.ACTIVE;
                break;

            case 1:
                newStatus = Constants.INACTIVE;
                break;

            case 2:
                newStatus = Constants.STOPPED;
                break;
        }

        status = new MutableLiveData<>(newStatus);
    }

    public String getLabelForIndex(int index) {
        if (this.options != null && this.options.size() > (index - 1)) {
            return this.options.get(index).name;
        } else if (this.description.size() > (index - 1)) {
            return this.description.get(index);
        }
        return "";
    }

    /**
     * Set the custom on and off descriptions for the appliance type.
     */
    private void setDescription(String type) {
        //Special circumstance for the blind scene card
        if(name.contains(Constants.BLIND_SCENE_SUBTYPE)) {
            this.description = new ArrayList<>(Arrays.asList("OPEN", "CLOSE", "STOP"));
            return;
        }

        switch(type) {
            case "blinds":
                this.description = new ArrayList<>(Arrays.asList("OPEN", "CLOSE", "STOP"));
                break;

            case "splicers":
                this.description = new ArrayList<>(Arrays.asList("STRETCH", "MIRROR"));
                break;

            case "sources":
                this.description = new ArrayList<>(Arrays.asList("HDMI 1", "HDMI 2", "HDMI 3"));
                break;

            case "scenes":
                this.description = new ArrayList<>(Arrays.asList("ACTIVE", "INACTIVE"));
                break;

            default:
                this.description = new ArrayList<>(Arrays.asList("ON", "OFF"));
                break;
        }
    }
}

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

    public String getLabelForIndex(int index) {
        if (this.options != null) {
            return this.options.get(index).name;
        } else {
            return this.description.get(index);
        }
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

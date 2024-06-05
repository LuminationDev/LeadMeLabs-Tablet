package com.lumination.leadmelabs.models.applications;

import com.lumination.leadmelabs.models.applications.details.Details;
import com.lumination.leadmelabs.models.applications.information.Information;

import org.json.JSONObject;

import java.util.Objects;

public class Application {
    public String type;
    public String name;
    public String id;
    public boolean isVr;
    public JSONObject subtype;
    public Details details;
    public Information information;

    public Application(String type, String name, String id, boolean isVr, JSONObject subtype) {
        this.type = type;
        this.name = name;
        this.id = id;
        this.isVr = isVr;
        this.subtype = subtype;
        this.information = new Information(null, null, null, null, null, null);
    }

    //region Settings/Getters
    public String getType() {
        return this.type;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setInformation(Information information) {
        this.information = information;
    }

    public Information getInformation() {
        return this.information;
    }
    //endregion

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        Application other = (Application) obj;
        return Objects.equals(id, other.id);
    }

    public String HasCategory() {
        if (subtype == null) {
            return "none";
        }

        return subtype.optString("category", "none");
    }
}

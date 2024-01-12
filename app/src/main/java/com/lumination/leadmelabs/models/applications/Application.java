package com.lumination.leadmelabs.models.applications;

import com.lumination.leadmelabs.models.applications.details.Details;

import java.util.Objects;

public class Application {
    public String type;
    public String name;
    public String id;
    public boolean isVr;
    public Details details;

    public Application(String type, String name, String id, boolean isVr) {
        this.type = type;
        this.name = name;
        this.id = id;
        this.isVr = isVr;
    }

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
}

package com.lumination.leadmelabs.qa;

import com.google.gson.Gson;

public class QaDetail {
    public String value = null;
    public String message = null;
    public String id;

    public QaDetail(String id) {
        this.id = id;
    }

    public QaDetail(String id, String value) {
        this.id = id;
        this.value = value;
    }

    public QaDetail(String id, String value, String message) {
        this.id = id;
        this.value = value;
        this.message = message;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}

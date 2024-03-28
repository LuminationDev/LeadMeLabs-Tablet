package com.lumination.leadmelabs.qa;

import org.json.JSONException;
import org.json.JSONObject;

public class QaCheck {
    public String passedStatus = null;
    public String message = null;
    public String id;

    public QaCheck(String id) {
        this.id = id;
    }

    public void setFailed(String message)
    {
        this.passedStatus = "failed";
        this.message = message;
    }

    public void setPassed()
    {
        this.passedStatus = "passed";
    }

    public void setPassed(String message)
    {
        this.setPassed();
        this.message = message;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("_id", id);
            jsonObject.put("_passedStatus", passedStatus);
            jsonObject.put("_message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}

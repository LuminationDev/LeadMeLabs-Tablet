package com.lumination.leadmelabs.ui.appliance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ApplianceViewModel extends ViewModel {
    public MutableLiveData<List<String>> activeAppliances;
    private MutableLiveData<List<Appliance>> appliances;

    public LiveData<List<Appliance>> getAppliances() {
        if (appliances == null) {
            appliances = new MutableLiveData<>();
            loadAppliances();
        }

        return appliances;
    }

    public LiveData<List<String>> getActiveAppliances() {
        if (activeAppliances == null) {
            activeAppliances = new MutableLiveData<>();
            loadActiveAppliances();
        }

        return activeAppliances;
    }

    public void setAppliances(JSONArray applianceList) throws JSONException {
        List<Appliance> st = new ArrayList<>();

        //Iterator over the outer loop - different appliance types
        for (int i = 0; i < applianceList.length(); i++) {
            String type = applianceList.getJSONObject(i).getString("name");

            JSONArray currentObjectList = applianceList.getJSONObject(i).getJSONArray("objects");

            if(currentObjectList.length() > 0) {
                //Iterator over the child objects
                for(int x = 0; x < currentObjectList.length(); x++) {
                    JSONObject current = currentObjectList.getJSONObject(x);
                    Appliance appliance = new Appliance(type, current.getString("name"), current.getInt("id"));
                    st.add(appliance);
                }
            }
        }

        appliances.setValue(st);
    }

    //Do not load in user_parameters as their data field is a string and not an object.
    public void setActiveAppliances(JSONArray appliances) throws JSONException {
        ArrayList<String> active = new ArrayList<>();

        for (int i = 0; i < appliances.length(); i++) {
            if(appliances.getJSONObject(i).getString("is_user_parameter").equals("false")) {
                if (appliances.getJSONObject(i).getJSONObject("data").has("level")) {
                    if (!appliances.getJSONObject(i).getJSONObject("data").getString("level").equals("0")) {
                        active.add(appliances.getJSONObject(i).getString("id"));
                    }
                }
            }
        }

        activeAppliances.setValue(active);
    }

    public void loadActiveAppliances() {
        NetworkService.sendMessage("NUC", "Automation", "Get:appliances");
    }

    private void loadAppliances() {
        NetworkService.sendMessage("NUC", "Appliances", "List");
    }
}

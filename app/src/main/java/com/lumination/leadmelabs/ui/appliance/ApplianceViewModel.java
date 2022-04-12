package com.lumination.leadmelabs.ui.appliance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ApplianceViewModel extends ViewModel {
    //regular arraylist to hold the currently selected
    public ArrayList<String> activeAppliances = new ArrayList<>();

    private MutableLiveData<List<Appliance>> appliances;

    public LiveData<List<Appliance>> getAppliances() {
        if (appliances == null) {
            appliances = new MutableLiveData<>();
            loadAllAppliances();

            //Update this later
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            loadAppliances();
        }

        return appliances;
    }

    public void setAppliances(JSONArray appliances) throws JSONException {
        List<Appliance> st = new ArrayList<>();

        for (int i = 0; i < appliances.length(); i++) {
            Appliance appliance = new Appliance(appliances.getJSONObject(i).getString("name"), appliances.getJSONObject(i).getInt("id"));
            st.add(appliance);
        }

        this.setAppliances(st);
    }

    public void setAppliances(List<Appliance> appliances) {
        this.appliances.setValue(appliances);
    }

    private void loadAppliances() {
        NetworkService.sendMessage("NUC","Appliances", "List");
    }

    /**
     * Loading the current value for the selected scene from a cold start, grabbing the current
     * value from the CBUS system.
     */
    public void loadAllAppliances() {
        NetworkService.sendMessage("NUC", "Automation", "Get:lighting");
    }

    public void setActiveAppliances(JSONArray appliances) throws JSONException {
        ArrayList<String> st = new ArrayList<>();

        for (int i = 0; i < appliances.length(); i++) {
//            Appliance appliance = new Appliance(appliances.getJSONObject(i).getString("id"), appliances.getJSONObject(i).getInt("id"));

            if(appliances.getJSONObject(i).getJSONObject("data").has("level")) {
                if (!appliances.getJSONObject(i).getJSONObject("data").getString("level").equals("0")) {
                    st.add(appliances.getJSONObject(i).getString("id"));
                }
            }
        }

        activeAppliances = st;
    }
}
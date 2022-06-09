package com.lumination.leadmelabs.ui.appliance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.room.RoomFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ApplianceViewModel extends ViewModel {
    public static HashSet<String> activeApplianceList = new HashSet<>();
    public static HashSet<Appliance> activeSceneList = new HashSet<>();
    public MutableLiveData<HashSet<String>> activeAppliances;
    private MutableLiveData<List<Appliance>> appliances;

    public LiveData<List<Appliance>> getAppliances() {
        if (appliances == null) {
            appliances = new MutableLiveData<>();
            loadAppliances();
        }

        return appliances;
    }

    public LiveData<HashSet<String>> getActiveAppliances() {
        if (activeAppliances == null) {
            activeAppliances = new MutableLiveData<>();
            loadActiveAppliances();
        }

        return activeAppliances;
    }

    public void setAppliances(JSONArray applianceList) throws JSONException {
        List<Appliance> st = new ArrayList<>();
        HashSet<String> rooms = new HashSet<>();

        //Iterator over the outer loop - different appliance types
        for (int i = 0; i < applianceList.length(); i++) {
            String type = applianceList.getJSONObject(i).getString("name");

            JSONArray currentObjectList = applianceList.getJSONObject(i).getJSONArray("objects");

            if(currentObjectList.length() > 0) {
                //Iterator over the child objects
                for(int x = 0; x < currentObjectList.length(); x++) {
                    JSONObject current = currentObjectList.getJSONObject(x);
                    rooms.add(current.getString("room"));

                    Appliance appliance;
                    if(type.equals("scenes")) {
                        appliance = new Appliance(type, current.getString("name"), current.getString("room"), current.getInt("id"), current.getInt("automationGroup"), current.getInt("automationId"), current.getInt("automationValue"));
                    } else {
                        appliance = new Appliance(type, current.getString("name"), current.getString("room"), current.getInt("id"), current.getInt("automationGroup"), current.getInt("automationId"), 0);
                    }
                    st.add(appliance);
                }
            }
        }

        if(rooms.size() > 1) {
            rooms.add("All");
        }
        RoomFragment.mViewModel.setRooms(rooms);
        appliances.setValue(st);
    }

    //Do not load in user_parameters as their data field is a string and not an object.
    public void setActiveAppliances(JSONArray appliances) throws JSONException {
        HashSet<String> active = new HashSet<>();

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

    public void updateActiveAppliances(int id, int value, String room) {
        if(appliances.getValue() == null) {
            getAppliances();
            loadActiveAppliances();
            return;
        }

        for(Appliance appliance : appliances.getValue()) {
            if(appliance.id == id) {
                if(appliance.type.equals("scenes")) {
                    for(Appliance scene : appliances.getValue()) {
                        if(ApplianceViewModel.activeSceneList.contains(scene) && scene.room.equals(room) && scene.id != id) {
                            ApplianceViewModel.activeSceneList.remove(scene);
                        }
                    }

                    activeSceneList.add(appliance);
                } else {
                    if (value == 0) {
                        activeApplianceList.remove(String.valueOf(id));
                    } else {
                        activeApplianceList.add(String.valueOf(id));
                    }
                }
            }
        }

        //Update the current data set
        if(ApplianceAdapter.getInstance() != null) {
            ApplianceAdapter.getInstance().notifyDataSetChanged();
        }
    }

    public void loadActiveAppliances() {
        NetworkService.sendMessage("NUC", "Automation", "Get:appliances");
    }

    public void loadAppliances() {
        NetworkService.sendMessage("NUC", "Appliances", "List");
    }
}

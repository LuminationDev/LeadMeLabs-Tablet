package com.lumination.leadmelabs.ui.appliance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

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
    public static MutableLiveData<HashSet<String>> activeScenes;

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
            activeScenes = new MutableLiveData<>();
            loadActiveAppliances();
        }

        return activeAppliances;
    }

    /**
     * Set the appliances that are to be controlled from the tablet. The JSON file located on the
     * NUC is sent over and configured into the separate types.
     * @param applianceList A JSON received from the NUC containing all CBUS objects that are
     *                      to be controlled.
     * @throws JSONException If the JSON is not in the correct format an exception is thrown.
     */
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
                        appliance = new Appliance(type, current.getString("name"), current.getString("room"), current.getString("id"), current.getInt("automationGroup"), current.getInt("automationId"), current.getInt("automationValue"));
                    } else {
                        appliance = new Appliance(type, current.getString("name"), current.getString("room"), current.getString("id"), current.getInt("automationGroup"), current.getInt("automationId"), 0);

                        if(type.equals("LED rings")) {
                            Station temp = StationsFragment.mViewModel.getStationById(current.getInt("associatedStation"));
                            if(temp != null) {
                                temp.associated = appliance;
                            }
                        }
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

    /**
     * The result of calling the CBUS unit to find what objects are currently active. The JSON array
     * received contains the full list of objects on the CBUS, sorting and filtering needs to be
     * performed.
     * @param appliances A JSON received from the NUC containing all CBUS objects.
     * @throws JSONException If the JSON is not in the correct format an exception is thrown.
     */
    public void setActiveAppliances(JSONArray appliances) throws JSONException {
        HashSet<String> objects = new HashSet<>();
        HashSet<String> scenes = new HashSet<>();

        for (int i = 0; i < appliances.length(); i++) {
            if(appliances.getJSONObject(i).getString("is_user_parameter").equals("false")) {
                if (appliances.getJSONObject(i).getJSONObject("data").has("level")) {
                    if (!appliances.getJSONObject(i).getJSONObject("data").getString("level").equals("0")) {
                        objects.add(appliances.getJSONObject(i).getString("id"));
                    }
                } else if (appliances.getJSONObject(i).getJSONObject("data").has("value")) {
                    String value = appliances.getJSONObject(i).getJSONObject("data").getString("value");
                    String id = appliances.getJSONObject(i).getString("id");
                    String sceneId = id + value;
                    scenes.add(sceneId);
                }
            }
        }

        activeAppliances.setValue(objects);
        activeScenes.setValue(scenes);
    }

    /**
     * Receiving a message from the NUC after another tablet has activated something on the CBUS.
     * Update the necessary appliance object based on the supplied Id.
     * @param id An int representing the Id of the appliance, relates directly to the Id in the
     *           supplied JSON file.
     * @param value An int representing the new value of the appliance object.
     * @param room A string representing what room the appliance belongs to. Only applicable for the
     *             scene subtype.
     */
    public void updateActiveApplianceList(String id, int value, String room) {
        if(appliances.getValue() == null) {
            getAppliances();
            loadActiveAppliances();
            return;
        }

        for(Appliance appliance : appliances.getValue()) {
            if(appliance.id.equals(id)) {
                if(appliance.type.equals("scenes")) {
                    for(Appliance scene : appliances.getValue()) {
                        if(ApplianceViewModel.activeSceneList.contains(scene) && scene.room.equals(room) && !scene.id.equals(id)) {
                            ApplianceViewModel.activeSceneList.remove(scene);
                            ApplianceAdapter.getInstance().latestOff.add(scene.id);
                        }
                    }

                    activeSceneList.add(appliance);
                    ApplianceAdapter.getInstance().latestOn.add(appliance.id);

                    //TODO test that this does not cause an infinity loop
                    loadActiveAppliances();
                } else {
                    if (value == 0) {
                        activeApplianceList.remove(id);
                    } else {
                        activeApplianceList.add(id);
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

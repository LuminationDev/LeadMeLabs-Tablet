package com.lumination.leadmelabs.ui.appliance;

import android.annotation.SuppressLint;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuFragment;
import com.lumination.leadmelabs.utilities.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ApplianceViewModel extends ViewModel {
    //Use a hash map for active appliances, the id is the key, the value is the CBus value
    public static HashMap<String, String> activeApplianceList = new HashMap<>();
    //Use a hash map for active scenes, the room is the key, the value is the scene
    public static HashMap<String, Appliance> activeSceneList = new HashMap<>();
    //Use a hash map for active scenes
    public static MutableLiveData<HashMap<String, String>> activeScenes;
    private MutableLiveData<HashMap<String, String>> activeAppliances;

    private MutableLiveData<List<Appliance>> appliances;
    public static boolean init = false;

    public LiveData<List<Appliance>> getAppliances() {
        if (activeScenes == null) {
            activeScenes = new MutableLiveData<>();
        }
        if (appliances == null) {
            appliances = new MutableLiveData<>();
            loadAppliances();
        }

        return appliances;
    }

    public LiveData<HashMap<String, String>> getActiveAppliances() {
        if (activeAppliances == null) {
            activeAppliances = new MutableLiveData<>();
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
    @SuppressLint("NotifyDataSetChanged")
    public void setAppliances(JSONArray applianceList) throws JSONException {
        HashSet<String> types = new HashSet<>(); // Save the different types for the submenu options

        List<Appliance> st = new ArrayList<>();
        HashSet<String> rooms = new HashSet<>();

        HashMap<String, String> activeObjects = new HashMap<>();
        HashMap<String, String> inactiveObjects = new HashMap<>();
        HashMap<String, String> scenes = new HashMap<>();

        //Iterator over the outer loop - different appliance types
        for (int i = 0; i < applianceList.length(); i++) {
            JSONObject current = applianceList.getJSONObject(i);
            if (current.has("displayType") && !current.isNull("displayType")) {
                types.add(current.getString("displayType"));
            } else {
                types.add(current.getString("type"));
            }
            rooms.add(current.getString("room"));

            Appliance appliance = new Appliance(
                    current.getString("type"),
                    current.getString("name"),
                    current.getString("room"),
                    current.getString("id")
            );
            if (current.has("stations") && !current.isNull("stations") && current.getJSONArray("stations").length() > 0) {
                appliance.setStations(current.getJSONArray("stations"));
            }
            if (current.has("displayType") && !current.isNull("displayType")) {
                appliance.displayType = current.getString("displayType");
            }
            if (current.has("options") && !current.isNull("options")) {
                appliance.setOptions(current.getJSONArray("options"));
            }
            if (current.has("value") && !current.isNull("value")) {
                appliance.value = current.getString("value");
            }
            if (appliance.value.equals("0") || appliance.value.equals("")) {
                inactiveObjects.put(appliance.id, "0");
            } else {
                activeObjects.put(appliance.id, appliance.value);
            }
            if (appliance.type.equals(Constants.SCENE) && appliance.value.equals("On")) {
                scenes.put(appliance.id, appliance.value);
            }

            if (appliance.type.equals((Constants.BLIND))) {
                if(current.has("labels")) {
                    String labels = current.getString("labels");
                    if(!labels.equals("null")) {
                        ArrayList<String> customLabels = new ArrayList<>();
                        JSONObject jObj = new JSONObject(labels);

                        customLabels.add(!jObj.getString("open").equals("null") ? jObj.getString("open") : "OPEN");
                        customLabels.add(!jObj.getString("close").equals("null") ? jObj.getString("close") : "CLOSE");
                        customLabels.add(!jObj.getString("stop").equals("null") ? jObj.getString("stop") : "STOP");

                        appliance.description = customLabels;
                    }
                }

            }

            // this is deprecated as of January 2023. Please delete if more than 2 releases have occured since.
            if (appliance.type.equals((Constants.SOURCE))) {
                if(current.has("labels")) {
                    String labels = current.getString("labels");
                    if(!labels.equals("null")) {
                        ArrayList<String> customLabels = new ArrayList<>();
                        JSONObject jObj = new JSONObject(labels);

                        customLabels.add(!jObj.getString("hdmi1").equals("null") ? jObj.getString("hdmi1") : "HDMI 1");
                        customLabels.add(!jObj.getString("hdmi2").equals("null") ? jObj.getString("hdmi2") : "HDMI 2");
                        customLabels.add(!jObj.getString("hdmi3").equals("null") ? jObj.getString("hdmi3") : "HDMI 3");

                        appliance.description = customLabels;
                    }
                }
            }

            st.add(appliance);
        }

        if(rooms.size() > 1) {
            rooms.add("All");
        }

        ApplianceFragment.applianceCount.setValue(st.size());
        appliances.setValue(st);

        //OVER RIDES THE APPLIANCE LIST SO THE BLIND SCENE IS NO LONGER IN IT
        activeAppliances.setValue(activeObjects);

        //First connection from the Cbus, updates all Cards
        activeScenes.setValue(scenes);
        if(!init) {
            init = true;
            RoomFragment.mViewModel.setRooms(rooms);
            SubMenuFragment.mViewModel.setSubObjects(types);

            if(ApplianceAdapter.getInstance() != null) {
                ApplianceAdapter.getInstance().notifyDataSetChanged();
            }
            if (ApplianceParentAdapter.getInstance() != null) {
                ApplianceParentAdapter.getInstance().notifyDataSetChanged();
            }
        } else {
            clarifyBlindStatus(scenes);

            for(String cards : activeObjects.keySet()) {
                updateIfVisible(cards);
            }

            for(String cards : inactiveObjects.keySet()) {
                updateIfVisible(cards);
            }
        }
    }

    /**
     * Determine if a blind scene card needs to be updated. As the card is treated as both an appliance
     * and a scene rewriting the activeAppliances overrides the background value of the scene.
     */
    private void clarifyBlindStatus(HashMap<String, String> scenes) {
        if(this.appliances.getValue() != null) {
            for (Appliance appliance : this.appliances.getValue()) {
                if(ApplianceViewModel.activeSceneList.containsKey(appliance.name) && appliance.name.contains(Constants.BLIND_SCENE_SUBTYPE)) {
                    String value = scenes.get(appliance.id.substring(0, appliance.id.length() - 1));
                    if(value != null) {
                        appliance.value = value;

                        switch (value) {
                            case "0":
                                ApplianceViewModel.activeApplianceList.remove(appliance.id);
                                break;
                            case "1":
                                ApplianceViewModel.activeApplianceList.put(appliance.id, Constants.BLIND_STOPPED_VALUE);
                                break;
                            case "2":
                                ApplianceViewModel.activeApplianceList.put(appliance.id, Constants.APPLIANCE_ON_VALUE);
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Receiving a message from the NUC after another tablet has activated something on the CBUS.
     * Update the necessary object based on the supplied Id.
     * @param id An int representing the Id of the appliance, relates directly to the Id in the
     *           supplied JSON file.
     */
    public void updateActiveSceneList(String id) {
        if(appliances.getValue() == null) {
            getAppliances();
            loadActiveAppliances();
            return;
        }

        //Detect whether the set has changed
        Appliance temp = null;

        //Store the room value
        String room = null;

        for(Appliance appliance : appliances.getValue()) {
            if(appliance.id.equals(id)) {
                room = appliance.room;
                if(appliance.name.contains(Constants.BLIND_SCENE_SUBTYPE)) {
                    temp = activeSceneList.put(appliance.name, appliance);
                } else {
                    temp = activeSceneList.put(room, appliance);
                }
            }
        }

        // if a scene has changed load the active objects associated with the scene from the CBUS
        if(temp != activeSceneList.get(room)) {
            updateIfVisible(id);

            if(temp != null) {
                activeScenes.getValue().remove(temp.id);
                updateIfVisible(temp.id);
            }
        }
    }

    /**
     * Receiving a message from the NUC after another tablet has activated something on the CBUS.
     * Update the necessary object based on the supplied Id.
     * @param id An int representing the Id of the appliance, relates directly to the Id in the
     *           supplied JSON file.
     * @param value An int representing the new value of the appliance object.
     */
    public void updateActiveApplianceList(String id, String value, String ipAddress) {
        if(appliances.getValue() == null) {
            getAppliances();
            loadActiveAppliances();
            return;
        }

        //Disregard the message if from the same IP address
        if(NetworkService.getIPAddress().equals(ipAddress)) {
            return;
        }

        //Detect whether the set has changed
        String changed = null;

        //Find the appliance with the corresponding ID
        for(Appliance appliance : appliances.getValue()) {
            if(appliance.id.equals(id)) {
                //Set the new value - scenes is for the blind card
                if(appliance.type.equals("scenes")) {
                    changed = blindException(appliance, value);
                } else {
                    if (value.equals("0") || value.equals("1")) {
                        changed = activeApplianceList.remove(id);
                    } else if (!Objects.equals(activeApplianceList.get(id), value)) {
                        changed = "true";
                        activeApplianceList.put(id, value);
                    }
                }
            }
        }

        //Find the appliance within the recyclerview and notify of the change if currently present
        if(changed != null) {
            updateIfVisible(id);
        }
    }

    /**
     * The blind card on the scene page is treated as both a scene and an appliance. For this reason
     * there is a special exception that updates the Blind from the appliance updater.
     * @return A string representing if the appliance has changed value in any way.
     */
    private String blindException(Appliance appliance, String value) {
        String changed;

        appliance.value = value;

        if (value.equals("0")) {
            activeApplianceList.remove(appliance.id);
            Appliance temp = activeSceneList.remove(appliance.name);
            changed = temp == null ? null : "true";
        } else {
            if(value.equals(Constants.BLIND_SCENE_STOPPED) && !Objects.equals(activeApplianceList.get(appliance.id), Constants.BLIND_STOPPED_VALUE)) {
                activeSceneList.put(appliance.name, appliance);
                activeApplianceList.put(appliance.id, Constants.BLIND_STOPPED_VALUE);
                changed = "true";
            } else if(!Objects.equals(activeApplianceList.get(appliance.id), Constants.APPLIANCE_ON_VALUE)) {
                activeSceneList.put(appliance.name, appliance);
                activeApplianceList.put(appliance.id, Constants.APPLIANCE_ON_VALUE);
                changed = "true";
            } else {
                changed = null;
            }
        }

        return changed;
    }

    /**
     * Determine what the room type is and either update just the single ApplianceAdapter or cycle
     * through the multi adapters.
     */
    private void updateIfVisible(String id) {
        if(Objects.equals(RoomFragment.mViewModel.getSelectedRoom().getValue(), "All") && ApplianceFragment.checkForEmptyRooms(RoomFragment.mViewModel.getSelectedRoom().getValue())) {
            if(ApplianceParentAdapter.getInstance() != null) {
                ApplianceParentAdapter.getInstance().updateIfVisible(id);
            }
        } else {
            if(BaseAdapter.getInstance() != null) {
                BaseAdapter.getInstance().updateIfVisible(id);
            }
        }
    }

    public static void loadActiveAppliances() {
        NetworkService.sendMessage("NUC", "Automation", "Get:appliances");
    }

    //TODO this is called here and when the NUC address is set in main activity
    public void loadAppliances() {
        NetworkService.sendMessage("NUC", "Appliances", "List");
    }
}

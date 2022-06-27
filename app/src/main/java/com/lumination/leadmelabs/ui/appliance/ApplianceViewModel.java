package com.lumination.leadmelabs.ui.appliance;

import android.annotation.SuppressLint;
import android.os.CountDownTimer;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ApplianceViewModel extends ViewModel {
    public static HashSet<String> activeApplianceList = new HashSet<>();
    //Use a hash map for active scenes, the room is the key, the value is the scene
    public static HashMap<String, Appliance> activeSceneList = new HashMap<>();
    private MutableLiveData<HashSet<String>> activeAppliances;
    public static MutableLiveData<HashSet<String>> activeScenes;
    private MutableLiveData<List<Appliance>> appliances;
    public static boolean init = false;

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

                        if(type.equals("computers")) {
                            Station temp = StationsFragment.mViewModel.getStationById(current.getInt("associatedStation"));
                            if(temp != null) {
                                temp.automationGroup = appliance.automationGroup;
                                temp.automationId = appliance.automationId;
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
     * performed. It should only retrieve the scenes when first connecting. UpdateActiveApplianceList
     * handles the tablet syncing.
     * @param appliances A JSON received from the NUC containing all CBUS objects.
     * @throws JSONException If the JSON is not in the correct format an exception is thrown.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setActiveAppliances(JSONArray appliances) throws JSONException {
        HashSet<String> activeObjects = new HashSet<>();
        HashSet<String> inactiveObjects = new HashSet<>();
        HashSet<String> scenes = new HashSet<>();

        for (int i = 0; i < appliances.length(); i++) {
            if(appliances.getJSONObject(i).getString("is_user_parameter").equals("false")) {
                if (appliances.getJSONObject(i).getJSONObject("data").has("target")) {
                    if (!appliances.getJSONObject(i).getJSONObject("data").getString("target").equals("0")) {
                        activeObjects.add(appliances.getJSONObject(i).getString("id"));
                    } else {
                        inactiveObjects.add(appliances.getJSONObject(i).getString("id"));
                    }
                } else if (appliances.getJSONObject(i).getJSONObject("data").has("value")) {
                    String value = appliances.getJSONObject(i).getJSONObject("data").getString("value");
                    String id = appliances.getJSONObject(i).getString("id");
                    String sceneId = id + value;

                    if(!value.startsWith("-")) {
                        scenes.add(sceneId);
                    }
                }
            }
        }

        activeAppliances.setValue(activeObjects);

        if(!init) {
            activeScenes.setValue(scenes);
            init = true;

            if(ApplianceAdapter.getInstance() != null) {
                ApplianceAdapter.getInstance().notifyDataSetChanged();
            }
            if (ApplianceParentAdapter.getInstance() != null) {
                ApplianceParentAdapter.getInstance().notifyDataSetChanged();
            }
        } else {
            for(String cards : activeObjects) {
                updateIfVisible(cards);
            }

            for(String cards : inactiveObjects) {
                updateIfVisible(cards);
            }
        }
    }

    /**
     * Receiving a message from the NUC after another tablet has activated something on the CBUS.
     * Update the necessary object based on the supplied Id.
     * @param id An int representing the Id of the appliance, relates directly to the Id in the
     *           supplied JSON file.
     * @param room A string representing what room the appliance belongs to. Only applicable for the
     *             scene subtype.
     */
    public void updateActiveSceneList(String room, String id) {
        if(appliances.getValue() == null) {
            getAppliances();
            loadActiveAppliances();
            return;
        }

        //Detect whether the set has changed
        Appliance temp = null;

        for(Appliance appliance : appliances.getValue()) {
            if(appliance.id.equals(id)) {
                temp = activeSceneList.put(room, appliance);
            }
        }

        // if a scene has changed load the active objects associated with the scene from the CBUS
        if(temp != activeSceneList.get(room)) {
            delayLoadCall();
            updateIfVisible(id);

            if(temp != null) {
                updateIfVisible(temp.id);
            }
        }
    }

    private static final CountDownTimer timer = new CountDownTimer(2000, 1000) {
        @Override
        public void onTick(long l) {
        }

        @Override
        public void onFinish() {
            loadActiveAppliances();
        }
    };

    /**
     * Delay a call to the CBUS to get the active appliances, stops the unit from overloading if
     * the scenes are switched rapidly as it cancels any previous call.
     * Aim is to only run once (on the last update).
     */
    public static void delayLoadCall() {
        timer.cancel();
        timer.start();
    }

    /**
     * Receiving a message from the NUC after another tablet has activated something on the CBUS.
     * Update the necessary object based on the supplied Id.
     * @param id An int representing the Id of the appliance, relates directly to the Id in the
     *           supplied JSON file.
     * @param value An int representing the new value of the appliance object.
     */
    public void updateActiveApplianceList(String id, String value) {
        if(appliances.getValue() == null) {
            getAppliances();
            loadActiveAppliances();
            return;
        }

        //Detect whether the set has changed
        boolean changed = false;

        //Find the appliance with the corresponding ID
        for(Appliance appliance : appliances.getValue()) {
            if(appliance.id.equals(id)) {
                //Set the new value
                if (value.equals("0")) {
                    changed = activeApplianceList.remove(id);
                } else {
                    changed = activeApplianceList.add(id);
                }
            }
        }

        //Find the appliance within the recyclerview and notify of the change if currently present
        if(changed) {
            updateIfVisible(id);
        }
    }

    /**
     * Determine what the room type is and either update just the single ApplianceAdapter or cycle
     * through the multi adapters.
     */
    private void updateIfVisible(String id) {
        if(Objects.equals(RoomFragment.mViewModel.getSelectedRoom().getValue(), "All")) {
            if(ApplianceParentAdapter.getInstance() != null) {
                ApplianceParentAdapter.getInstance().updateIfVisible(id);
            }
        } else {
            if(ApplianceAdapter.getInstance() != null) {
                ApplianceAdapter.getInstance().updateIfVisible(id);
            }
        }
    }

    public static void loadActiveAppliances() {
        NetworkService.sendMessage("NUC", "Automation", "Get:appliances");
    }

    public void loadAppliances() {
        NetworkService.sendMessage("NUC", "Appliances", "List");
    }
}

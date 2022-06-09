package com.lumination.leadmelabs.ui.zones;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Scene;
import com.lumination.leadmelabs.models.Zone;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ZonesViewModel extends ViewModel {
    private MutableLiveData<List<Zone>> zones;

    public LiveData<List<Zone>> getZones() {
        if (zones == null) {
            zones = new MutableLiveData<>();
            loadZones();
        }

        return zones;
    }

    public void setZones(JSONArray zones) throws JSONException {
        List<Zone> zoneList = new ArrayList<>();

        for (int i = 0; i < zones.length(); i++) {
            ArrayList<Scene> sceneList = new ArrayList<>();
            JSONObject zoneObject = zones.getJSONObject(i);
            JSONArray scenes = zoneObject.getJSONArray("scenes");
            String activeSceneName = "";
            for (int j = 0; j < scenes.length(); j++) {
                JSONObject sceneObject = scenes.getJSONObject(j);
                JSONArray theatreIdsJson = sceneObject.getJSONArray("theatreIds");
                int[] theatreIds = new int[theatreIdsJson.length()];
                for (int k = 0; k < theatreIdsJson.length(); k++) {
                    theatreIds[k] = theatreIdsJson.getInt(k);
                }
                Scene scene = new Scene(sceneObject.getString("name"), sceneObject.getInt("id"), sceneObject.getString("automationValue"), theatreIds);
                scene.isActive.setValue(scene.value.equals(zoneObject.getString("currentValue")));
                if (scene.isActive.getValue()) {
                    activeSceneName = scene.name;
                }
                sceneList.add(scene);
            }
            Zone zone = new Zone(zoneObject.getString("name"), zoneObject.getInt("id"), zoneObject.getString("automationGroup"), zoneObject.getString("automationId"), sceneList);
            zone.activeSceneId = zoneObject.getString("currentValue");
            zone.activeSceneName = activeSceneName;
            zoneList.add(zone);
        }

        this.setZones(zoneList);
    }

    public void setZones(List<Zone> zones) {
        //this.zones.setValue(zones);
    }

    public void setActiveScene(String automationGroup, String automationId, String sceneValue, boolean fromNuc) {
        ArrayList<Zone> zoneList = (ArrayList<Zone>) this.zones.getValue();
        if (zoneList == null) {
            return;
        }
        for (int i = 0; i < zoneList.size(); i++) {
            Zone zone = zoneList.get(i);
            if (zone.automationGroup.equals(automationGroup) && zone.automationId.equals(automationId)) {
                zone.activeSceneId = sceneValue;
                ArrayList<Scene> sceneList = zone.scenes;
                sceneList = (ArrayList<Scene>) sceneList.clone();
                for (int j = 0; j < sceneList.size(); j++) {
                    Scene s = sceneList.get(j);
                    if (s.value.equals(zone.activeSceneId)) {
                        s.isActive.setValue(true);
                        zone.activeSceneName = s.name;
                        StationsFragment.mViewModel.setActiveTheatres(s.theatreIds, zone.zoneTheatreIds);
                        if (!fromNuc) { // don't need to tell the NUC about the change if the NUC told us about it
                            NetworkService.sendMessage("NUC", "Automation", "Set:0:" + zone.automationGroup + ":" + zone.automationId + ":" + s.value);
                        }
                    } else {
                        s.isActive.setValue(false);
                    }
                }
                this.zones.setValue(zoneList);
                break;
            }
        }

    }

    private void loadZones() {
        NetworkService.sendMessage("NUC","Zones", "List");
    }
}
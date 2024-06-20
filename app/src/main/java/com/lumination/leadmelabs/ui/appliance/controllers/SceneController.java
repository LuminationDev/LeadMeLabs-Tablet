package com.lumination.leadmelabs.ui.appliance.controllers;

import android.graphics.drawable.TransitionDrawable;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardApplianceSceneBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.SceneInfo;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceParentAdapter;
import com.lumination.leadmelabs.ui.appliance.adapters.ApplianceAdapter;
import com.lumination.leadmelabs.ui.appliance.adapters.BaseAdapter;
import com.lumination.leadmelabs.ui.appliance.adapters.SceneAdapter;
import com.lumination.leadmelabs.ui.dashboard.DashboardFragment;
import com.lumination.leadmelabs.ui.dashboard.DashboardModeManagement;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.utilities.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SceneController {
    /**
     * Track what scenes are being activated.
     */
    private static final Map<String, String> activatingScenes = Collections.synchronizedMap(new HashMap<>());

    public static ArrayList<String> latestOn = new ArrayList<>();
    public static ArrayList<String> latestOff = new ArrayList<>();
    public static int fadeTime = 200; //Fade time transitions in milliseconds;

    /**
     * Depending on the status of the scene card, change the background and start a transition.
     */
    public static void sceneTransition(String status, String id, View finalResult) {
        // just turned on - scene timers turned off
        if (latestOn.contains(id) && (Boolean.FALSE.equals(SettingsFragment.mViewModel.getSceneTimer().getValue()) || DashboardFragment.getInstance() == null)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_grey_to_blue, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(fadeTime);
            latestOn.remove(id);

            // just turned on - or is currently being set
        } else if ((latestOn.contains(id) || status.equals(Constants.LOADING))) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_grey_to_setting, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(fadeTime);
            latestOn.remove(id);

            //just turned off
        } else if (latestOff.contains(id)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_blue_to_grey, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(fadeTime);
            latestOff.remove(id);

            //just set
        } else if (status.equals(Constants.SET)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_setting_to_blue, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(fadeTime);

            //already active
        } else if(status.equals(Constants.ACTIVE)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_blue_to_grey, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.resetTransition();

            //not active
        } else {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_grey_to_blue, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.resetTransition();
        }
    }

    /**
     * Determine what icon a scene needs depending on their name.
     */
    public static void determineSceneType(CardApplianceSceneBinding binding, String status) {
        int icon;
        String name = binding.getAppliance().name.toLowerCase();

        if (name.contains("classroom") || name.equals("on")) {
            icon = status.equals(Constants.ACTIVE) ? R.drawable.icon_scene_classroommode_on :
                    R.drawable.icon_scene_classroommode_off;
        } else if (name.contains("vr")) {
            icon = status.equals(Constants.ACTIVE) ? R.drawable.icon_scene_vrmode_on :
                    R.drawable.icon_scene_vrmode_off;
        } else if (name.contains("apple") || name.contains("theatre") || name.contains("dim") ||
                name.contains("presentation") || name.contains("showcase")) {
            icon = status.equals(Constants.ACTIVE) ? R.drawable.icon_scene_theatremode_on :
                    R.drawable.icon_scene_theatremode_off;
        } else if (name.contains("off")) {
            icon = status.equals(Constants.ACTIVE) ? R.drawable.icon_scene_power_on :
                    R.drawable.icon_scene_power_off;
        } else if (name.contains("blind")) {
            icon = status.equals(Constants.ACTIVE) ? R.drawable.icon_appliance_blind_open :
                    status.equals(Constants.STOPPED) ? R.drawable.icon_settings :
                            R.drawable.icon_appliance_blind_closed;
        } else {
            icon = R.drawable.icon_settings;
        }

        binding.setIcon(new MutableLiveData<>(icon));
    }

    /**
     * Activate a scene which has been found using the searchForSceneTrigger, as we don't know exactly
     * what it is linked to we cannot activate the dashboard buttons.
     * @param scene The appliance object that is actively being triggered.
     */
    public static void handleBackupScene(Appliance scene) {
        // Retrieve the current list of appliances
        ArrayList<Appliance> currentList = getCurrentApplianceList();
        if (currentList == null) return;

        //Collect information about the scene
        SceneInfo info = collectSceneInfo(scene, Constants.LOADING);

        //Track which cards to visually update
        HashSet<String> updates = updateApplianceList(currentList, Constants.DISABLED,  info.getId(), info.getRoom());

        //Check what sort of RecyclerView is active then update the card's appearance if visible
        updateAdapter(ApplianceAdapter.getInstance(), updates);
    }

    /**
     * Activate the selected Scene whilst disabling all other scene cards in the same room
     * @param scene The appliance object that is actively being triggered.
     * @param isDashboardTrigger A boolean of if this has been triggered from the dashboard buttons
     */
    public static void handeActivatingScene(Appliance scene, boolean isDashboardTrigger) {
        // Retrieve the current list of appliances
        ArrayList<Appliance> currentList = getCurrentApplianceList();
        if (currentList == null) return;

        //Check if the scene timers are on in the settings
        boolean sceneTimers = Boolean.TRUE.equals(SettingsFragment.mViewModel.getSceneTimer().getValue()) && DashboardFragment.getInstance() != null;

        //Collect information about the scene
        SceneInfo info = collectSceneInfo(scene, sceneTimers ? Constants.LOADING : Constants.ACTIVE);

        //Track which cards to visually update
        HashSet<String> updates = updateApplianceList(currentList, sceneTimers ? Constants.DISABLED : Constants.INACTIVE,  info.getId(), info.getRoom());

        //Check what sort of RecyclerView is active then update the card's appearance if visible
        updateAdapter(ApplianceAdapter.getInstance(), updates);

        // Update the dashboard buttons and record the triggering scene
        if (scene != null && sceneTimers) {
            checkSceneAvailability(scene.name.toLowerCase(), scene.room, scene.id, isDashboardTrigger);
        }
    }

    /**
     * Checks the scene name and updates the mode button availability accordingly.
     * @param sceneName The name of the scene to be checked.
     * @param room The room a scene is linked to.
     * @param sceneId The id of the scene to be checked.
     * @param isDashboardTrigger A boolean of if this has been triggered from the dashboard buttons
     */
    private static void checkSceneAvailability(String sceneName, String room, String sceneId, boolean isDashboardTrigger) {
        // Check regular scenes
        if (sceneName.contains("classroom")) {
            // Update mode button availability for classroom mode
            if (!isDashboardTrigger) {
                DashboardFragment.getInstance().dashboardModeManagement.changeModeButtonAvailability("classroom", room, Constants.CLASSROOM_MODE);
            }
            activatingScenes.put(sceneId, Constants.CLASSROOM_MODE);
            return;
        }
        if (sceneName.contains("showcase")) {
            // Update mode button availability for presentation mode
            if (!isDashboardTrigger) {
                DashboardFragment.getInstance().dashboardModeManagement.changeModeButtonAvailability("showcase", room, Constants.SHOWCASE_MODE);
            }
            activatingScenes.put(sceneId, Constants.SHOWCASE_MODE);
            return;
        }
        if (sceneName.contains("vr")) {
            // Update mode button availability for VR mode
            if (!isDashboardTrigger) {
                DashboardFragment.getInstance().dashboardModeManagement.changeModeButtonAvailability("vr", room, Constants.VR_MODE);
            }
            activatingScenes.put(sceneId, Constants.VR_MODE);
            return;
        }

        // Check for alternate scenes with Station actions
        ArrayList<?> stationsOn = DashboardModeManagement.collectStations("Scene", sceneName, room, "On");
        if (!stationsOn.isEmpty()) {
            // Update mode button availability for basic_on mode
            if (!isDashboardTrigger) {
                DashboardFragment.getInstance().dashboardModeManagement.changeModeButtonAvailability("basic_on", room, Constants.BASIC_ON_MODE);
            }
            activatingScenes.put(sceneId, Constants.BASIC_ON_MODE);
            return;
        }

        ArrayList<?> stationsOff = DashboardModeManagement.collectStations("Scene", sceneName, room, "Off");
        if (!stationsOff.isEmpty()) {
            // Update mode button availability for basic_off mode
            if (!isDashboardTrigger) {
                DashboardFragment.getInstance().dashboardModeManagement.changeModeButtonAvailability("basic_off", room, Constants.BASIC_OFF_MODE);
            }
            activatingScenes.put(sceneId, Constants.BASIC_OFF_MODE);
            return;
        }

        // If scene doesn't match any specific criteria, set mode button availability to basic mode
        if (!isDashboardTrigger) {
            DashboardFragment.getInstance().dashboardModeManagement.changeModeButtonAvailability("basic", room, Constants.BASIC_MODE);
        }
        activatingScenes.put(sceneId, Constants.BASIC_MODE);
    }

    /**
     *
     * @param mode A string of the mode that was initially called.
     */
    public static void resetAfterSceneActivation(String mode) {
        // Retrieve the current list of appliances
        ArrayList<Appliance> currentList = getCurrentApplianceList();
        if (currentList == null) return;

        //Scene goes to status 'Set', visuals are updated and then after a few seconds the status is
        //set to active (no visual update)
        String sceneId = getSceneIdForMode(mode);

        //Get the 'scene' appliance object
        Appliance scene = currentList.stream()
                .filter(appliance -> appliance.id.equals(sceneId))
                .findFirst()
                .orElse(null);

        //Collect information about the scene
        SceneInfo info = collectSceneInfo(scene, Constants.SET);

        //Track which cards to visually update
        HashSet<String> updates = updateApplianceList(currentList, Constants.INACTIVE, info.getId(), info.getRoom());
        if (!info.getId().isEmpty()) {
            updates.add(info.getId());
        }

        //Check what sort of RecyclerView is active then update the card's appearance if visible
        updateAdapter(SceneAdapter.getInstance(), updates);

        //Reset the changed scene to Active as to fix the transition
        MainActivity.UIHandler.postDelayed(() -> {
            List<Appliance> resetList = currentList.stream()
                    .peek(appliance -> {
                        if (info.getId().equals(appliance.id)) { appliance.status.setValue(Constants.ACTIVE); }
                    })
                    .collect(Collectors.toList());

            ApplianceFragment.mViewModel.setAppliances(resetList);
        }, 5000);
    }

    /**
     * Retrieves the current list of appliances from the ViewModel.
     *
     * @return An ArrayList containing the current list of Appliance objects.
     */
    private static ArrayList<Appliance> getCurrentApplianceList() {
        return (ArrayList<Appliance>) ApplianceFragment.mViewModel.getAppliances().getValue();
    }

    /**
     * Collects information about a scene, including its room and Id, and updates its status if provided.
     *
     * @param scene         The Appliance object representing the scene. Can be null.
     * @param updatedStatus The status to set for the scene. Can be null.
     * @return A SceneInfo object containing the room and ID of the scene.
     */
    private static SceneInfo collectSceneInfo(Appliance scene, String updatedStatus) {
        String room = (scene == null) ? "All" : scene.room;
        String id = (scene == null) ? "" : scene.id;

        if (scene != null) {
            scene.status.setValue(updatedStatus);
        }

        return new SceneInfo(room, id);
    }

    /**
     * Updates the status of appliances in the provided list based on the specified criteria.
     *
     * @param currentList   The list of appliances to update.
     * @param updatedStatus The status to set for appliances that meet the criteria.
     * @param id            The Id used for filtering appliances.
     * @param room          The room used for filtering appliances. Can be "All" to indicate all rooms.
     * @return A HashSet containing the IDs of appliances that were updated.
     */
    public static HashSet<String> updateApplianceList(List<Appliance> currentList, String updatedStatus, String id, String room) {
        HashSet<String> updates = new HashSet<>();

        List<Appliance> updatedList = currentList.stream()
                .peek(appliance -> {
                    if (appliance.type.equals(Constants.SCENE) && !appliance.name.contains(Constants.BLIND_SCENE_SUBTYPE) && !id.equals(appliance.id) && (room.equals("All") || room.equals(appliance.room))) {
                        appliance.status.setValue(updatedStatus);
                        updates.add(appliance.id);
                    }
                })
                .collect(Collectors.toList());

        ApplianceFragment.mViewModel.setAppliances(updatedList);

        return updates;
    }

    /**
     * Updates the visible cards in the specified adapter based on the provided updates and scene information.
     *
     * @param adapter The adapter to update. Must be an instance of BaseAdapter, SceneAdapter, or ApplianceAdapter.
     * @param updates A HashSet containing the IDs of cards to update.
     */
    public static void updateAdapter(BaseAdapter adapter, HashSet<String> updates) {
        if (adapter == null) {
            return;
        }

        boolean isSceneAdapter = adapter instanceof SceneAdapter;

        for (String cards : updates) {
            if (isSceneAdapter) {
                adapter.updateIfVisible(cards);
            } else {
                adapter.updateIfVisible(cards);
            }
        }

        if (ApplianceParentAdapter.getInstance() == null) return;
        for (String cards : updates) {
            ApplianceParentAdapter.getInstance().updateIfVisible(cards);
        }
    }

    /**
     * Collect a scene name from the activatingScenes based on the mode that was set.
     * @param mode A string of the mode that was set with the scene.
     * @return A string of the scene name.
     */
    private static String getSceneIdForMode(String mode) {
        for (Map.Entry<String, String> entry : activatingScenes.entrySet()) {
            if (entry.getValue().equals(mode)) {
                String key = entry.getKey();
                activatingScenes.remove(key);
                return key; // Return the scene name for the matching mode
            }
        }
        return null; // Return null if the mode is not found in the map
    }

    public static void reset() {
        for (Map.Entry<String, String> entry : activatingScenes.entrySet()) {
            activatingScenes.remove(entry.getKey());
        }
    }
}

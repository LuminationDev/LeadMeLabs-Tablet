package com.lumination.leadmelabs.ui.appliance;

import android.graphics.drawable.TransitionDrawable;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.abstractClasses.AbstractApplianceStrategy;
import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.ui.appliance.Strategies.BlindStrategy;
import com.lumination.leadmelabs.ui.appliance.Strategies.SceneStrategy;
import com.lumination.leadmelabs.ui.appliance.Strategies.ToggleStrategy;
import com.lumination.leadmelabs.utilities.Constants;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Strategies to control the units on the CBUS.
 */
public class ApplianceController {
    public static ArrayList<String> latestOn = new ArrayList<>();
    public static ArrayList<String> latestOff = new ArrayList<>();
    public static int fadeTime = 200; //Fade time transitions in milliseconds;

    /**
     * Determine the type of strategy to use for the on click listener.
     * @param binding The binding associated with the appliance.
     * @param appliance The model populate with particular details of an appliance.
     * @param cardLayout The view that the binding is attached to.
     */
    public void strategyType(CardApplianceBinding binding, Appliance appliance, View cardLayout) {
        AbstractApplianceStrategy strategy;

        switch (appliance.type) {
            case "scenes":
                strategy = new SceneStrategy();
                break;

            case "blinds":
                strategy = new BlindStrategy();
                break;

            case "source":
            default:
                strategy = new ToggleStrategy();
                break;
        }

        strategy.trigger(binding, appliance, cardLayout);
        setIcon(binding);
    }

    /**
     * Determine the current status of an appliance card and organise the UI accordingly.
     */
    public String determineIfActive(Appliance appliance, View cardView) {
        String status;

        //Determine if the appliance is active
        if(appliance.type.equals("scenes")) {
            //This is applicable when first starting up the application
            if(ApplianceViewModel.activeScenes.getValue() != null) {
                if (ApplianceViewModel.activeScenes.getValue().remove(appliance.id)) {
                    ApplianceViewModel.activeSceneList.put(appliance.room, appliance);
                }
            }

            status = ApplianceViewModel.activeSceneList.containsValue(appliance) ? Constants.ACTIVE : Constants.INACTIVE;
            sceneTransition(status, appliance.id, cardView);

        } else {
            if(appliance.type.equals(Constants.BLIND) && Objects.equals(ApplianceViewModel.activeApplianceList.get(appliance.id), Constants.BLIND_STOPPED_VALUE)) {
                status = Constants.STOPPED;
            } else {
                status = ApplianceViewModel.activeApplianceList.containsKey(appliance.id) ? Constants.ACTIVE : Constants.INACTIVE;
            }

            if(status.equals(Constants.ACTIVE)) {
                applianceTransition(cardView, R.drawable.transition_appliance_fade);

            } else if(status.equals(Constants.STOPPED)) {
                applianceTransition(cardView, R.drawable.transition_blind_stopped);

            } else {
                applianceTransition(cardView, R.drawable.transition_appliance_fade_active);
            }
        }

        return status;
    }

    public static void applianceTransition(View cardView, int transitionDrawable) {
        cardView.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), transitionDrawable, null));
        TransitionDrawable transition = (TransitionDrawable) cardView.getBackground();
        transition.startTransition(fadeTime);
    }

    /**
     * Depending on the status of the scene card, change the background and start a transition.
     */
    private void sceneTransition(String status, String id, View finalResult) {
        // just turned on
        if (latestOn.contains(id)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_fade, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(fadeTime);
            latestOn.remove(id);

            //just turned off
        } else if (latestOff.contains(id)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_fade_active, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(fadeTime);
            latestOff.remove(id);

            //already active
        } else if(status.equals(Constants.ACTIVE)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_fade_active, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.resetTransition();

            //not active
        } else {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_fade, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.resetTransition();
        }
    }

    /**
     * Depending on an appliances type add an icon.
     * @param binding A SceneCardBinding relating associated with the current scene.
     */
    public static void setIcon(CardApplianceBinding binding) {
        if(binding.getAppliance().type.equals("scenes")) {
            determineSceneType(binding, Boolean.getBoolean(ApplianceViewModel.activeSceneList.containsValue(binding.getAppliance()) ? Constants.ACTIVE : Constants.INACTIVE));
        } else {
            determineApplianceType(binding, ApplianceViewModel.activeApplianceList.containsKey(binding.getAppliance().id) ? Constants.ACTIVE : Constants.INACTIVE);
        }
    }

    /**
     * Determine what icon a scene needs depending on their name.
     */
    private static void determineSceneType(CardApplianceBinding binding, Boolean status) {
        MutableLiveData<Integer> icon;

        switch (binding.getAppliance().name) {
            case "Classroom":
                icon = status ? new MutableLiveData<>(R.drawable.icon_scene_classroommode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_classroommode_off);
                break;
            case "VR Mode":
                icon = status ? new MutableLiveData<>(R.drawable.icon_scene_vrmode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_vrmode_off);
                break;
            case "Theatre":
                icon = status ? new MutableLiveData<>(R.drawable.icon_scene_theatremode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_theatremode_off);
                break;
            case "Off":
                icon = status ? new MutableLiveData<>(R.drawable.icon_scene_power_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_power_off);
                break;
            default:
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
        }

        binding.setIcon(icon);
    }

    /**
     * Determine what icon an appliance needs depending on their type.
     */
    private static void determineApplianceType(CardApplianceBinding binding, String status) {
        MutableLiveData<Integer> icon;

        //Add to this in the future
        switch(binding.getAppliance().type) {
            case "lights":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_appliance_light_bulb_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_light_bulb_off);
                break;

            case "blinds":
                if(status.equals(Constants.ACTIVE)) {
                    icon = new MutableLiveData<>(R.drawable.icon_appliance_blind_open);
                } else if(status.equals(Constants.STOPPED)) {
                    icon = new MutableLiveData<>(R.drawable.icon_settings);
                } else {
                    icon = new MutableLiveData<>(R.drawable.icon_appliance_blind_closed);
                }
                break;

            case "projectors":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_appliance_projector_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_projector_off);
                break;

            case "LED rings":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_appliance_ring_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_ring_off);
                break;

            case "sources":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_appliance_source_2) :
                        new MutableLiveData<>(R.drawable.icon_appliance_source_1);
                break;

            default:
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
        }

        binding.setIcon(icon);
    }
}

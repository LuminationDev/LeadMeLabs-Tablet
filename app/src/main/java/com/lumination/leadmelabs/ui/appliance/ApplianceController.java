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
import com.lumination.leadmelabs.ui.appliance.Strategies.HDMIStrategy;
import com.lumination.leadmelabs.ui.appliance.Strategies.RadioStrategy;
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
                strategy = appliance.name.contains(Constants.BLIND_SCENE_SUBTYPE) ? new BlindStrategy(true) :  new SceneStrategy();
                break;

            case "blinds":
                strategy = new BlindStrategy(false);
                break;

            case "sources":
                strategy = new HDMIStrategy(false);
                break;

            default:
                strategy = new ToggleStrategy();
                break;
        }

        strategy.trigger(binding, appliance, cardLayout);
        setIcon(binding);
    }

    /**
     * Determine the type of strategy to use for the on click listener.
     * @param appliance The model populate with particular details of an appliance.
     * @param value The value that the appliance is being set to
     */
    public void triggerRadioAppliance(Appliance appliance, String value) {
        RadioStrategy strategy = new RadioStrategy();
        strategy.trigger(appliance, value);
    }

    /**
     * Determine the current status of an appliance card and organise the UI accordingly.
     */
    public String determineIfActive(Appliance appliance, View cardView) {
        String status;

        //Determine if the appliance is active
        if(appliance.type.equals("scenes")) {
            status = isSceneActive(appliance, cardView);

        } else {
            status = isApplianceActive(appliance, cardView);
        }

        return status;
    }

    /**
     * Determine if a scene card is currently active. A different method is used for the blind
     * controller as they extend the card instead of having different cards for different options.
     */
    public String isSceneActive(Appliance appliance, View cardView) {
        String status;
        String value = null;

        //This is applicable when first starting up the application
        if(ApplianceViewModel.activeScenes.getValue() != null) {
            String sceneId = appliance.id;
            value = ApplianceViewModel.activeScenes.getValue().get(sceneId);

            if (appliance.name.contains("Blind") && value != null && !value.equals("0")) {
                ApplianceViewModel.activeSceneList.put(appliance.name, appliance);
            } else if(Objects.equals(value, "On")) {
                ApplianceViewModel.activeSceneList.put(appliance.room, appliance);
            }
        }

        if(appliance.name.contains(Constants.BLIND_SCENE_SUBTYPE)) {
            if(appliance.value != null) {
                value = appliance.value;
            }

            //Add the scene to the activeApplianceList with the correct value
            if(ApplianceViewModel.activeSceneList.get(appliance.name) != null) {
                if(Objects.equals(value, Constants.BLIND_SCENE_STOPPED)) {
                    status = Constants.STOPPED;
                    ApplianceViewModel.activeApplianceList.put(appliance.id, Constants.BLIND_STOPPED_VALUE);
                } else {
                    status = Constants.ACTIVE;
                    ApplianceViewModel.activeApplianceList.put(appliance.id, Constants.APPLIANCE_ON_VALUE);
                }
            } else {
                status = Constants.INACTIVE;
                ApplianceViewModel.activeApplianceList.remove(appliance.id);
            }

            applianceTransition(status, cardView);
        } else {
            status = ApplianceViewModel.activeSceneList.containsValue(appliance) ? Constants.ACTIVE : Constants.INACTIVE;
            sceneTransition(status, appliance.id, cardView);
        }

        return status;
    }

    /**
     * Determine if an appliance card is active or not.
     */
    public String isApplianceActive(Appliance appliance, View cardView) {
        String status;

        if(appliance.type.equals(Constants.BLIND) && Objects.equals(ApplianceViewModel.activeApplianceList.get(appliance.id), Constants.BLIND_STOPPED_VALUE)) {
            status = Constants.STOPPED;
        } else if (appliance.options != null) {
            String currentValue = ApplianceViewModel.activeApplianceList.get(appliance.id);
            if (currentValue == null) {
                status = Constants.ACTIVE;
            } else if (appliance.options.size() > 0 && currentValue.equals(appliance.options.get(0).id)) {
                status = Constants.ACTIVE;
            } else if (appliance.options.size() > 1 && currentValue.equals(appliance.options.get(1).id)) {
                status = Constants.INACTIVE;
            } else if (appliance.options.size() > 2 && currentValue.equals(appliance.options.get(2).id)) {
                status = Constants.STOPPED;
            } else {
                status = Constants.ACTIVE;
            }
        }
        else if (appliance.type.equals(Constants.SOURCE) && Objects.equals(ApplianceViewModel.activeApplianceList.get(appliance.id), Constants.SOURCE_HDMI_3)) {
            status = Constants.STOPPED;
        } else {
            status = ApplianceViewModel.activeApplianceList.containsKey(appliance.id) ? Constants.ACTIVE : Constants.INACTIVE;
        }

        applianceTransition(status, cardView);

        return status;
    }

    /**
     * Apply the proper transition for an appliance or blind card.
     */
    private void applianceTransition(String status, View cardView) {
        if(status.equals(Constants.ACTIVE)) {
            applianceTransition(cardView, R.drawable.transition_appliance_grey_to_blue);

        } else if(status.equals(Constants.STOPPED)) {
            applianceTransition(cardView, R.drawable.transition_appliance_none_to_navy);

        } else {
            applianceTransition(cardView, R.drawable.transition_appliance_blue_to_grey);
        }
    }

    /**
     * Set a new transition background for the supplied card object.
     */
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
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_grey_to_blue, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(fadeTime);
            latestOn.remove(id);

            //just turned off
        } else if (latestOff.contains(id)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_blue_to_grey, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(fadeTime);
            latestOff.remove(id);

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
     * Depending on an appliances type add an icon.
     * @param binding A SceneCardBinding relating associated with the current scene.
     */
    public static void setIcon(CardApplianceBinding binding) {
        if(binding.getAppliance().type.equals("scenes")) {
            determineSceneType(binding, ApplianceViewModel.activeSceneList.containsValue(binding.getAppliance()) ? Constants.ACTIVE : Constants.INACTIVE);
        } else {
            determineApplianceType(binding, ApplianceViewModel.activeApplianceList.containsKey(binding.getAppliance().id) ? Constants.ACTIVE : Constants.INACTIVE);
        }
    }

    /**
     * Determine what icon a scene needs depending on their name.
     */
    private static void determineSceneType(CardApplianceBinding binding, String status) {
        MutableLiveData<Integer> icon;

        switch (binding.getAppliance().name) {
            case "Classroom":
            case "On":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_scene_classroommode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_classroommode_off);
                break;
            case "VR Mode":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_scene_vrmode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_vrmode_off);
                break;
            case "Apple TV":
            case "Theatre":
            case "Dim":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_scene_theatremode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_theatremode_off);
                break;
            case "Off":
            case "All Off":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_scene_power_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_power_off);
                break;
            case "Window Blinds":
            case "Blind Control":
                if(status.equals(Constants.ACTIVE)) {
                    icon = new MutableLiveData<>(R.drawable.icon_appliance_blind_open);
                } else if(status.equals(Constants.STOPPED)) {
                    icon = new MutableLiveData<>(R.drawable.icon_settings);
                } else {
                    icon = new MutableLiveData<>(R.drawable.icon_appliance_blind_closed);
                }
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

            case "computers":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_computer_on) :
                        new MutableLiveData<>(R.drawable.icon_computer_off);
                break;

            case "LED rings":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_appliance_ring_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_ring_off);
                break;

            case "sources":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_appliance_source_2) :
                        new MutableLiveData<>(R.drawable.icon_appliance_source_1);
                break;

            case "splicers":
                icon = status.equals(Constants.ACTIVE) ? new MutableLiveData<>(R.drawable.icon_splicer_white) :
                        new MutableLiveData<>(R.drawable.icon_splicer_grey);
                break;

            default:
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
        }

        binding.setIcon(icon);
    }
}

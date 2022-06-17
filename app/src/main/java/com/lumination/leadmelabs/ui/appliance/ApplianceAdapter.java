package com.lumination.leadmelabs.ui.appliance;

import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;

import java.util.ArrayList;

/**
 * Use this adapter for scripts in the future.
 */
public class ApplianceAdapter extends BaseAdapter {
    private final String TAG = "ApplianceAdapter";

    public static ApplianceAdapter instance;
    public static ApplianceAdapter getInstance() { return instance; }

    public ArrayList<Appliance> applianceList = new ArrayList<>();
    public ArrayList<String> latestOn = new ArrayList<>();
    public ArrayList<String> latestOff = new ArrayList<>();

    private LayoutInflater mInflater;

    ApplianceAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
        instance = this;
    }

    @Override
    public int getCount() {
        return applianceList != null ? applianceList.size() : 0;
    }

    @Override
    public Appliance getItem(int position) {
        return applianceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public String getItemType(int position) { return applianceList.get(position).type; }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        View result = view;

        CardApplianceBinding binding;
        Appliance appliance = getItem(position);
        String id = String.valueOf(appliance.id);

        if (result == null) {
            if (mInflater == null) {
                mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            binding = CardApplianceBinding.inflate(mInflater, parent, false);
            result = binding.getRoot();
            result.setTag(binding);
        } else {
            binding = (CardApplianceBinding) result.getTag();
        }
        binding.setAppliance(appliance);

        boolean active;

        //Determine if the appliance is active
        if(getItemType(position).equals("scenes")) {
            if(ApplianceViewModel.activeScenes.getValue() != null) {
                if (ApplianceViewModel.activeScenes.getValue().contains(String.valueOf(getItem(position).id))) {
                    ApplianceViewModel.activeSceneList.add(getItem(position));
                }
            }
            active = ApplianceViewModel.activeSceneList.contains(getItem(position));
            sceneTransition(active, id, result);

        } else {
            active = ApplianceViewModel.activeApplianceList.contains(id);

            TransitionDrawable transition = (TransitionDrawable) result.getBackground();
            if(active) {
                transition.startTransition(200);
            }
        }

        //TODO Find appropriate place to put this after more testing
        //Clear the active list after loading
        ApplianceViewModel.activeScenes = new MutableLiveData<>();

        //Load what appliance is active or not
        setIcon(binding, active);
        binding.setIsActive(new MutableLiveData<>(active));

        View finalResult = result;
        result.setOnClickListener(v -> {
            String type = getItemType(position);

            switch (type) {
                case "scenes":
                    sceneStrategy(binding, appliance, finalResult);
                    break;
                case "blinds":
                    //Open the blind widget when that is built for now just act as a toggle
                case "source":
                    //Toggle the source when that is implemented for now just act as a toggle
                default:
                    toggleStrategy(binding, appliance, finalResult);
                    break;
            }
        });

        return result;
    }

    /**
     * Depending on an appliances type add an icon.
     * @param binding A SceneCardBinding relating associated with the current scene.
     * @param active A Boolean representing if the appliance is currently on or off
     */
    private void setIcon(CardApplianceBinding binding, Boolean active) {
        if(binding.getAppliance().type.equals("scenes")) {
            determineSceneType(binding, active);
        } else {
            determineApplianceType(binding, active);
        }
    }

    /**
     * Determine what icon a scene needs depending on their name.
     */
    private void determineSceneType(CardApplianceBinding binding, Boolean active) {
        MutableLiveData<Integer> icon;

        switch (binding.getAppliance().name) {
            case "Classroom":
                icon = active ? new MutableLiveData<>(R.drawable.icon_scene_classroommode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_classroommode_off);
                break;
            case "VR Mode":
                icon = active ? new MutableLiveData<>(R.drawable.icon_scene_vrmode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_vrmode_off);
                break;
            case "Theatre":
                icon = active ? new MutableLiveData<>(R.drawable.icon_scene_theatremode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_theatremode_off);
                break;
            case "Off":
                icon = active ? new MutableLiveData<>(R.drawable.icon_scene_power_on) :
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
    private void determineApplianceType(CardApplianceBinding binding, Boolean active) {
        MutableLiveData<Integer> icon;

        //Add to this in the future
        switch(binding.getAppliance().type) {
            case "lights":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_light_bulb_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_light_bulb_off);
                break;
            case "blinds":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_blind_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_blind_off);
                break;
            case "projectors":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_projector_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_projector_off);
                break;
            case "LED rings":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_ring_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_ring_off);
                break;
            case "sources":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_source_2) :
                        new MutableLiveData<>(R.drawable.icon_appliance_source_1);
                break;
            default:
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
        }

        binding.setIcon(icon);
    }

    private void sceneTransition(boolean active, String id, View finalResult) {
        // just turned on
        if (latestOn.contains(id)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_fade, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(200);
            latestOn.remove(id);

            //just turned off
        } else if (latestOff.contains(id)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_fade_active, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(200);
            latestOff.remove(id);

            //already active
        } else if(active) {
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

    //Strategies to control the units on the CBUS, there should be toggle and dimmer
    private void toggleStrategy(CardApplianceBinding binding, Appliance appliance, View finalResult) {
        String value;
        TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();

        if(ApplianceViewModel.activeApplianceList.contains(String.valueOf(appliance.id))) {
            transition.reverseTransition(200);
            binding.setIsActive(new MutableLiveData<>(false));
            ApplianceViewModel.activeApplianceList.remove(String.valueOf(appliance.id));
            value = "0";

        } else {
            binding.setIsActive(new MutableLiveData<>(true));
            transition.startTransition(200);
            ApplianceViewModel.activeApplianceList.add(String.valueOf(appliance.id));
            value = "255";
        }

        //Set the new icon and send a message to the NUC
        setIcon(binding, ApplianceViewModel.activeApplianceList.contains(String.valueOf(appliance.id)));
        NetworkService.sendMessage("NUC", "Automation", "Set:0:" + appliance.automationGroup + ":" + appliance.automationId  + ":" + appliance.id + ":" + value + ":" + appliance.room);
    }


    private void sceneStrategy(CardApplianceBinding binding, Appliance scene, View finalResult) {
        TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
        //Set the new icon and send a message to the NUC
        setIcon(binding, ApplianceViewModel.activeSceneList.contains(scene));

        if(ApplianceViewModel.activeSceneList.contains(scene)) {
            //Do nothing don't want to double click something
            return;
        } else {
            binding.setIsActive(new MutableLiveData<>(true));
            ApplianceViewModel.activeSceneList.add(scene);
            latestOn.add(scene.id);
        }

        //Remove any scene from the activeScene list that is in the same room?
        for(Appliance appliance : applianceList) {
            if(ApplianceViewModel.activeSceneList.contains(appliance) && appliance.room.equals(scene.room) && !appliance.id.equals(scene.id)) {
                ApplianceViewModel.activeSceneList.remove(appliance);
                latestOff.add(appliance.id);
            }
        }

        notifyDataSetChanged();
        NetworkService.sendMessage("NUC", "Automation", "Set:0:" + scene.automationGroup + ":" + scene.automationId  + ":" + scene.id + ":" + scene.automationValue + ":" + scene.room);
    }
}

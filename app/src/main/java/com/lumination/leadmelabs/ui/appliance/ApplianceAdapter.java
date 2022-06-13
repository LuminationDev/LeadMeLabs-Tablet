package com.lumination.leadmelabs.ui.appliance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.lifecycle.MutableLiveData;

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
        } else {
            active = ApplianceViewModel.activeApplianceList.contains(id);
        }

        //TODO Find appropriate place to put this after more testing
        //Clear the active list after loading
        ApplianceViewModel.activeScenes = new MutableLiveData<>();

        //Load what appliance is active or not
        setIcon(binding, getItemType(position), active);
        binding.setIsActive(new MutableLiveData<>(active));

        result.setOnClickListener(v -> {
            if(getItemType(position).equals("scenes")) {
                sceneStrategy(binding, appliance, position);
            } else {
                toggleStrategy(binding, appliance, position);
            }
        });

        return result;
    }

    /**
     * Depending on a scenes type add an icon.
     * @param binding A SceneCardBinding relating associated with the current scene.
     * @param type A string representing what appliance type the card triggers on the CBUS.
     * @param active A Boolean representing if the appliance is currently on or off
     */
    private void setIcon(CardApplianceBinding binding, String type, Boolean active) {
        MutableLiveData<Integer> icon;

        //Add to this in the future
        switch(type) {
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

                //TODO add scene cases

            default:
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
        }

        binding.setIcon(icon);
    }

    //Strategies to control the units on the CBUS, there should be toggle and dimmer
    private void toggleStrategy(CardApplianceBinding binding, Appliance appliance, int position) {
        //Set the new icon and send a message to the NUC
        setIcon(binding, getItemType(position), ApplianceViewModel.activeApplianceList.contains(String.valueOf(appliance.id)));

        String value;

        if(ApplianceViewModel.activeApplianceList.contains(String.valueOf(appliance.id))) {
            binding.setIsActive(new MutableLiveData<>(false));
            ApplianceViewModel.activeApplianceList.remove(String.valueOf(appliance.id));
            value = "0";

        } else {
            binding.setIsActive(new MutableLiveData<>(true));
            ApplianceViewModel.activeApplianceList.add(String.valueOf(appliance.id));
            value = "255";
        }

        NetworkService.sendMessage("NUC", "Automation", "Set:0:" + appliance.automationGroup + ":" + appliance.automationId  + ":" + appliance.id + ":" + value + ":" + appliance.room);
    }


    private void sceneStrategy(CardApplianceBinding binding, Appliance scene, int position) {
        //Set the new icon and send a message to the NUC
        setIcon(binding, getItemType(position), ApplianceViewModel.activeSceneList.contains(scene));

        if(ApplianceViewModel.activeSceneList.contains(scene)) {
            //Do nothing don't want to double click something
            return;
        } else {
            binding.setIsActive(new MutableLiveData<>(true));
            ApplianceViewModel.activeSceneList.add(scene);
        }

        //Remove any scene from the activeScene list that is in the same room?
        for(Appliance appliance : applianceList) {
            if(ApplianceViewModel.activeSceneList.contains(appliance) && appliance.room.equals(scene.room) && !appliance.id.equals(scene.id)) {
                ApplianceViewModel.activeSceneList.remove(appliance);
            }
        }

        notifyDataSetChanged();
        NetworkService.sendMessage("NUC", "Automation", "Set:0:" + scene.automationGroup + ":" + scene.automationId  + ":" + scene.id + ":" + scene.automationValue + ":" + scene.room);
    }
}

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

    public ArrayList<Appliance> applianceList = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context context;
    private View list_view;

    ApplianceAdapter(Context context, View list_view) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.list_view = list_view;
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
        return applianceList.get(position).id;
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

        Boolean active = ApplianceViewModel.activeApplianceList.contains(id);

        //Load what appliance is active or not
        setIcon(binding, getItemType(position), active);
        binding.setIsActive(new MutableLiveData<>(active));

        result.setOnClickListener(v -> {
            //TODO Expand this so we can load different trigger strategies
            String value = toggleStrategy(binding, id);

            //Set the new icon and send a message to the NUC
            setIcon(binding, getItemType(position), ApplianceViewModel.activeApplianceList.contains(id));
            NetworkService.sendMessage("NUC", "Automation", "SetId:" + appliance.id + ":" + value);
        });

        return result;
    }

    /**
     * Depending on a scenes value add an icon.
     * @param binding A SceneCardBinding relating associated with the current scene.
     * @param type An int representing what appliance the card triggers on the CBUS.
     */
    private void setIcon(CardApplianceBinding binding, String type, Boolean active) {
        MutableLiveData<Integer> icon;

        //Add to this in the future
        switch(type) {
            case "lighting":
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
            case "rings":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_ring_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_ring_off);
                break;
            case "sources":
                icon = new MutableLiveData<>(R.drawable.icon_home);
                break;

            default:
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
        }

        binding.setIcon(icon);
    }

    //Strategies to control the units on the CBUS, there should be toggle and dimmer
    private String toggleStrategy(CardApplianceBinding binding, String id) {
        if(ApplianceViewModel.activeApplianceList.contains(id)) {
            binding.setIsActive(new MutableLiveData<>(false));
            ApplianceViewModel.activeApplianceList.remove(id);
            return "0";

        } else {
            binding.setIsActive(new MutableLiveData<>(true));
            ApplianceViewModel.activeApplianceList.add(id);
            return "255";
        }
    }
}

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
    public ArrayList<String> activeApplianceList = new ArrayList<>();
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

        binding.setAppliance(getItem(position));
        setIcon(binding, getItemType(position));

        //Load what appliance is active or not
        if(activeApplianceList.contains(String.valueOf(getItem(position).id))) {
            binding.setIsActive(new MutableLiveData<>(true));
        } else {
            binding.setIsActive(new MutableLiveData<>(false));
        }

        result.setOnClickListener(v -> {
            //TODO Expand this so we can load different trigger strategies
            String value = toggleStrategy(binding, position);
            NetworkService.sendMessage("NUC", "Automation", "SetId:" + getItem(position).id + ":" + value);
        });

        return result;
    }

    private String toggleStrategy(CardApplianceBinding binding, int position) {
        if(activeApplianceList.contains(String.valueOf(getItem(position).id))) {
            binding.setIsActive(new MutableLiveData<>(false));
            activeApplianceList.remove(String.valueOf(getItem(position).id));
            return "0";

        } else {
            binding.setIsActive(new MutableLiveData<>(true));
            activeApplianceList.add(String.valueOf(getItem(position).id));
            return "255";
        }
    }

    /**
     * Depending on a scenes value add an icon.
     * @param binding A SceneCardBinding relating associated with the current scene.
     * @param type An int representing what appliance the card triggers on the CBUS.
     */
    private void setIcon(CardApplianceBinding binding, String type) {
        MutableLiveData<Integer> icon;

        //Add to this in the future
        switch(type) {
            case "lighting":
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
            case "blinds":
                icon = new MutableLiveData<>(R.drawable.icon_home);
                break;
            case "projector":
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
            case "rings":
                icon = new MutableLiveData<>(R.drawable.icon_home);
                break;
            case "source":
                icon = new MutableLiveData<>(R.drawable.icon_home);
                break;

            default:
                icon = new MutableLiveData<>(R.drawable.icon_home);
                break;
        }

        binding.setIcon(icon);
    }
}

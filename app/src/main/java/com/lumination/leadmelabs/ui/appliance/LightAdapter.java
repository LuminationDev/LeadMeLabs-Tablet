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
public class LightAdapter extends BaseAdapter {
    private final String TAG = "ApplianceAdapter";

    //Not sure if this is a good idea or not but handy to access for data binding UI changes
    public ArrayList<CardApplianceBinding> applianceBindings = new ArrayList<>();

    public ArrayList<Appliance> applianceList = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context context;
    private View list_view;

    LightAdapter(Context context, View list_view) {
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

    public int getItemValue(int position) { return applianceList.get(position).value; }

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
        setIcon(binding, getItemValue(position));

        //Load what scene has been selected - change this as it should be a list....
        if(LightFragment.mViewModel.activeAppliances.contains(String.valueOf(getItem(position).id))) {
            binding.setIsActive(new MutableLiveData<>(true));
        } else {
            binding.setIsActive(new MutableLiveData<>(false));
        }

        result.setOnClickListener(v -> {
            String value;
            if(LightFragment.mViewModel.activeAppliances.contains(String.valueOf(getItem(position).id))) {
                binding.setIsActive(new MutableLiveData<>(false));
                LightFragment.mViewModel.activeAppliances.remove(String.valueOf(getItem(position).id));
                value = "0";

            } else {
                binding.setIsActive(new MutableLiveData<>(true));
                LightFragment.mViewModel.activeAppliances.add(String.valueOf(getItem(position).id));
                value = "255";
            }

            NetworkService.sendMessage("NUC", "Automation", "SetId:" + getItem(position).id + ":" + value);
        });

        applianceBindings.add(binding);

        return result;
    }

    /**
     * Depending on a scenes value add an icon.
     * @param binding A SceneCardBinding relating associated with the current scene.
     * @param type An int representing what appliance the card triggers on the CBUS.
     */
    private void setIcon(CardApplianceBinding binding, int type) {
        MutableLiveData<Integer> icon;

        //Add to this in the future
        switch(type) {
            case 0:
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
            case 1:
                icon = new MutableLiveData<>(R.drawable.icon_home);
                break;
            case 2:
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
            case 3:
                icon = new MutableLiveData<>(R.drawable.icon_home);
                break;
            default:
                icon = new MutableLiveData<>(R.drawable.icon_home);
                break;
        }

        binding.setIcon(icon);
    }
}

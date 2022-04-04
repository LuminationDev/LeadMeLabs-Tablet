package com.lumination.leadmelabs.ui.scenes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.R;

import com.lumination.leadmelabs.databinding.SceneCardBinding;
import com.lumination.leadmelabs.models.Scene;
import com.lumination.leadmelabs.services.NetworkService;

import java.util.ArrayList;

public class SceneAdapter extends BaseAdapter {
    private final String TAG = "ScenesAdapter";

    //Not sure if this is a good idea or not but handy to access for data binding UI changes
    public ArrayList<SceneCardBinding> sceneBindings = new ArrayList<>();

    public ArrayList<Scene> sceneList = new ArrayList<>();
    public int selected;
    private LayoutInflater mInflater;
    private Context context;
    private View list_view;

    SceneAdapter(Context context, View list_view) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.list_view = list_view;
    }

    @Override
    public int getCount() {
        return sceneList != null ? sceneList.size() : 0;
    }

    @Override
    public Scene getItem(int position) {
        return sceneList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return sceneList.get(position).number;
    }

    public int getItemValue(int position) { return sceneList.get(position).value; }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        View result = view;

        SceneCardBinding binding;

        if (result == null) {
            if (mInflater == null) {
                mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            binding = SceneCardBinding.inflate(mInflater, parent, false);
            result = binding.getRoot();
            result.setTag(binding);
        } else {
            binding = (SceneCardBinding) result.getTag();
        }
        binding.setScene(getItem(position));
        setIcon(binding, getItemValue(position));

        //Load what scene has been selected
        if(getItemValue(position) == selected) {
            binding.setIsActive(new MutableLiveData<>(true));
        }

        result.setOnClickListener(v -> {
            binding.setIsActive(new MutableLiveData<>(true));
            ScenesFragment.mViewModel.setCurrentValue(getItemValue(position));

            for(SceneCardBinding sceneBinding : sceneBindings) {
                if(sceneBinding != binding) {
                    sceneBinding.setIsActive(new MutableLiveData<>(false));
                }
            }

            //Disable when not connected to CBUS otherwise NUC will timeout waiting for response
            NetworkService.sendMessage("NUC", "Automation", "TriggerScene:" + getItemValue(position));
        });

        sceneBindings.add(binding);

        return result;
    }

    /**
     * Depending on a scenes value add an icon.
     * @param binding A SceneCardBinding relating associated with the current scene.
     * @param value An int representing what scene the card triggers on the CBUS.
     */
    private void setIcon(SceneCardBinding binding, int value) {
        MutableLiveData<Integer> icon;

        //Add to this in the future
        switch(value) {
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

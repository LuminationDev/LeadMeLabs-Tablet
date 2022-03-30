package com.lumination.leadmelabs.ui.scenes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.lumination.leadmelabs.databinding.SceneCardBinding;
import com.lumination.leadmelabs.models.Scene;

import java.util.ArrayList;

public class ScenesAdapter extends BaseAdapter {
    private final String TAG = "ScenesAdapter";

    public ArrayList<Scene> sceneList = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context context;
    private View list_view;

    ScenesAdapter(Context context, View list_view) {
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

        return result;
    }
}

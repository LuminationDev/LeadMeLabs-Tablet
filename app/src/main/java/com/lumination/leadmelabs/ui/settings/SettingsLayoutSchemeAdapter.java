package com.lumination.leadmelabs.ui.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.lifecycle.LifecycleOwner;

import com.lumination.leadmelabs.databinding.ItemLayoutTabletLayoutBinding;

import java.util.List;

public class SettingsLayoutSchemeAdapter extends BaseAdapter {
    private final Context mContext;
    private final LifecycleOwner mLifecycleOwner;
    private final List<String> mData;

    public SettingsLayoutSchemeAdapter(Context context, List<String> data, LifecycleOwner lifecycleOwner) {
        mContext = context;
        mData = data;
        mLifecycleOwner = lifecycleOwner;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemLayoutTabletLayoutBinding binding;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            binding = ItemLayoutTabletLayoutBinding.inflate(inflater, parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
        }  else {
            binding = (ItemLayoutTabletLayoutBinding) convertView.getTag();
        }

        binding.setLifecycleOwner(mLifecycleOwner);
        binding.setSettings(SettingsFragment.mViewModel);
        binding.setLayout(mData.get(position));

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}

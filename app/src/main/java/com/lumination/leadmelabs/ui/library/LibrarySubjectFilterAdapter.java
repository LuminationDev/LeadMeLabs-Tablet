package com.lumination.leadmelabs.ui.library;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.ItemLayoutFilterAllBinding;
import com.lumination.leadmelabs.databinding.ItemLayoutFilterBinding;
import com.lumination.leadmelabs.models.applications.information.TagConstants;
import com.lumination.leadmelabs.ui.pages.LibraryPageFragment;

import java.util.HashMap;
import java.util.List;

public class LibrarySubjectFilterAdapter extends BaseAdapter {
    private final Context mContext;
    private final LifecycleOwner mLifecycleOwner;
    private final List<String> mData;

    private static final HashMap<String, Integer> DEFAULT_TAG_DRAWABLE_MAP = new HashMap<>();

    // Initialize default tag drawable map
    static {
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.HASS, R.drawable.filter_icon_hass);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.MATHS, R.drawable.filter_icon_maths);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.SCIENCE, R.drawable.filter_icon_science);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.ARTS, R.drawable.filter_icon_arts);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.HEALTH_PE, R.drawable.filter_icon_health_pe);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.ENGLISH, R.drawable.filter_icon_english);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.DESIGN_TECH, R.drawable.filter_icon_design_tech);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.LANGUAGES, R.drawable.filter_icon_languages);
    }

    public LibrarySubjectFilterAdapter(Context context, List<String> data, LifecycleOwner lifecycleOwner) {
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
        LayoutInflater inflater = LayoutInflater.from(mContext);

        if (position == 0) {
            // Inflate layout for special item
            ItemLayoutFilterAllBinding specialBinding = ItemLayoutFilterAllBinding.inflate(inflater, parent, false);
            specialBinding.setLibrary(LibraryPageFragment.mViewModel);
            specialBinding.setLifecycleOwner(mLifecycleOwner);
            convertView = specialBinding.getRoot();
        } else {
            // Inflate layout for normal items
            ItemLayoutFilterBinding binding = ItemLayoutFilterBinding.inflate(inflater, parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);

            binding.setLibrary(LibraryPageFragment.mViewModel);
            binding.setFilter(mData.get(position));

            Integer backgroundResource = DEFAULT_TAG_DRAWABLE_MAP.get(mData.get(position));
            if (backgroundResource != null) {
                binding.setFilterIcon(ContextCompat.getDrawable(mContext, backgroundResource));
            } else {
                binding.setFilterIcon(ContextCompat.getDrawable(mContext, R.drawable.filter_icon_design_tech));
            }

            binding.setLifecycleOwner(mLifecycleOwner);
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}

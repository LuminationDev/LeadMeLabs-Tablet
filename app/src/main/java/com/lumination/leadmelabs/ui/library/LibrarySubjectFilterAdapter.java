package com.lumination.leadmelabs.ui.library;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.ItemLayoutFilterBinding;
import com.lumination.leadmelabs.models.applications.information.TagConstants;
import com.lumination.leadmelabs.ui.pages.LibraryPageFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LibrarySubjectFilterAdapter extends BaseAdapter {
    private final Context mContext;
    private final LifecycleOwner mLifecycleOwner;
    private final HashMap<String, ArrayList<String>> mData;

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
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.SIMPLE, R.drawable.grey_difficulty_simple);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.INTERMEDIATE, R.drawable.grey_difficulty_intermediate);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.COMPLEX, R.drawable.grey_difficulty_advanced);
    }

    public LibrarySubjectFilterAdapter(Context context, HashMap<String, ArrayList<String>> data, LifecycleOwner lifecycleOwner) {
        mContext = context;
        mData = data;
        mLifecycleOwner = lifecycleOwner;
    }

    @Override
    public int getCount() {
        AtomicInteger size = new AtomicInteger();
        mData.forEach((key, value) -> {
            size.set(size.get() + value.size());
        });
        return size.get();
    }

    @Override
    public Object getItem(int position) {
        AtomicInteger sizeOfPreviousLists = new AtomicInteger();
        AtomicReference<Pair<String, String>> returnValue = new AtomicReference<>(new Pair<>("", ""));
        mData.forEach((key, value) -> {
            if (!returnValue.get().first.isEmpty()) {
                return;
            }
            if (position <= (value.size() + sizeOfPreviousLists.get() - 1)) {
                returnValue.set(new Pair<>(key, value.get(position - sizeOfPreviousLists.get())));
            }
            sizeOfPreviousLists.addAndGet(value.size());
        });
        return returnValue.get();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);

        // Inflate layout for normal items
        ItemLayoutFilterBinding binding = ItemLayoutFilterBinding.inflate(inflater, parent, false);
        convertView = binding.getRoot();
        convertView.setTag(binding);

        binding.setLibrary(LibraryPageFragment.mViewModel);

        Pair<String, String> item = ((Pair<String, String>)getItem(position));

        if (position == 0) {
            binding.setHeading(item.first);
        } else {
            Pair<String, String> prev = ((Pair<String, String>)getItem(position - 1));
            if (!prev.first.equals(item.first)) {
                binding.setHeading(item.first);
            }
        }
        binding.setFilter(item.second);
        binding.setPosition(position);

        Integer backgroundResource = DEFAULT_TAG_DRAWABLE_MAP.get(item.second);
        if (backgroundResource != null) {
            binding.setFilterIcon(ContextCompat.getDrawable(mContext, backgroundResource));
        } else {
            binding.setFilterIcon(ContextCompat.getDrawable(mContext, R.drawable.filter_icon_design_tech));
        }

        binding.setLifecycleOwner(mLifecycleOwner);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}

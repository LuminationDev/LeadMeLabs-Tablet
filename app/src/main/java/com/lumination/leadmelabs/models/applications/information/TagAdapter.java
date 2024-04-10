package com.lumination.leadmelabs.models.applications.information;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardTagBinding;

import java.util.HashMap;
import java.util.List;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {
    private final List<String> tags;
    private static final HashMap<String, Integer> DEFAULT_TAG_DRAWABLE_MAP = new HashMap<>();

    // Initialize default tag drawable map
    static {
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.HASS, R.drawable.tag_yellow_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.MATHS, R.drawable.tag_blue_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.SCIENCE, R.drawable.tag_green_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.ARTS, R.drawable.tag_purple_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.HEALTH_PE, R.drawable.tag_orange_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.ENGLISH, R.drawable.tag_rose_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.DESIGN_TECH, R.drawable.tag_teal_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.LANGUAGES, R.drawable.tag_fuchsia_curved);
    }

    public TagAdapter(List<String> tags) {
        this.tags = tags;
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        CardTagBinding binding = CardTagBinding.inflate(layoutInflater, parent, false);
        return new TagViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        String tag = tags.get(position);
        holder.bind(tag);
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public static class TagViewHolder extends RecyclerView.ViewHolder {
        private final CardTagBinding binding;
        private final Context context;

        TagViewHolder(CardTagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
        }

        void bind(String tag) {
            binding.setTag(tag);

            // Assign background drawable resource based on tag type
            Integer backgroundResource = DEFAULT_TAG_DRAWABLE_MAP.get(tag);
            if (backgroundResource != null) {
                binding.setBackgroundResource(ContextCompat.getDrawable(context, backgroundResource));
            } else {
                binding.setBackgroundResource(ContextCompat.getDrawable(context, R.drawable.tag_default_curved));
            }
        }
    }
}

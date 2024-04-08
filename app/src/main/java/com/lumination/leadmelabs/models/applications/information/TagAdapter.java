package com.lumination.leadmelabs.models.applications.information;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardTagBinding;

import java.util.List;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {
    private final List<String> tags;

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

        TagViewHolder(CardTagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String tag) {
            binding.setTag(tag);

            //Assign the background 'color' based on the tag type
            int backgroundResource;

            switch (tag) {
                case TagConstants.HASS:
                    backgroundResource = R.drawable.tag_yellow_curved;
                    break;

                case TagConstants.MATHS:
                    backgroundResource = R.drawable.tag_blue_curved;
                    break;

                case TagConstants.SCIENCE:
                    backgroundResource = R.drawable.tag_green_curved;
                    break;

                case TagConstants.ART:
                    backgroundResource = R.drawable.tag_purple_curved;
                    break;

                case TagConstants.HEALTH_PE:
                    backgroundResource = R.drawable.tag_orange_curved;
                    break;

                case TagConstants.ENGLISH:
                    backgroundResource = R.drawable.tag_rose_curved;
                    break;

                case TagConstants.DESIGN_TECH:
                    backgroundResource = R.drawable.tag_teal_curved;
                    break;

                case TagConstants.LANGUAGES:
                    backgroundResource = R.drawable.tag_fuchsia_curved;
                    break;

                case TagConstants.DEFAULT:
                default:
                    backgroundResource = R.drawable.tag_default_curved;
            }

            binding.setBackgroundResource(ContextCompat.getDrawable(binding.getRoot().getContext(), backgroundResource));
        }
    }
}

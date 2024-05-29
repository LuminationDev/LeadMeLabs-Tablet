package com.lumination.leadmelabs.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;

import java.util.List;

public class NotificationImageAdapter extends RecyclerView.Adapter<NotificationImageAdapter.CardViewHolder> {

    private final String title;
    private final List<String> texts;
    private final List<String> imageUrls;

    public NotificationImageAdapter(String title, List<String> texts, List<String> imageUrls) {
        this.title = title;
        this.texts = texts;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout_notification_text_images, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        if (imageUrls.isEmpty()) {
            holder.defaultImageView.setVisibility(View.VISIBLE);
            holder.imageContainer.setVisibility(View.GONE);
        } else {
            holder.defaultImageView.setVisibility(View.GONE);
            holder.imageContainer.setVisibility(View.VISIBLE);

            String imageUrl = position < imageUrls.size() ? imageUrls.get(position) : imageUrls.get(imageUrls.size() - 1);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .into((ImageView) holder.imageContainer.findViewById(R.id.supplied_image));
        }

        holder.titleView.setText(title);
        String text = position < texts.size() ? texts.get(position) : texts.get(texts.size() - 1);
        holder.textView.setText(text);
    }

    @Override
    public int getItemCount() {
        return Math.max(texts.size(), imageUrls.size());
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView defaultImageView;
        FlexboxLayout imageContainer;
        TextView titleView;
        TextView textView;

        CardViewHolder(View itemView) {
            super(itemView);
            defaultImageView = itemView.findViewById(R.id.default_image);
            imageContainer = itemView.findViewById(R.id.supplied_image_container);
            titleView = itemView.findViewById(R.id.title);
            textView = itemView.findViewById(R.id.content_text);
        }
    }
}

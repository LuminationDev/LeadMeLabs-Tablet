package com.lumination.leadmelabs.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;

import java.util.List;

public class NotificationTextAdapter extends RecyclerView.Adapter<NotificationTextAdapter.CardViewHolder> {

    private final String title;
    private final List<String> texts;

    public NotificationTextAdapter(String title, List<String> texts) {
        this.title = title;
        this.texts = texts;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout_notification_text, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        holder.titleView.setText(title);
        String text = texts.get(position);
        holder.textView.setText(text);
    }

    @Override
    public int getItemCount() {
        return texts.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView textView;

        CardViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.title);
            textView = itemView.findViewById(R.id.content_text);
        }
    }
}

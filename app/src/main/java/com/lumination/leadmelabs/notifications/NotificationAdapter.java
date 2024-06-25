package com.lumination.leadmelabs.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.Notification;
import com.lumination.leadmelabs.utilities.Constants;

import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private CopyOnWriteArrayList<Notification> notifications;

    public NotificationAdapter(CopyOnWriteArrayList<Notification> notifications) {
        this.notifications = notifications;
    }

    public void setObjects(CopyOnWriteArrayList<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout_notification, parent, false);
        return new NotificationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification object = notifications.get(position);
        holder.bind(object);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView messageTextView;
        private final TextView statusTextView;
        private final FlexboxLayout statusTextContainer;
        private final TextView timeStampTextView;
        private final FlexboxLayout showNotification;

        public NotificationViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            statusTextContainer = itemView.findViewById(R.id.statusTextContainer);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            timeStampTextView = itemView.findViewById(R.id.timeStampTextView);
            showNotification = itemView.findViewById(R.id.show_notification_button);
        }

        public void bind(Notification object) {
            String message = String.join(" ", object.getMessages());

            titleTextView.setText(object.getTitle());
            messageTextView.setText(message);

            //Update the UI for the status container
            if (object.getStatus().equals(Constants.STATUS_ACCEPTED)) {
                statusTextContainer.setBackgroundResource(R.drawable.bg_curved_green);
                statusTextView.setTextColor(ContextCompat.getColor(MainActivity.getInstance(), R.color.green_medium));
            } else {
                statusTextContainer.setBackgroundResource(R.drawable.bg_curved_gray);
                statusTextView.setTextColor(ContextCompat.getColor(MainActivity.getInstance(), R.color.grey_dark));
            }
            statusTextView.setText(object.getStatus());
            timeStampTextView.setText(object.transformDateFormat());
            showNotification.setOnClickListener(v -> {
                FirebaseManager.createEmergencyNotification(object.getKey(), object, true);
            });
        }
    }
}

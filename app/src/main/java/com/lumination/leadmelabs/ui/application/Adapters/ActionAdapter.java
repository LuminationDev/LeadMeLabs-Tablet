package com.lumination.leadmelabs.ui.application.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.applications.details.Actions;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.application.ApplicationAdapter;

import java.util.ArrayList;

public class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.ViewHolder> {
    private ArrayList<Actions> mData;
    private boolean mIsExpanded;

    public ActionAdapter(ArrayList<Actions> data) {
        mData = data;
        mIsExpanded = false;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout_action, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Actions action = mData.get(position);
        holder.textView.setText(action.name);
        holder.textView.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + ApplicationAdapter.stationId, "Experience", "PassToExperience:" + action.trigger);
        });
    }

    @Override
    public int getItemCount() {
        if (mIsExpanded) {
            return mData.size();
        } else {
            return 0;
        }
    }

    /**
     * Toggle the visibility of the attached data set.
     */
    public void toggleExpanded() {
        mIsExpanded = !mIsExpanded;
        notifyDataSetChanged();
    }

    public boolean getExpanded() {
        return this.mIsExpanded;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialButton textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.action_button);
        }
    }
}

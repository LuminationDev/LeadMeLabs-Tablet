package com.lumination.leadmelabs.ui.library.application.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.applications.details.Actions;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.LibraryPageFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class GlobalAdapter extends RecyclerView.Adapter<GlobalAdapter.ViewHolder> {
    private ArrayList<Actions> mData;
    public GlobalAdapter(ArrayList<Actions> data) {
        mData = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout_global_action, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Actions action = mData.get(position);
        holder.textView.setText(action.name);
        holder.textView.setOnClickListener(v -> {

            //BACKWARDS COMPATIBILITY - JSON Messaging system with fallback
            if (MainActivity.isNucJsonEnabled) {
                JSONObject message = new JSONObject();
                try {
                    message.put("Action", "PassToExperience");
                    message.put("Trigger", action.trigger);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                NetworkService.sendMessage("Station," + LibraryPageFragment.getStationId(), "Experience", message.toString());
            }
            else {
                NetworkService.sendMessage("Station," + LibraryPageFragment.getStationId(), "Experience", "PassToExperience:" + action.trigger);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialButton textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.action_button);
        }
    }
}

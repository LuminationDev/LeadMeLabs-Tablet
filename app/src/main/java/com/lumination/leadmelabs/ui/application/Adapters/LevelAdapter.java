package com.lumination.leadmelabs.ui.application.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.applications.details.Levels;

import java.util.ArrayList;

public class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.ViewHolder> {
    private ArrayList<Levels> mData;

    public LevelAdapter(ArrayList<Levels> data) {
        mData = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout_level, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Levels level = mData.get(position);
        holder.textView.setText(level.name);

        holder.subActionRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.getInstance().getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        ActionAdapter actionAdapter = new ActionAdapter(level.getActions());
        holder.subActionRecyclerView.setAdapter(actionAdapter);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        MaterialButton expandView;
        RecyclerView subActionRecyclerView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.action_button);
            expandView = itemView.findViewById(R.id.expand_view);
            subActionRecyclerView = itemView.findViewById(R.id.sub_action_recycler_view);

            expandView.setOnClickListener(v -> {
                ActionAdapter subActionAdapter = (ActionAdapter) subActionRecyclerView.getAdapter();
                if (subActionAdapter != null) {
                    subActionAdapter.toggleExpanded();

                    //Flip the arrow
                    expandView.setIcon(subActionAdapter.getExpanded() ?
                            ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.icon_circle_minus, null) :
                            ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.icon_circle_plus, null)
                    );
                }
            });
        }
    }
}

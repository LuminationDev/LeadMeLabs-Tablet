package com.lumination.leadmelabs.ui.appliance;

import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.ui.appliance.adapters.ApplianceAdapter;
import com.lumination.leadmelabs.ui.appliance.adapters.BaseAdapter;
import com.lumination.leadmelabs.ui.appliance.adapters.RadioAdapter;
import com.lumination.leadmelabs.ui.appliance.adapters.SceneAdapter;
import com.lumination.leadmelabs.utilities.Constants;

import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * Holds multiple ApplianceAdapters - creating a nested adapter allowing for rows of recycler views
 * that can be navigated horizontally to see all appliance cards and vertically for all the different
 * rows (which represent rooms)
 */
public class ApplianceParentAdapter extends RecyclerView.Adapter<ApplianceParentAdapter.ParentViewHolder> {
    public static ApplianceParentAdapter instance;
    public static ApplianceParentAdapter getInstance() { return instance; }

    public ArrayMap<String, ArrayList<Appliance>> parentModelArrayList;
    public ArrayList<BaseAdapter> adapters = new ArrayList<>();

    public static class ParentViewHolder extends RecyclerView.ViewHolder {
        public TextView category;
        public TextView warningMessage;
        public RecyclerView childRecyclerView;

        public ParentViewHolder(View itemView) {
            super(itemView);

            category = itemView.findViewById(R.id.room_type);
            warningMessage = itemView.findViewById(R.id.warning_message);
            childRecyclerView = itemView.findViewById(R.id.Child_RV);
        }
    }

    public ApplianceParentAdapter(ArrayMap<String, ArrayList<Appliance>> applianceList) {
        this.parentModelArrayList = applianceList;
        instance = this;
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.multi_recycler_layout_appliance, parent, false);
        return new ParentViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return parentModelArrayList.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ParentViewHolder holder, int position) {
        if (parentModelArrayList.size() <= position) {
            holder.warningMessage.setText(MessageFormat.format("There are no {0} in this room.", ApplianceFragment.type.getValue()));
            holder.warningMessage.setVisibility(View.VISIBLE);
            return;
        }
        String currentItem = parentModelArrayList.keyAt(position);
        holder.category.setText(currentItem);
        if(parentModelArrayList.valueAt(position).isEmpty()) {
            holder.warningMessage.setText(MessageFormat.format("There are no {0} in {1}.", ApplianceFragment.type.getValue(), currentItem));
            holder.warningMessage.setVisibility(View.VISIBLE);
            return;
        }
        BaseAdapter adapter = getBaseAdapter(position);
        holder.childRecyclerView.setAdapter(adapter);

        adapters.add(adapter);
    }

    @NonNull
    private BaseAdapter getBaseAdapter(int position) {
        String type = parentModelArrayList.valueAt(position).get(0).type;

        BaseAdapter applianceAdapter;
        switch(type) {
            case Constants.LED_WALLS:
                applianceAdapter = new RadioAdapter();
                break;

            case Constants.SCENE:
                applianceAdapter = new SceneAdapter();
                break;

            default:
                applianceAdapter = new ApplianceAdapter();
                break;
        }
        applianceAdapter.setApplianceList(parentModelArrayList.valueAt(position));
        return applianceAdapter;
    }

    /**
     * Cycle through the associated adapters and reload the related card with the supplied ID.
     */
    public void updateIfVisible(String id) {
        for (BaseAdapter adapter : adapters) {
            adapter.updateIfVisible(id);
        }
    }
}

package com.lumination.leadmelabs.ui.appliance;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.Appliance;

import java.util.ArrayList;

/**
 * Use this adapter for scripts in the future. Acts as a singleton when individual rooms are
 * displayed. When 'All' rooms are chosen it acts as a regular class access through references in
 * the parent adapter class.
 */
public abstract class BaseAdapter extends RecyclerView.Adapter<BaseAdapter.BaseViewHolder> {
    public static BaseAdapter instance;
    public static BaseAdapter getInstance() { return instance; }

    public BaseAdapter() {
        instance = this;
    }

    public abstract ArrayList<Appliance> getApplianceList();
    public abstract void setApplianceList(ArrayList<Appliance> newAppliances);

    public abstract class BaseViewHolder extends RecyclerView.ViewHolder {
        private final View binding;
        private boolean recentlyClicked = false;

        public BaseViewHolder(@NonNull View binding) {
            super(binding);
            this.binding = binding;
        }

        public abstract void bind(Appliance appliance);
    }

    @NonNull
    @Override
    public abstract BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override
    public abstract void onBindViewHolder(@NonNull BaseViewHolder holder, int position);

    //Accessors
    @Override
    public int getItemCount() {
        return getApplianceList() != null ? getApplianceList().size() : 0;
    }

    public Appliance getItem(int position) {
        return getApplianceList().get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    /**
     * Update the data set only if the card with the supplied ID is visible otherwise it will be
     * update when it is next visible automatically with the new ViewHolder creation.
     * @param id A string representing the ID of the appliance.
     */
    public void updateIfVisible(String id) {
        for(int i=0; i < getApplianceList().size(); i++) {
            if(getApplianceList().get(i).id.equals(id)) {
                int finalI = i;
                MainActivity.runOnUI(() ->
                        notifyItemChanged(finalI)
                );
            }
        }
    }
}

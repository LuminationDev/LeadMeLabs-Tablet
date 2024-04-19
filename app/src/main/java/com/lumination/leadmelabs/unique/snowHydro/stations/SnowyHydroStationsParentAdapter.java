package com.lumination.leadmelabs.unique.snowHydro.stations;

import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.stations.Station;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class SnowyHydroStationsParentAdapter extends RecyclerView.Adapter<SnowyHydroStationsParentAdapter.ParentViewHolder> {
    public static SnowyHydroStationsParentAdapter instance;
    private final FragmentManager fragmentManager;
    public static SnowyHydroStationsParentAdapter getInstance() { return instance; }

    public TreeMap<String, ArrayList<Station>> parentModelArrayList;
    public ArrayList<SnowyHydroStationAdapter> adapters = new ArrayList<>();

    public SnowyHydroStationsParentAdapter(FragmentManager fragmentManager, ArrayMap<String, ArrayList<Station>> parentModelArrayList) {
        this.fragmentManager = fragmentManager;
        this.parentModelArrayList = sortArrayMap(parentModelArrayList);
    }

    public void setParentModelArrayList(ArrayMap<String, ArrayList<Station>> parentModelArrayList) {
        this.parentModelArrayList = sortArrayMap(parentModelArrayList);
    }

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

    /**
     * Create a TreeMap with a custom comparator for reverse alphabetical order
     * @param map An ArrayMap containing different rooms and their Station lists.
     * @return An organised TreeMap in reverse alphabetical order.
     */
    public static TreeMap<String, ArrayList<Station>> sortArrayMap(ArrayMap<String, ArrayList<Station>> map) {
        TreeMap<String, ArrayList<Station>> sortedMap = new TreeMap<>(Collections.reverseOrder());
        sortedMap.putAll(map);
        return sortedMap;
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.multi_recycler_layout_station, parent, false);
        return new ParentViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return parentModelArrayList.size();
    }

    @Override
    public void onBindViewHolder(@NonNull SnowyHydroStationsParentAdapter.ParentViewHolder holder, int position) {
        if (parentModelArrayList.size() <= position) {
            holder.warningMessage.setText(MessageFormat.format("There are no {0} in this room.", "Stations"));
            holder.warningMessage.setVisibility(View.VISIBLE);
            return;
        }

        String currentItem = parentModelArrayList.entrySet().stream()
                .skip(position)
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);

        holder.category.setText(currentItem);
        if(parentModelArrayList.get(currentItem) == null || Objects.requireNonNull(parentModelArrayList.get(currentItem)).isEmpty()) {
            holder.warningMessage.setText(MessageFormat.format("There are no {0} in {1}.", "Stations", currentItem));
            holder.warningMessage.setVisibility(View.VISIBLE);
            return;
        }

        SnowyHydroStationAdapter snowyHydroStationAdapter = new SnowyHydroStationAdapter(fragmentManager);
        snowyHydroStationAdapter.setStationList(parentModelArrayList.get(currentItem));
        holder.childRecyclerView.setAdapter(snowyHydroStationAdapter);

        adapters.add(snowyHydroStationAdapter);
    }

    //TODO make it better
    /**
     * Cycle through the associated adapters and reload the related card with the supplied ID.
     */
    public void updateIfVisible() {
        for (SnowyHydroStationAdapter adapter : adapters) {
            adapter.notifyDataSetChanged();
        }
    }
}

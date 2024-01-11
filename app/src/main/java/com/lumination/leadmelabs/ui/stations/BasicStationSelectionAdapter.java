package com.lumination.leadmelabs.ui.stations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardStationVirtualBinding;
import com.lumination.leadmelabs.models.stations.VirtualStation;

import java.util.ArrayList;

public class BasicStationSelectionAdapter extends RecyclerView.Adapter<BasicStationSelectionAdapter.StationViewHolder> {
    private final String TAG = "StationAdapter";

    public ArrayList<CardStationVirtualBinding> stationBindings = new ArrayList<>();
    public ArrayList<VirtualStation> stationList = new ArrayList<>();

    public class StationViewHolder extends RecyclerView.ViewHolder {
        private final CardStationVirtualBinding binding;
        public StationViewHolder(@NonNull CardStationVirtualBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(VirtualStation station, BasicStationSelectionAdapter basicStationSelectionAdapter) {
            binding.setStation(station);
            View finalResult = binding.getRoot().findViewById(R.id.station_card);
            if (station.status.equals("Off")) {
                finalResult.setForeground(ContextCompat.getDrawable(finalResult.getContext(), R.drawable.bg_disabled));
            } else {
                finalResult.setOnClickListener(v -> {
                    station.selected = !station.selected;
                    basicStationSelectionAdapter.notifyDataSetChanged();
                });
            }
            stationBindings.add(binding);
        }
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        CardStationVirtualBinding binding = CardStationVirtualBinding.inflate(layoutInflater, parent, false);
        return new BasicStationSelectionAdapter.StationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        VirtualStation station = getItem(position);
        ((StationViewHolder) holder).bind(station, this);
    }

    @Override
    public int getItemCount() {
        return stationList != null ? stationList.size() : 0;
    }

    public VirtualStation getItem(int position) {
        return stationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return stationList.get(position).id;
    }
}
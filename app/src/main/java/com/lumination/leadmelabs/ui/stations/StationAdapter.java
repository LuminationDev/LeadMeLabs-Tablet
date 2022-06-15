package com.lumination.leadmelabs.ui.stations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.Station;
import androidx.core.content.ContextCompat;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.databinding.CardStationBinding;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;

import java.util.ArrayList;

public class StationAdapter extends RecyclerView.Adapter {
    private final String TAG = "StationAdapter";

    public ArrayList<CardStationBinding> stationBindings = new ArrayList<>();

    public ArrayList<Station> stationList = new ArrayList<>();
    private boolean launchSingleOnTouch = false;
    private final StationsViewModel viewModel;

    StationAdapter(StationsViewModel viewModel, boolean launchSingleOnTouch) {
        this.launchSingleOnTouch = launchSingleOnTouch;
        this.viewModel = viewModel;
    }

    public class StationViewHolder extends RecyclerView.ViewHolder {
        private final CardStationBinding binding;
        public StationViewHolder(@NonNull CardStationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Station station, int position) {
            binding.setStation(station);
            View finalResult = binding.getRoot();
            if (launchSingleOnTouch) {
                finalResult.setOnClickListener(v -> {
                    finalResult.setTransitionName("card_station");
                    viewModel.selectStation(position);
                    MainActivity.fragmentManager.beginTransaction()
                            .addSharedElement(finalResult, "card_station")
                            .replace(R.id.main, StationSingleFragment.class, null)
                            .addToBackStack("menu:dashboard:stationSingle")
                            .commit();
                    SideMenuFragment.currentType = "stationSingle";
                });
            } else {
                if (station.hasSteamApplicationInstalled(viewModel.getSelectedSteamApplicationId())) {
                    finalResult.setOnClickListener(v -> {
                        station.selected = !station.selected;
                        viewModel.updateStationById(station.id, station);
                    });
                } else {
                    finalResult.setForeground(ContextCompat.getDrawable(finalResult.getContext(), R.drawable.bg_disabled));
                    if (!station.status.equals("Off")) {
                        TextView infoText = finalResult.findViewById(R.id.card_info_text);
                        infoText.setText(R.string.game_not_installed);
                        infoText.setVisibility(View.VISIBLE);
                    }
                }
            }
            stationBindings.add(binding);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        CardStationBinding binding = CardStationBinding.inflate(layoutInflater, parent, false);
        return new StationAdapter.StationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Station station = getItem(position);
        StationAdapter.StationViewHolder stationViewHolder = (StationAdapter.StationViewHolder) holder;
        stationViewHolder.bind(station, position);
    }

    @Override
    public int getItemCount() {
        return stationList != null ? stationList.size() : 0;
    }

    public Station getItem(int position) {
        return stationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return stationList.get(position).id;
    }
}
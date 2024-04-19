package com.lumination.leadmelabs.unique.snowHydro.stations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardStationBinding;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.segment.analytics.Properties;

import java.util.ArrayList;

public class SnowyHydroStationAdapter extends RecyclerView.Adapter<SnowyHydroStationAdapter.StationViewHolder> {
    public ArrayList<CardStationBinding> stationBindings = new ArrayList<>();

    public ArrayList<Station> stationList = new ArrayList<>();

    private final FragmentManager fragmentManager;

    public SnowyHydroStationAdapter(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public void setStationList(ArrayList<Station> stationList) {
        this.stationList = stationList;
    }

    public class StationViewHolder extends RecyclerView.ViewHolder {
        private final CardStationBinding binding;

        public StationViewHolder(@NonNull CardStationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Station station) {
            View finalResult = Station.determineStationType(binding, station);

            if (finalResult == null) {
                return;
            }

            //Check if there are nested stations (meaning this must be the primary)
            Fragment fragment;
            if(station.nestedStations == null || station.nestedStations.isEmpty()) {
                fragment = new StationSingleFragment();
            } else {
                fragment = new StationSingleNestedFragment();
            }

            finalResult.setOnClickListener(v -> {
                finalResult.setTransitionName("card_station");
                SnowyHydroStationsFragment.mViewModel.selectStation(station.id);
                fragmentManager.beginTransaction()
                        .addSharedElement(finalResult, "card_station")
                        .setCustomAnimations(android.R.anim.fade_in,
                                android.R.anim.fade_out,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out)
                        .replace(R.id.main, fragment, null)
                        .addToBackStack("menu:dashboard:stationSingle")
                        .commit();
                SideMenuFragment.currentType = "stationSingle";
                Properties segmentProperties = new Properties();
                segmentProperties.put("classification", DashboardPageFragment.segmentClassification);
                Segment.trackEvent(SegmentConstants.Open_Station_Details, segmentProperties);
                Segment.trackScreen("menu:dashboard:stationSingle");
            });

            stationBindings.add(binding);
        }
    }

    @NonNull
    @Override
    public SnowyHydroStationAdapter.StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        CardStationBinding binding = CardStationBinding.inflate(layoutInflater, parent, false);
        return new SnowyHydroStationAdapter.StationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SnowyHydroStationAdapter.StationViewHolder holder, int position) {
        Station station = getItem(position);
        holder.bind(station);
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

package com.lumination.leadmelabs.ui.stations.adapters;

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

import androidx.core.content.ContextCompat;

import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.ui.dashboard.DashboardFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionPageFragment;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.unique.snowHydro.stations.StationSingleNestedFragment;
import com.segment.analytics.Properties;

import java.util.ArrayList;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationViewHolder> {
    public ArrayList<CardStationBinding> stationBindings = new ArrayList<>();

    public ArrayList<Station> stationList = new ArrayList<>();
    private final boolean launchSingleOnTouch;
    private final StationsViewModel mViewModel;
    private final FragmentManager fragmentManager;

    public StationAdapter(StationsViewModel viewModel, boolean launchSingleOnTouch, FragmentManager fragmentManager) {
        this.launchSingleOnTouch = launchSingleOnTouch;
        this.mViewModel = viewModel;
        this.fragmentManager = fragmentManager;
        this.setHasStableIds(true);
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

            if (launchSingleOnTouch) {
                //Check if there are nested stations (meaning this must be the primary)
                Fragment fragment;
                if(station.nestedStations == null || station.nestedStations.isEmpty()) {
                    fragment = new StationSingleFragment();
                } else {
                    fragment = new StationSingleNestedFragment();
                }

                finalResult.setOnClickListener(v -> {
                    finalResult.setTransitionName("card_station");
                    mViewModel.selectStation(station.id);
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
                    segmentProperties.put("classification", DashboardFragment.segmentClassification);
                    Segment.trackEvent(SegmentConstants.Open_Station_Details, segmentProperties);
                    Segment.trackScreen("menu:dashboard:stationSingle");
                });
            } else {
                String type = mViewModel.getSelectionType().getValue();
                type = type != null ? type : "application";

                boolean hasLocalApplication = type.equals("application") && station.applicationController.hasApplicationInstalled(mViewModel.getSelectedApplicationId());

                boolean hasLocalVideoAndPlayer = type.equals("video")
                        && station.fileController.hasLocalVideo(mViewModel.getSelectedVideo().getValue())
                        && station.applicationController.findApplicationByName(StationSelectionPageFragment.videoPlayerSelection) != null;

                if (hasLocalApplication || hasLocalVideoAndPlayer) {
                    finalResult.setOnClickListener(v -> {
                        station.selected = !station.selected;
                        mViewModel.updateStationById(station.id, station);
                    });
                } else {
                    finalResult.setForeground(ContextCompat.getDrawable(finalResult.getContext(), R.drawable.bg_disabled));
                    if (!station.isOff()) {
                        StationSelectionPageFragment fragment = (StationSelectionPageFragment) fragmentManager.findFragmentById(R.id.main);
                        View notInstalledAlert = fragment.getView().findViewById(R.id.not_installed_alert);
                        notInstalledAlert.setVisibility(View.VISIBLE);
                        finalResult.setForeground(ContextCompat.getDrawable(finalResult.getContext(), R.drawable.card_disabled_red_border));
                    }
                }
            }
            stationBindings.add(binding);
        }
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        CardStationBinding binding = CardStationBinding.inflate(layoutInflater, parent, false);
        return new StationAdapter.StationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
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

    /**
     * Detect if any stations do not have the selected application.
     * @return A boolean representing if the Experience is installed.
     */
    public boolean isApplicationInstalledOnAll() {
        for (Station station : stationList) {
            if(!station.applicationController.hasApplicationInstalled(mViewModel.getSelectedApplicationId())){
                return false;
            };
        }

        return true;
    }

    /**
     * Detect if any stations do not have the selected video and selected video player.
     * @return A boolean representing if the Video exists.
     */
    public boolean isVideoOnAll() {
        for (Station station : stationList) {
            if(!station.fileController.hasLocalVideo(mViewModel.getSelectedVideo().getValue())
                    || station.applicationController.findApplicationByName(StationSelectionPageFragment.videoPlayerSelection) == null)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Run through the Station and determine if they are all off.
     * @return A boolean representing if all the Stations are turned off.
     */
    public boolean areAllStationsOff() {
        for (Station station : stationList) {
            if(!station.isOff()){
                return false;
            };
        }

        return true;
    }
}

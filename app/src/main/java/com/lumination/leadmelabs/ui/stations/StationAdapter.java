package com.lumination.leadmelabs.ui.stations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardStationBinding;
import com.lumination.leadmelabs.databinding.CardStationContentBinding;
import com.lumination.leadmelabs.databinding.CardStationVirtualBinding;
import com.lumination.leadmelabs.models.stations.ContentStation;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.stations.VirtualStation;
import androidx.core.content.ContextCompat;

import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;

import java.util.ArrayList;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationViewHolder> {
    private final String TAG = "StationAdapter";

    public ArrayList<CardStationBinding> stationBindings = new ArrayList<>();

    public ArrayList<Station> stationList = new ArrayList<>();
    private boolean launchSingleOnTouch = false;
    private final StationsViewModel viewModel;
    private FragmentManager fragmentManager;

    public StationAdapter(StationsViewModel viewModel, boolean launchSingleOnTouch, FragmentManager fragmentManager) {
        this.launchSingleOnTouch = launchSingleOnTouch;
        this.viewModel = viewModel;
        this.fragmentManager = fragmentManager;
    }

    public class StationViewHolder extends RecyclerView.ViewHolder {
        private final CardStationBinding binding;

        public StationViewHolder(@NonNull CardStationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Station station, int position) {
            View finalResult = determineStationType(binding, station);

            if (finalResult == null) {
                return;
            }

            if (launchSingleOnTouch) {
                finalResult.setOnClickListener(v -> {
                    finalResult.setTransitionName("card_station");
                    viewModel.selectStation(station.id);
                    fragmentManager.beginTransaction()
                            .addSharedElement(finalResult, "card_station")
                            .setCustomAnimations(android.R.anim.fade_in,
                                    android.R.anim.fade_out,
                                    android.R.anim.fade_in,
                                    android.R.anim.fade_out)
                            .replace(R.id.main, StationSingleFragment.class, null)
                            .addToBackStack("menu:dashboard:stationSingle")
                            .commit();
                    SideMenuFragment.currentType = "stationSingle";
                });
            } else {
                if (station.hasApplicationInstalled(viewModel.getSelectedApplicationId())) {
                    finalResult.setOnClickListener(v -> {
                        station.selected = !station.selected;
                        viewModel.updateStationById(station.id, station);
                    });
                } else {
                    finalResult.setForeground(ContextCompat.getDrawable(finalResult.getContext(), R.drawable.bg_disabled));
                    if (!station.status.equals("Off")) {
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

    /**
     * Determines the type of the given station (VirtualStation or ContentStation) and binds the corresponding
     * data to the associated layout. It sets the visibility of the relevant layout to VISIBLE and returns
     * the root view of the card associated with the station type.
     *
     * @param binding The data binding object for the card station layout.
     * @param station The station for which the type needs to be determined and data bound.
     * @return The root view of the card associated with the station type, or null if the station type is unknown.
     */
    private View determineStationType(CardStationBinding binding, Station station) {
        if (station instanceof VirtualStation) {
            CardStationVirtualBinding virtualBinding = binding.cardStationVirtual;
            virtualBinding.setStation((VirtualStation) station);
            virtualBinding.getRoot().setVisibility(View.VISIBLE);
            return binding.cardStationVirtual.getRoot().findViewById(R.id.station_card);

        } else if (station instanceof ContentStation) {
            CardStationContentBinding classicBinding = binding.cardStationContent;
            classicBinding.setStation((ContentStation) station);
            classicBinding.getRoot().setVisibility(View.VISIBLE);
            return binding.cardStationContent.getRoot().findViewById(R.id.station_card);

        } else {
            return null;
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
        holder.bind(station, position);
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
     * Detect if any stations do not have the selected steam application.
     * @return A boolean representing if the Steam Experience is installed.
     */
    public boolean isApplicationInstalledOnAll() {
        for (Station station : stationList) {
            if(!station.hasApplicationInstalled(viewModel.getSelectedApplicationId())){
                return false;
            };
        }

        return true;
    }

    /**
     * Run through the Station and determine if they are all off.
     * @return A boolean representing if all the Stations are turned off.
     */
    public boolean areAllStationsOff() {
        for (Station station : stationList) {
            if(!station.status.equals("Off")){
                return false;
            };
        }

        return true;
    }
}

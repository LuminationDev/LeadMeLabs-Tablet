package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;

import java.util.ArrayList;
import java.util.List;

public class StationSelectionFragment extends Fragment {

    public static StationsViewModel mViewModel;
    public StationAdapter stationAdapter;

    public static StationSelectionFragment instance;
    public static StationSelectionFragment getInstance() { return instance; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class);
        return inflater.inflate(R.layout.fragment_station_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.stations_list);
        stationAdapter = new StationAdapter(mViewModel, false);
        stationAdapter.stationList = new ArrayList<>();
        recyclerView.setAdapter(stationAdapter);

        mViewModel.getStations().observe(getViewLifecycleOwner(), this::reloadData);

        instance = this;
    }

    /**
     * Reload the current appliance list when a room is changed.
     */
    public void notifyDataChange() {
        StationSelectionPageFragment.childManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.station_selection_list_container, StationSelectionFragment.class, null)
                .commitNow();
    }

    private void reloadData(List<Station> stations) {
        ArrayList<Station> stationRoom = new ArrayList<>();

        String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();
        if(roomType == null) {
            roomType = "All";
        }

        for(Station station : stations) {
            if(roomType.equals("All")) {
                if(SettingsFragment.checkLockedRooms(station.room)) {
                    stationRoom.add(station);
                }
            } else if(station.room.equals(roomType)) {
                stationRoom.add(station);
            }
        }

        stationAdapter.stationList = stationRoom;
        stationAdapter.notifyDataSetChanged();

        changeWarningLabel(stationRoom.size());
    }

    /**
     * Change the warning label on the Station selection page to reflect if there is a game that is
     * not installed or if there are no stations available in a selected room.
     * @param numOfStations A integer representing how many stations are present in the currently
     *                      selected room.
     */
    private void changeWarningLabel(int numOfStations) {
        StationSelectionPageFragment fragment = (StationSelectionPageFragment) MainActivity.fragmentManager.findFragmentById(R.id.main);
        Button playBtn = fragment.getView().findViewById(R.id.select_stations);
        TextView top = fragment.getView().findViewById(R.id.not_installed_alert_top_line);

        View container = fragment.getView().findViewById(R.id.not_installed_alert);
        container.setVisibility(stationAdapter.isApplicationInstalledOnAll() && numOfStations > 0 ? View.GONE : View.VISIBLE);

        TextView bottom = fragment.getView().findViewById(R.id.not_installed_alert_bottom_line);
        bottom.setVisibility((numOfStations == 0 || stationAdapter.areAllStationsOff()) ? View.GONE : View.VISIBLE);

        TextView noStationsAvailable = getView().findViewById(R.id.no_stations_available);
        noStationsAvailable.setVisibility(numOfStations == 0 ? View.VISIBLE : View.GONE);

        if(numOfStations == 0) { //No stations in the room
            container.setBackground(ContextCompat.getDrawable(MainActivity.getInstance(), R.drawable.bg_mustard_rounded));
            top.setText(R.string.no_stations_available);
            playBtn.setBackgroundColor(ContextCompat.getColor(MainActivity.getInstance(), R.color.grey_medium));

        } else if(stationAdapter.areAllStationsOff()) { //All stations are turned off
            container.setBackground(ContextCompat.getDrawable(MainActivity.getInstance(), R.drawable.bg_mustard_rounded));
            top.setText(R.string.all_computers_off);
            playBtn.setBackgroundColor(ContextCompat.getColor(MainActivity.getInstance(), R.color.grey_medium));

        } else { //Application not installed on some Station
            container.setBackground(ContextCompat.getDrawable(MainActivity.getInstance(), R.drawable.bg_red_lightest_rounded));
            top.setText(R.string.not_installed_on_stations);
            playBtn.setBackgroundColor(ContextCompat.getColor(MainActivity.getInstance(), R.color.blue));
        }

        playBtn.setEnabled(numOfStations != 0 || stationAdapter.areAllStationsOff());
    }

    public ArrayList<Station> getRoomStations()
    {
        return stationAdapter.stationList;
    }
}
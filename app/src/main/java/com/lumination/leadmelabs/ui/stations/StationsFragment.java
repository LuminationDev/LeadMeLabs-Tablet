package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationsBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;

import java.util.ArrayList;
import java.util.List;

public class StationsFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private StationAdapter stationAdapter;
    private FragmentStationsBinding binding;

    public static StationsFragment instance;
    public static StationsFragment getInstance() { return instance; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stations, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.stations_list);
        stationAdapter = new StationAdapter(mViewModel, true);
        stationAdapter.stationList = new ArrayList<>();
        binding.setStationList(stationAdapter.stationList);
        recyclerView.setAdapter(stationAdapter);

        mViewModel.getStations().observe(getViewLifecycleOwner(), this::reloadData);

        instance = this;
    }

    /**
     * Reload the current appliance list when a room is changed.
     */
    public void notifyDataChange() {
        mViewModel.getStations().observe(getViewLifecycleOwner(), this::reloadData);
    }

    private void reloadData(List<Station> stations) {
        ArrayList<Station> stationRoom = new ArrayList<>();

        String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();
        if(roomType == null) {
            roomType = "All";
        }

        for(Station station : stations) {
            if(roomType.equals("All")) {
                stationRoom.add(station);
            } else if(station.room.equals(roomType)) {
                stationRoom.add(station);
            }
        }

        stationAdapter.stationList = stationRoom;
        stationAdapter.notifyDataSetChanged();
        binding.setStationList(stationAdapter.stationList);
    }

    public ArrayList<Station> getRoomStations()
    {
        return stationAdapter.stationList;
    }
}
package com.lumination.leadmelabs.unique.snowHydro.stations;

import android.os.Bundle;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationsSnowyHydroBinding;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

//TODO Investigate handling Station updates better
public class SnowyHydroStationsFragment extends Fragment {
    public static StationsViewModel mViewModel;
    private SnowyHydroStationsParentAdapter stationParentAdapter;
    private FragmentStationsSnowyHydroBinding binding;

    public static int stationNumber;
    public static ArrayMap<String, ArrayList<Station>> rooms = new ArrayMap<>();

    public static SnowyHydroStationsFragment instance;
    public static SnowyHydroStationsFragment getInstance() { return instance; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stations_snowy_hydro, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            RecyclerView parentRecyclerView = view.findViewById(R.id.multi_recyclerView);
            parentRecyclerView.setVisibility(View.VISIBLE);
            parentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            // Sort the available Stations into rooms - only collect rooms with Stations
            rooms = loadData();

            stationParentAdapter = new SnowyHydroStationsParentAdapter(requireActivity().getSupportFragmentManager(), rooms);

            binding.setLifecycleOwner(getViewLifecycleOwner());
            binding.setStationsLoaded(false);
            binding.setStationsAvailable(!rooms.isEmpty());

            parentRecyclerView.setAdapter(stationParentAdapter);
        }

        mViewModel.getStations().observe(getViewLifecycleOwner(), this::reloadData);
        instance = this;
    }

    /**
     * Populates the rooms map with stations and removes rooms without any associated stations.
     * Retrieves the list of rooms from the RoomViewModel and initializes the rooms map,
     * excluding the 'All' room.
     *
     * @return An ArrayMap of Rooms and Station lists.
     */
    private ArrayMap<String, ArrayList<Station>> loadData() {
        // Initialize rooms map
        ArrayMap<String, ArrayList<Station>> temp = new ArrayMap<>();

        // Retrieve rooms from RoomViewModel
        HashSet<String> roomList = RoomFragment.mViewModel.getRooms().getValue();

        // Populate rooms map excluding 'All' room
        if (roomList != null) {
            for (String room : roomList) {
                if (!room.equals("All")) {
                    temp.put(room, new ArrayList<>());
                }
            }
        }

        List<Station> stations = mViewModel.getStations().getValue();
        stations = getStationsByRoom(stations);

        if (stations.isEmpty()) {
            new ArrayMap<>();
        } else {
            stationNumber = stations.size();

            // Populate rooms with stations
            for (Station station : stations) {
                ArrayList<Station> roomStations = temp.get(station.room);
                if (roomStations != null) {
                    roomStations.add(station);
                }
            }

            // Remove rooms with no associated stations
            temp.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }

        return temp;
    }

    /**
     * Reload the current station list when a room is changed.
     */
    public void notifyDataChange() {
        FragmentTransaction transactionAttempt = DashboardPageFragment.childManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.stations, SnowyHydroStationsFragment.class, null);

        transactionAttempt.commitNowAllowingStateLoss();
    }

    private ArrayList<Station> getStationsByRoom(List<Station> stations) {
        ArrayList<Station> stationRoom = new ArrayList<>();
        if (stations == null) return stationRoom;

        String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();
        if(roomType == null) {
            roomType = "All";
        }

        for(Station station : stations) {
            // Do not show hidden stations
            if (station.getIsHidden()) {
                continue;
            }

            if(roomType.equals("All")) {
                if(SettingsFragment.checkLockedRooms(station.room)) {
                    stationRoom.add(station);
                }
            } else if(station.room.equals(roomType)) {
                stationRoom.add(station);
            }
        }

        return stationRoom;
    }

    private void reloadData(List<Station> stations) {
        stations = getStationsByRoom(stations);

        // No additional Stations have been found, only the individual adapter should update
        if (stations.size() == stationNumber) {
            //Update the child adapters
            stationParentAdapter.updateIfVisible();
            return;
        }

        // Compare the current 'rooms' with the newly loaded one and if there is a difference update
        // the adapter, if not the individual adapters will take care of detail updates. This is just
        // for the station lists.
        ArrayMap<String, ArrayList<Station>> temp = loadData();
        ArrayList<Integer> positions = checkDifferences(temp, rooms);

        rooms = temp;


        binding.setStationsLoaded(true);
        binding.setStationsAvailable(!rooms.isEmpty());

        stationParentAdapter.setParentModelArrayList(rooms);

        // Only update the adapters that have changed
        for (Integer position: positions) {
            stationParentAdapter.notifyItemChanged(position);
        }
    }

    /**
     * Compares two ArrayMaps containing ArrayLists of Stations and returns a list of positions
     * where the sizes of corresponding ArrayLists differ or where a list is missing in the original map.
     *
     * @param newMap The first ArrayMap to compare.
     * @param original The second ArrayMap to compare.
     * @return A list of positions where differences were found.
     */
    private ArrayList<Integer> checkDifferences(ArrayMap<String, ArrayList<Station>> newMap, ArrayMap<String, ArrayList<Station>> original) {
        ArrayList<Integer> positions = new ArrayList<>();

        int position = 0;

        // Iterate over entries of map1
        for (Map.Entry<String, ArrayList<Station>> entry : newMap.entrySet()) {
            String key = entry.getKey();
            ArrayList<Station> newList = entry.getValue();

            // Get corresponding list from map2
            ArrayList<Station> originalList = original.get(key);

            // Check if both lists exist and have different sizes
            if (originalList == null) {
                positions.add(position);
                position++;
                continue;
            }

            if (newList.size() != originalList.size()) {
                positions.add(position);
                position++;
            }
        }
        return positions;
    }
}

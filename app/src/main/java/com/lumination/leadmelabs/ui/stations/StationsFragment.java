package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationsBinding;
import com.lumination.leadmelabs.models.Station;

import java.util.ArrayList;

public class StationsFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private View view;
    private StationAdapter stationAdapter;
    private SteamApplicationAdapter steamApplicationAdapter;
    private FragmentStationsBinding binding;

    public static StationsFragment newInstance() {
        return new StationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_stations, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);
        GridView gridView = (GridView) view.findViewById(R.id.stations_list);
        stationAdapter = new StationAdapter(getContext(), mViewModel);
        stationAdapter.stationList = new ArrayList<>();
        gridView.setAdapter(stationAdapter);

        GridView steamGridView = (GridView) view.findViewById(R.id.steam_list);
        steamApplicationAdapter = new SteamApplicationAdapter(getContext());
        steamApplicationAdapter.steamApplicationList = new ArrayList<>();
        steamGridView.setAdapter(steamApplicationAdapter);

        mViewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
            stationAdapter.stationList = (ArrayList<Station>) stations;
            stationAdapter.notifyDataSetChanged();
        });
        mViewModel.getSelectedStation().observe(getViewLifecycleOwner(), station -> {
            binding.setSelectedStation(station);
            steamApplicationAdapter.steamApplicationList = station.steamApplications;
            steamApplicationAdapter.stationId = station.id;
            steamApplicationAdapter.notifyDataSetChanged();
        });
    }
}
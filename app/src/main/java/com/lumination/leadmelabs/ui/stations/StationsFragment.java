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
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;

import java.util.ArrayList;

public class StationsFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private View view;
    private StationAdapter stationAdapter;
    private FragmentStationsBinding binding;

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

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.stations_list);
        stationAdapter = new StationAdapter(mViewModel, true);
        stationAdapter.stationList = new ArrayList<>();
        recyclerView.setAdapter(stationAdapter);

        mViewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
            stationAdapter.stationList = (ArrayList<Station>) stations;
            stationAdapter.notifyDataSetChanged();
        });

        Button newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            SideMenuFragment.loadFragment(SteamSelectionFragment.class, "session");
            SteamSelectionFragment.setStationId(0);
        });
    }
}
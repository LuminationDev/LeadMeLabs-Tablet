package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.Station;

import java.util.ArrayList;

public class StationsFragment extends Fragment {

    private StationsViewModel mViewModel;
    private View view;
    private StationAdapter stationAdapter;

    public static StationsFragment newInstance() {
        return new StationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_stations, container, false);
        GridView gridView = (GridView) view.findViewById(R.id.stations_list);
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        stationAdapter = new StationAdapter(getContext(), gridView);
        stationAdapter.stationList = new ArrayList<>();
        gridView.setAdapter(stationAdapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);
        mViewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
            stationAdapter.stationList = (ArrayList<Station>) stations;
            stationAdapter.notifyDataSetChanged();
        });
    }
}
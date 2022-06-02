package com.lumination.leadmelabs.ui.logo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import java.util.Arrays;

public class LogoFragment extends Fragment {

    public static LogoViewModel mViewModel;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_logo, container, false);
        view.setOnClickListener(v -> pingAll());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void pingAll() {
        int[] selectedIds = new ViewModelProvider(requireActivity()).get(StationsViewModel.class).getAllStationIds();
        String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));

        //TODO stagger the messages
        NetworkService.sendMessage("Station," + stationIds, "CommandLine", "IdentifyStation");
    }
}
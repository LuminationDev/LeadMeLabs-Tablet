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
import com.lumination.leadmelabs.databinding.FragmentSteamSelectionBinding;
import com.lumination.leadmelabs.models.SteamApplication;

import java.util.ArrayList;

public class SteamSelectionFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private View view;
    private SteamApplicationAdapter steamApplicationAdapter;
    private FragmentSteamSelectionBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_steam_selection, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);

        GridView steamGridView = (GridView) view.findViewById(R.id.steam_list);
        steamApplicationAdapter = new SteamApplicationAdapter(getContext(), mViewModel, false);
        steamApplicationAdapter.steamApplicationList = (ArrayList<SteamApplication>) mViewModel.getAllSteamApplications();
        steamGridView.setAdapter(steamApplicationAdapter);
    }
}
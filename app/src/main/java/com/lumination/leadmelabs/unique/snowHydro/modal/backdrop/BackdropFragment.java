package com.lumination.leadmelabs.unique.snowHydro.modal.backdrop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentModalBackdropsBinding;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.unique.snowHydro.StationSingleNestedFragment;
import com.lumination.leadmelabs.unique.snowHydro.modal.ModalDialogFragment;
import com.lumination.leadmelabs.utilities.Constants;

import java.util.ArrayList;

public class BackdropFragment extends Fragment {
    public static BackdropAdapter localBackdropAdapter;
    private FragmentModalBackdropsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modal_backdrops, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        Station newlySelectedStation = ModalDialogFragment.mViewModel.getSelectedStation().getValue();
        if (newlySelectedStation == null) return;

        //Specifically set the selected nested station - used to populate the backdrop parts
        Station nestedStation = newlySelectedStation.getFirstNestedStationOrNull();
        if (nestedStation == null) return;

        //Set the adapter for backdrops
        binding.setSelectedNestedStation(nestedStation);
        GridView backdropGridView = view.findViewById(R.id.backdrop_grid);
        localBackdropAdapter = new BackdropAdapter(getContext(), false);
        localBackdropAdapter.backdropList = (ArrayList<Video>) nestedStation.videoController.getVideosOfType(Constants.VideoTypeBackdrop);
        backdropGridView.setAdapter(localBackdropAdapter);


        /*
         * Observes changes in the list of stations and updates the UI accordingly.
         * For each station in the list, if it the selected nested station,
         * it updates the UI and notifies the backdrop adapter.
         */
        StationSingleNestedFragment.mViewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
            if (stations == null) return;

            stations.forEach(station -> {
                //Only update the nested station
                if (nestedStation.getId() == station.getId()) {
                    binding.setSelectedNestedStation(nestedStation);
                    if (localBackdropAdapter != null) {
                        localBackdropAdapter.backdropList = (ArrayList<Video>) nestedStation.videoController.getVideosOfType(Constants.VideoTypeBackdrop);
                        localBackdropAdapter.notifyDataSetChanged();
                    }
                }
            });
        });
    }
}

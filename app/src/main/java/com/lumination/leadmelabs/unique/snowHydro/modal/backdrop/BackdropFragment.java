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

        //Specifically set the selected station - used to populate the layouts parts
        Station newlySelectedStation = ModalDialogFragment.mViewModel.getSelectedStation().getValue();
        binding.setSelectedStation(newlySelectedStation);

        //Set the adapter for backdrops
        if (newlySelectedStation != null) {
            GridView backdropGridView = view.findViewById(R.id.backdrop_section);
            localBackdropAdapter = new BackdropAdapter(getContext(), false);
            localBackdropAdapter.backdropList = (ArrayList<Video>) newlySelectedStation.videoController.getVideosOfType(Constants.VideoTypeBackdrop);
            backdropGridView.setAdapter(localBackdropAdapter);
        }
    }
}

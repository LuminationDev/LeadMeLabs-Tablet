package com.lumination.leadmelabs.ui.library.video;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentLibraryVideoBinding;
import com.lumination.leadmelabs.interfaces.ILibraryInterface;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import java.util.ArrayList;

public class VideoLibraryFragment extends Fragment implements ILibraryInterface {
    public static StationsViewModel mViewModel;
    public VideoAdapter localVideoAdapter;
    public static ArrayList<Video> localVideoList;
    private FragmentLibraryVideoBinding binding;
    public static FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library_video, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GridView videoGridView = view.findViewById(R.id.video_grid);
        localVideoAdapter = new VideoAdapter(getContext(), requireActivity().getSupportFragmentManager(), (SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
        updateVideoList(LibrarySelectionFragment.getStationId(), videoGridView, true);
        mViewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
            if (LibrarySelectionFragment.getStationId() > 0) {
                if (localVideoAdapter.videoList.size() != mViewModel.getStationVideos(LibrarySelectionFragment.getStationId()).size()) {
                    updateVideoList(LibrarySelectionFragment.getStationId(), videoGridView, false);
                }
            } else {
                if (localVideoAdapter.videoList.size() != mViewModel.getAllVideos().size()) {
                    updateVideoList(LibrarySelectionFragment.getStationId(), videoGridView, false);
                }
            }
        });
    }

    /**
     * Updates the video list displayed in the provided GridView based on the given station ID.
     * If the new video list is the same as the currently local video list, no update is performed.
     * If a station ID is provided, updates the local video list with videos specific to that station;
     * otherwise, updates it with all videos. Then, sets the adapter's video list,
     * updates the UI, performs a search (if applicable), and sets the adapter to the provided GridView.
     *
     * @param stationId The ID of the station to retrieve applications for, Id = 0 retrieves all videos.
     * @param view The GridView to update with the video list.
     * @param onCreate A boolean representing if the fragment has just been created.
     */
    private void updateVideoList(int stationId, GridView view, boolean onCreate) {
        ArrayList<Video> newVideoList = (ArrayList<Video>) mViewModel.getAllVideos();
        if (!onCreate && newVideoList.equals(localVideoList)) {
            return;
        }

        if (stationId > 0) {
            localVideoList = (ArrayList<Video>) mViewModel.getStationVideos(stationId);
        } else {
            localVideoList = (ArrayList<Video>) mViewModel.getAllVideos();
        }

        localVideoAdapter.videoList = new ArrayList<>(localVideoList);
        binding.setVideoList(localVideoAdapter.videoList);
        binding.setVideosLoaded(mViewModel.getAllApplications().size() > 0);

        //The list has been updated, perform the search again
        performSearch(LibrarySelectionFragment.mViewModel.getCurrentSearch().getValue());
        view.setAdapter(localVideoAdapter);
    }

    public void performSearch(String searchTerm) {
        localVideoAdapter.getFilter().filter(searchTerm);
    }

    public void refreshList() {
        Log.e("VIDEO", "REFRESH THE LIST!");
    }
}

package com.lumination.leadmelabs.ui.library.video;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardVideoBinding;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionPageFragment;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.unique.snowHydro.StationSingleNestedFragment;
import com.lumination.leadmelabs.utilities.Helpers;
import com.segment.analytics.Properties;

import java.util.ArrayList;

public class VideoAdapter extends BaseAdapter {
    public ArrayList<Video> videoList = new ArrayList<>();
    private final LayoutInflater mInflater;
    public static StationsViewModel mViewModel;
    private final FragmentManager fragmentManager;
    private final SideMenuFragment sideMenuFragment;

    VideoAdapter(Context context, FragmentManager fragmentManager, SideMenuFragment fragment) {
        this.mInflater = LayoutInflater.from(context);
        this.fragmentManager = fragmentManager;
        this.sideMenuFragment = fragment;
        mViewModel = ViewModelProviders.of((FragmentActivity) context).get(StationsViewModel.class);
    }

    @Override
    public int getCount() {
        return videoList != null ? videoList.size() : 0;
    }

    @Override
    public Video getItem(int position) {
        return videoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        CardVideoBinding binding;
        if (view == null) {
            mInflater.inflate(R.layout.card_video, null);
            binding = CardVideoBinding.inflate(mInflater, parent, false);
            view = binding.getRoot();
            view.setTag(binding);
        } else {
            binding = (CardVideoBinding) view.getTag();
        }

        Video currentVideo = getItem(position);
        Helpers.setVideoImage(currentVideo.getId(), view);
        binding.setVideo(currentVideo);

        View finalView = view;
        View.OnClickListener selectVideo = view1 -> {
            InputMethodManager inputManager = (InputMethodManager) finalView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(finalView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            completeSelectVideoAction(currentVideo);
        };

        view.setOnClickListener(selectVideo);
        Button playButton = view.findViewById(R.id.video_watch_button);
        playButton.setOnClickListener(selectVideo);

        return view;
    }

    private void completeSelectVideoAction(Video currentVideo) {
        if (LibrarySelectionFragment.getStationId() > 0) {
            Station station = mViewModel.getStationById(LibrarySelectionFragment.getStationId());
            if (station == null) {
                return;
            }
            station.checkForVideoPlayer(currentVideo);

            Properties segmentProperties = new Properties();
            segmentProperties.put("classification", LibrarySelectionFragment.segmentClassification);
            segmentProperties.put("name", currentVideo.getName());
            segmentProperties.put("id", station.getId());
            segmentProperties.put("type", currentVideo.getVideoType());
            segmentProperties.put("length", currentVideo.getLength());
            Segment.trackEvent(SegmentConstants.Launch_Video, segmentProperties);

            //Return to the regular station view or the nested station view
            if (station.nestedStations == null || station.nestedStations.isEmpty()) {
                sideMenuFragment.loadFragment(StationSingleFragment.class, "dashboard", null);
            }
            else {
                sideMenuFragment.loadFragment(StationSingleNestedFragment.class, "dashboard", null);
            }
        } else {
            mViewModel.setSelectedVideo(currentVideo);

            Properties segmentProperties = new Properties();
            segmentProperties.put("classification", LibrarySelectionFragment.segmentClassification);
            segmentProperties.put("name", currentVideo.getName());
            segmentProperties.put("type", currentVideo.getVideoType());
            segmentProperties.put("length", currentVideo.getLength());
            Segment.trackEvent(SegmentConstants.Select_Video, segmentProperties);

            Bundle args = new Bundle();
            args.putString("selection", "video");
            sideMenuFragment.loadFragment(StationSelectionPageFragment.class, "notMenu", args);
            fragmentManager.beginTransaction()
                    .replace(R.id.rooms, RoomFragment.class, null)
                    .commitNow();
            Segment.trackScreen("stationSelection");
        }
    }
}

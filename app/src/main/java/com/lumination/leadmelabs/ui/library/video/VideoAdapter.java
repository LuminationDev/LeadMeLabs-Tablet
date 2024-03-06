package com.lumination.leadmelabs.ui.library.video;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardVideoBinding;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.EmbeddedApplication;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionPageFragment;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Constants;

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
        //Helpers.SetExperienceImage(currentApplication.type, currentApplication.name, currentApplication.id, view);
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
        Log.e("VIDEO", currentVideo.getId());
        Log.e("ID", "ID: " + MainActivity.getStationId());

        if (MainActivity.getStationId() > 0) {
            Station station = mViewModel.getStationById(MainActivity.getStationId());
            if (station == null) {
                return;
            }

            Log.e("VIDEO", currentVideo.getId());

            //TODO
            // Work out how to send the source to a Station
            // Open the Video Play / VR Video Player (users choice) if not already open

            // Add an on click listener to the image if the video player is active
            Application current = station.findCurrentApplication();
            if (!(current instanceof EmbeddedApplication)) {
                // Video player is not open

                //TODO Open Video player and then send the source...
                // or handle that on the station side??
                // depends if we want control over what Video Player to open...
                return;
            }

            String subtype = current.subtype.optString("category", "");
            if (subtype.equals(Constants.VideoPlayer)) {
                station.videoController.loadTrigger(currentVideo.getSource());
            }

            sideMenuFragment.loadFragment(StationSingleFragment.class, "dashboard", null);
        } else {
            // Go to the station selection screen
            sideMenuFragment.loadFragment(StationSelectionPageFragment.class, "notMenu", null);
            fragmentManager.beginTransaction()
                    .replace(R.id.rooms, RoomFragment.class, null)
                    .commitNow();
        }
    }
}

package com.lumination.leadmelabs.ui.library.video;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardVideoBinding;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionPageFragment;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.unique.snowHydro.StationSingleNestedFragment;
import com.lumination.leadmelabs.utilities.Helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class VideoAdapter extends BaseAdapter implements Filterable {
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
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String searchTerm = constraint.toString().toLowerCase(Locale.ROOT);

                List<Video> filteredList = VideoLibraryFragment.localVideoList.stream()
                        .filter(video ->
                                video.getName().toLowerCase(Locale.ROOT).contains(searchTerm))
                        .collect(Collectors.toList());

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                filterResults.count = filteredList.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.values instanceof ArrayList<?>) {
                    videoList = ((ArrayList<?>) results.values)
                            .stream()
                            .filter(obj -> obj instanceof Video)
                            .map(obj -> (Video) obj)
                            .collect(Collectors.toCollection(ArrayList::new));
                    notifyDataSetChanged(); // Notify adapter about the data change
                }
            }
        };
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

            //Return to the regular station view or the nested station view
            if (station.nestedStations == null || station.nestedStations.isEmpty()) {
                sideMenuFragment.loadFragment(StationSingleFragment.class, "dashboard", null);
            }
            else {
                sideMenuFragment.loadFragment(StationSingleNestedFragment.class, "dashboard", null);
            }
        } else {
            mViewModel.setSelectedVideo(currentVideo);

            Bundle args = new Bundle();
            args.putString("selection", "video");
            sideMenuFragment.loadFragment(StationSelectionPageFragment.class, "notMenu", args);
            fragmentManager.beginTransaction()
                    .replace(R.id.rooms, RoomFragment.class, null)
                    .commitNow();
        }
    }
}

package com.lumination.leadmelabs.unique.snowHydro.modal.backdrop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.BR;
import com.lumination.leadmelabs.databinding.CardBackdropBinding;
import com.lumination.leadmelabs.databinding.CardBackdropSmallBinding;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;

import java.util.ArrayList;

public class BackdropAdapter extends BaseAdapter {
    public ArrayList<Video> backdropList = new ArrayList<>();
    private final LayoutInflater mInflater;

    public static StationsViewModel mViewModel;

    /**
     * A boolean representing if the backdrop is just the small preview. If the card_backdrop_small
     * should be used (true) or if the regular card_backdrop should be used (false).
     */
    private final boolean isPreview;

    public BackdropAdapter(Context context, boolean isPreview) {
        this.mInflater = LayoutInflater.from(context);
        this.isPreview = isPreview;
        mViewModel = ViewModelProviders.of((FragmentActivity) context).get(StationsViewModel.class);
    }
    @Override
    public int getCount()  {
        return backdropList != null ? backdropList.size() : 0;
    }

    @Override
    public Video getItem(int position)  {
        return backdropList.get(position);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            ViewDataBinding binding;
            if (isPreview) {
                binding = CardBackdropSmallBinding.inflate(mInflater, parent, false);
            } else {
                binding = CardBackdropBinding.inflate(mInflater, parent, false);
            }
            view = binding.getRoot();
            view.setTag(binding);
        }

        ViewDataBinding binding = (ViewDataBinding) view.getTag();
        Video currentVideo = getItem(position);
        Helpers.setVideoImage(currentVideo.getId(), view);
        binding.setVariable(BR.video, currentVideo);

        Station station = mViewModel.getSelectedStation().getValue();
        if (station != null) {
            Station nestedStation = station.getFirstNestedStationOrNull();
            binding.setVariable(BR.selectedNestedStation, nestedStation);
            view.setOnClickListener(v -> {
                if (nestedStation != null) {
                    nestedStation.checkForVideoPlayer(currentVideo, true, Constants.VIDEO_PLAYER_NAME);
                }
            });
        }

        return view;
    }
}

package com.lumination.leadmelabs.unique.snowHydro;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardBackdropBinding;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Helpers;

import java.util.ArrayList;

public class BackdropAdapter extends BaseAdapter {
    public ArrayList<Video> backdropList = new ArrayList<>();
    private final LayoutInflater mInflater;

    public static StationsViewModel mViewModel;

    BackdropAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
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
        CardBackdropBinding binding;
        if (view == null) {
            mInflater.inflate(R.layout.card_backdrop, null);
            binding = CardBackdropBinding.inflate(mInflater, parent, false);
            view = binding.getRoot();
            view.setTag(binding);
        } else {
            binding = (CardBackdropBinding) view.getTag();
        }

        Video currentVideo = getItem(position);
        Helpers.SetVideoImage(currentVideo.getId(), view);
        binding.setVideo(currentVideo);

        view.setOnClickListener(v -> {
            Log.e("BACKDROP", "SELECTED BACKDROP: " + currentVideo.getName());
            //Send the video launch command to all bound stations
        });

        return view;
    }
}

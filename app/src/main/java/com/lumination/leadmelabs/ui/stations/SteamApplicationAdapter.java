package com.lumination.leadmelabs.ui.stations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.databinding.SteamTileBinding;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.SteamApplication;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.services.NetworkService;

import java.util.ArrayList;

public class SteamApplicationAdapter extends BaseAdapter {

    private final String TAG = "SteamApplicationAdapter";

    public ArrayList<SteamApplication> steamApplicationList = new ArrayList<>();
    public int stationId = 0;
    private LayoutInflater mInflater;
    private Context context;
    private StationsViewModel viewModel;

    SteamApplicationAdapter(Context context, StationsViewModel viewModel) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.viewModel = viewModel;
    }

    @Override
    public int getCount() {
        return steamApplicationList != null ? steamApplicationList.size() : 0;
    }

    @Override
    public SteamApplication getItem(int position) {
        return steamApplicationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return steamApplicationList.get(position).id;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        SteamTileBinding binding;
        if (view == null) {
            view = mInflater.inflate(R.layout.steam_tile, null);
            binding = SteamTileBinding.inflate(mInflater, parent, false);
            view = binding.getRoot();
            view.setTag(binding);
        } else {
            binding = (SteamTileBinding) view.getTag();
        }

        SteamApplication steamApplication = getItem(position);
        Glide.with(view).load(SteamApplication.getImageUrl(steamApplication.id)).into((ImageView) view.findViewById(R.id.steam_image));

        binding.setSteamApplication(steamApplication);

        Button playButton = view.findViewById(R.id.steam_play_button);

        playButton.setOnClickListener(v -> {
            viewModel.selectSelectedSteamApplication(steamApplication.id);
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, StationSelectionFragment.class, null)
                    .commitNow();
            StationSelectionFragment fragment = (StationSelectionFragment) MainActivity.fragmentManager.findFragmentById(R.id.main);
            View newView = fragment.getView();
            TextView textView = newView.findViewById(R.id.station_selection_game_name);
            textView.setText(steamApplication.name);
        });

        return view;
    }
}
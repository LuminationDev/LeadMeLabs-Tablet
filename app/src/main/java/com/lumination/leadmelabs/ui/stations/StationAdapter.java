package com.lumination.leadmelabs.ui.stations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.lumination.leadmelabs.databinding.CardStationBinding;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;

import java.util.ArrayList;

public class StationAdapter extends BaseAdapter {

    private final String TAG = "StationAdapter";

    public ArrayList<Station> stationList = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context context;
    private StationsViewModel viewModel;

    StationAdapter(Context context, StationsViewModel viewModel) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.viewModel = viewModel;
    }

    @Override
    public int getCount() {
        return stationList != null ? stationList.size() : 0;
    }

    @Override
    public Station getItem(int position) {
        return stationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return stationList.get(position).id;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        View result = view;
        CardStationBinding binding;
        Station station = getItem(position);
        if (result == null) {
            if (mInflater == null) {
                mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            binding = CardStationBinding.inflate(mInflater, parent, false);
            result = binding.getRoot();
            result.setTag(binding);
        } else {
            binding = (CardStationBinding) result.getTag();
        }
        binding.setStation(station);
        result.setOnClickListener(v -> {
            viewModel.selectStation(position);
            NetworkService.sendMessage("Station," + station.id, "CommandLine", "StartVR");
        });

        return result;
    }
}
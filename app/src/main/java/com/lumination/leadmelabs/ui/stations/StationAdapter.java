package com.lumination.leadmelabs.ui.stations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.databinding.StationCardBinding;
import com.lumination.leadmelabs.models.Station;

import java.util.ArrayList;

public class StationAdapter extends BaseAdapter {

    private final String TAG = "CuratedContentAdapter";

    public ArrayList<Station> stationList = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context context;
    private View list_view;

    StationAdapter(Context context, View list_view) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.list_view = list_view;
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
        return stationList.get(position).number;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        View result = view;
        StationCardBinding binding;
        if (result == null) {
            if (mInflater == null) {
                mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            binding = StationCardBinding.inflate(mInflater, parent, false);
            result = binding.getRoot();
            result.setTag(binding);
        } else {
            binding = (StationCardBinding) result.getTag();
        }
        binding.setStation(getItem(position));

        return result;
    }
}
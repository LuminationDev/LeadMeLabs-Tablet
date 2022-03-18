package com.lumination.leadmelab.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.lumination.leadmelab.MainActivity;
import com.lumination.leadmelab.R;
import com.lumination.leadmelab.network.Station;
import com.lumination.leadmelab.managers.StationManager;

import java.util.ArrayList;
import java.util.List;

public class StationAdapter extends BaseAdapter {
    private final String TAG = "Station Adapter";

    public ArrayList<Station> mData = new ArrayList<>();
    private final LayoutInflater mInflater;
    private final MainActivity main;
    private final StationManager manager;

    private final GridView stationGrid;
    private final TextView waitingForStations;


    public StationAdapter(MainActivity main, StationManager manager, List<Station> data) {
        this.main = main;
        this.manager = manager;
        this.mInflater = LayoutInflater.from(main);
        this.stationGrid = main.stationManagerScreen.findViewById(R.id.stationListView);
        this.waitingForStations = main.stationManagerScreen.findViewById(R.id.no_stations_connected);

        mData.addAll(data);
    }
    
    public void refresh() {
        main.runOnUiThread(() -> {
            notifyDataSetChanged();
            super.notifyDataSetChanged();
        });
    }

    public void addStation(Station station) {
        //does someone with this name already exist?
        Station found = null; //fix this later

        if (found != null) {
            Log.w(TAG, "Already have a record for this person! Replacing it with a new one.");
            //remove old one so we keep the newest version
            mData.remove(found);
        }

        //add the newest version
        mData.add(station);

        //Turn to binding later
        main.runOnUiThread(() -> {
            waitingForStations.setVisibility(mData.size() > 0 ? View.GONE : View.VISIBLE);
            stationGrid.setVisibility(mData.size() > 0 ? View.VISIBLE : View.GONE);
            manager.showLoading(false);
        });

        refresh();

        Log.d(TAG, "Adding " + station.getName() + " to station list, IP: " + station.getIP() + ". Now: " + mData.size());
    }

    public boolean hasConnectedStations() {
        return mData.size() > 0;
    }

    @Override
    public int getCount() {
        if (mData != null) {
            return mData.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = mInflater.inflate(R.layout.row_station, parent, false);

        TextView stationName = convertView.findViewById(R.id.station_name);
        ImageView selectedIndicator = convertView.findViewById(R.id.selected_indicator);

        final Station mStation = mData.get(position);
        if (mStation != null) {
            stationName.setText(mStation.getName());

//            if (mStation.isSelected()) {
//                selectedIndicator.setVisibility(View.VISIBLE);
//            } else {
//                selectedIndicator.setVisibility(View.INVISIBLE);
//            }

            convertView.setLongClickable(true);

            convertView.setOnClickListener(v -> {
                Log.d(TAG, "Clicked on " + mStation.getName() + ", " + mStation.getIP());
                StationManager.selected = mStation;
                manager.mApplicationManager.showLoading(true);
                main.setStation(mStation);
                StationManager.executeCommand(StationManager.APPS);
                main.changeScreen(MainActivity.ANIM_STATION_INDEX);
            });
        }
        return convertView;
    }
}


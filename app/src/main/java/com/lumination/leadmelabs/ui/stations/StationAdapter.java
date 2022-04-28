package com.lumination.leadmelabs.ui.stations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
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
    private boolean launchSingleOnTouch = false;

    StationAdapter(Context context, StationsViewModel viewModel, boolean launchSingleOnTouch) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.viewModel = viewModel;
        this.launchSingleOnTouch = launchSingleOnTouch;
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

        ImageView imageView = result.findViewById(R.id.station_vr_icon);
        if (station.status.equals("Off")) {
            imageView.setImageResource(R.drawable.icon_vr_gray);
        } else {
            imageView.setImageResource(R.drawable.icon_vr);
        }

        // todo - I don't really like having the two separate flows here - can probably split out into other methods
        if (launchSingleOnTouch == true) {
            result.setOnClickListener(v -> {
                viewModel.selectStation(position);
                MainActivity.fragmentManager.beginTransaction()
                        .replace(R.id.main, StationSingleFragment.class, null)
                        .commitNow();
            });
        } else {
            View finalResult = result;
            if (station.hasSteamApplicationInstalled(viewModel.getSelectedSteamApplicationId())) {
                result.setOnClickListener(v -> {
                    station.selected = !station.selected;
                    viewModel.updateStationById(station.id, station);
                });
            } else {
                finalResult.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_disabled));
                if (!station.status.equals("Off")) {
                    TextView infoText = finalResult.findViewById(R.id.card_info_text);
                    infoText.setText("Game not installed");
                    infoText.setVisibility(View.VISIBLE);
                }
            }
        }

        return result;
    }
}
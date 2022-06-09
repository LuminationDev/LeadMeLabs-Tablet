package com.lumination.leadmelabs.ui.stations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.databinding.SteamTileBinding;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.SteamApplication;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;

import java.util.ArrayList;

public class SteamApplicationAdapter extends BaseAdapter {

    private final String TAG = "SteamApplicationAdapter";

    public ArrayList<SteamApplication> steamApplicationList = new ArrayList<>();
    public static int stationId = 0;
    private LayoutInflater mInflater;
    private Context context;
    private StationsViewModel viewModel;
    private StationsViewModel stationsViewModel;

    SteamApplicationAdapter(Context context, StationsViewModel viewModel) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.viewModel = viewModel;
        stationsViewModel = ViewModelProviders.of((FragmentActivity) context).get(StationsViewModel.class);
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

        View finalView = view;
        view.setOnClickListener(v -> {
            InputMethodManager inputManager = (InputMethodManager) finalView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(finalView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        });

        if (stationId > 0) {
            playButton.setOnClickListener(v -> {
                Station station = stationsViewModel.getStationById(stationId);
                if (station != null && station.theatreText != null) {
                    View confirmDialogView = View.inflate(context, R.layout.dialog_confirm, null);
                    Button confirmButton = confirmDialogView.findViewById(R.id.confirm_button);
                    Button cancelButton = confirmDialogView.findViewById(R.id.cancel_button);
                    TextView headingText = confirmDialogView.findViewById(R.id.heading_text);
                    TextView contentText = confirmDialogView.findViewById(R.id.content_text);
                    headingText.setText("Exit theatre mode?");
                    contentText.setText(station.name + " is currently in theatre mode. Are you sure you want to exit theatre mode?");
                    AlertDialog confirmDialog = new AlertDialog.Builder(context).setView(confirmDialogView).create();
                    confirmButton.setOnClickListener(w -> {
                        NetworkService.sendMessage("Station," + stationId, "Steam", "Launch:" + steamApplication.id);
                        SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
                        confirmDialog.dismiss();
                        MainActivity.awaitStationGameLaunch(new int[] { station.id }, steamApplication.name);
                    });
                    cancelButton.setOnClickListener(x -> confirmDialog.dismiss());
                    confirmDialog.show();
                } else {
                    NetworkService.sendMessage("Station," + stationId, "Steam", "Launch:" + steamApplication.id);
                    SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
                    MainActivity.awaitStationGameLaunch(new int[] { station.id }, steamApplication.name);
                }
            });
        } else {
            playButton.setOnClickListener(v -> {
                viewModel.selectSelectedSteamApplication(steamApplication.id);
                SideMenuFragment.loadFragment(StationSelectionFragment.class, "notMenu");
                MainActivity.fragmentManager.beginTransaction()
                        .replace(R.id.rooms, RoomFragment.class, null)
                        .commitNow();

                StationSelectionFragment fragment = (StationSelectionFragment) MainActivity.fragmentManager.findFragmentById(R.id.main);
                View newView = fragment.getView();
                TextView textView = newView.findViewById(R.id.station_selection_game_name);
                textView.setText(steamApplication.name);
            });
        }

        return view;
    }
}
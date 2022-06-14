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

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.databinding.SteamTileBinding;
import com.lumination.leadmelabs.managers.DialogManager;
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
    private final LayoutInflater mInflater;
    private final Context context;
    public static StationsViewModel mViewModel;

    SteamApplicationAdapter(Context context) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        mViewModel = ViewModelProviders.of((FragmentActivity) context).get(StationsViewModel.class);
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
                Station station = SteamApplicationAdapter.mViewModel.getStationById(SteamApplicationAdapter.stationId);
                if (station != null && station.theatreText != null) {
                    DialogManager.buildLaunchExperienceDialog(context, steamApplication, station);
                } else if(station != null) {
                    NetworkService.sendMessage("Station," + SteamApplicationAdapter.stationId, "Steam", "Launch:" + steamApplication.id);
                    SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
                    DialogManager.awaitStationGameLaunch(new int[] { station.id }, steamApplication.name);
                }
            });
        } else {
            playButton.setOnClickListener(v -> {
                mViewModel.selectSelectedSteamApplication(steamApplication.id);
                SideMenuFragment.loadFragment(StationSelectionFragment.class, "notMenu");
                MainActivity.fragmentManager.beginTransaction()
                        .replace(R.id.rooms, RoomFragment.class, null)
                        .commitNow();

                StationSelectionFragment fragment = (StationSelectionFragment) MainActivity.fragmentManager.findFragmentById(R.id.main);
                View newView = fragment.getView();
                FlexboxLayout additionalStepsWarning = newView.findViewById(R.id.additional_steps_warning);
                additionalStepsWarning.setVisibility(View.GONE);
                TextView textView = newView.findViewById(R.id.station_selection_game_name);
                textView.setText(steamApplication.name);
                ArrayList<Integer> gamesWithAdditionalStepsRequired = new ArrayList<>();

                gamesWithAdditionalStepsRequired.add(513490); // 1943 Berlin Blitz
                gamesWithAdditionalStepsRequired.add(408340); // Gravity Lab

                if (gamesWithAdditionalStepsRequired.contains(steamApplication.id)) {
                    additionalStepsWarning.setVisibility(View.VISIBLE);
                }
            });
        }

        return view;
    }
}
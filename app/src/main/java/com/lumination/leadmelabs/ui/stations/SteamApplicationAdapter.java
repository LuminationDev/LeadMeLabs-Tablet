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
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
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
    public static StationsViewModel mViewModel;

    SteamApplicationAdapter(Context context) {
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

        View finalView = view;
        View.OnClickListener selectGame = view1 -> {
            InputMethodManager inputManager = (InputMethodManager) finalView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(finalView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
//                confirm if it is one of the dodgy apps
            ArrayList<Integer> gamesWithAdditionalStepsRequired = new ArrayList<>();

            gamesWithAdditionalStepsRequired.add(513490); // 1943 Berlin Blitz
            gamesWithAdditionalStepsRequired.add(408340); // Gravity Lab
            gamesWithAdditionalStepsRequired.add(1053760); // Arkio

            if (gamesWithAdditionalStepsRequired.contains(steamApplication.id)) {
                BooleanCallbackInterface booleanCallbackInterface = new BooleanCallbackInterface() {
                    @Override
                    public void callback(boolean result) {
                        if (result) {
                            completeSelectApplicationAction(steamApplication);
                        }
                    }
                };
                DialogManager.createConfirmationDialog(
                        "Attention",
                        "This game may require additional steps to launch and may not be able to launch automatically.",
                        booleanCallbackInterface,
                        "Go Back",
                        "Continue");
            } else {
                completeSelectApplicationAction(steamApplication);
            }
        };
        view.setOnClickListener(selectGame);
        Button playButton = view.findViewById(R.id.steam_play_button);
        playButton.setOnClickListener(selectGame);

//        Button infoButton = view.findViewById(R.id.steam_info_button);
//        infoButton.setOnClickListener(v -> {
//            DialogManager.buildWebViewDialog(MainActivity.getInstance(), "https://store.steampowered.com/app/" + binding.getSteamApplication().id);
//        });

        return view;
    }

    private void completeSelectApplicationAction(SteamApplication steamApplication) {
        if (stationId > 0) {
            Station station = SteamApplicationAdapter.mViewModel.getStationById(SteamApplicationAdapter.stationId);
            if (station != null) {
                NetworkService.sendMessage("Station," + SteamApplicationAdapter.stationId, "Steam", "Launch:" + steamApplication.id);
                SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
                DialogManager.awaitStationGameLaunch(new int[] { station.id }, steamApplication.name, false);
            }
        } else {
            mViewModel.selectSelectedSteamApplication(steamApplication.id);
            SideMenuFragment.loadFragment(StationSelectionPageFragment.class, "notMenu");
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.rooms, RoomFragment.class, null)
                    .commitNow();

            StationSelectionPageFragment fragment = (StationSelectionPageFragment) MainActivity.fragmentManager.findFragmentById(R.id.main);
            View newView = fragment.getView();
            TextView textView = newView.findViewById(R.id.station_selection_game_name);
            textView.setText(steamApplication.name);
        }
    }
}
package com.lumination.leadmelabs.ui.application;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.ApplicationTileBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.CustomApplication;
import com.lumination.leadmelabs.models.applications.SteamApplication;
import com.lumination.leadmelabs.models.applications.ViveApplication;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionPageFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import java.util.ArrayList;
import java.util.Objects;

public class ApplicationAdapter extends BaseAdapter {
    private final String TAG = "ApplicationAdapter";

    public ArrayList<Application> applicationList = new ArrayList<>();
    public static int stationId = 0;
    private final LayoutInflater mInflater;
    public static StationsViewModel mViewModel;

    ApplicationAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
        mViewModel = ViewModelProviders.of((FragmentActivity) context).get(StationsViewModel.class);
    }

    @Override
    public int getCount() {
        return applicationList != null ? applicationList.size() : 0;
    }

    @Override
    public Application getItem(int position) {
        return applicationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ApplicationTileBinding binding;
        if (view == null) {
            mInflater.inflate(R.layout.application_tile, null);
            binding = ApplicationTileBinding.inflate(mInflater, parent, false);
            view = binding.getRoot();
            view.setTag(binding);
        } else {
            binding = (ApplicationTileBinding) view.getTag();
        }

        Application currentApplication = getItem(position);

        String filePath;
        switch(currentApplication.type) {
            case "Custom":
                filePath = CustomApplication.getImageUrl(currentApplication.name);
                break;
            case "Steam":
                filePath = SteamApplication.getImageUrl(currentApplication.name, currentApplication.id);
                break;
            case "Vive":
                filePath = ViveApplication.getImageUrl(currentApplication.id);
                break;
            default:
                filePath = "";
        }

        //Attempt to load the image url or a default image if nothing is available
        if(Objects.equals(filePath, "")) {
            Glide.with(view).load(R.drawable.default_header).into((ImageView) view.findViewById(R.id.experience_image));
        } else {
            View finalView1 = view;
            Glide.with(view).load(filePath)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        // Error occurred while loading the image, change the imageUrl to the fallback image
                        MainActivity.runOnUI(() -> {
                            Glide.with(finalView1)
                                    .load(R.drawable.default_header)
                                    .into((ImageView) finalView1.findViewById(R.id.experience_image));
                        });
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        // Image loaded successfully
                        return false;
                    }
                })
                .into((ImageView) view.findViewById(R.id.experience_image));
        }

        binding.setApplication(currentApplication);

        View finalView = view;
        View.OnClickListener selectGame = view1 -> {
            InputMethodManager inputManager = (InputMethodManager) finalView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(finalView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
//                confirm if it is one of the dodgy apps
            ArrayList<String> gamesWithAdditionalStepsRequired = new ArrayList<>();

            gamesWithAdditionalStepsRequired.add("513490"); // 1943 Berlin Blitz
            gamesWithAdditionalStepsRequired.add("408340"); // Gravity Lab
            gamesWithAdditionalStepsRequired.add("1053760"); // Arkio

            if (gamesWithAdditionalStepsRequired.contains(currentApplication.id)) {
                BooleanCallbackInterface booleanCallbackInterface = result -> {
                    if (result) {
                        completeSelectApplicationAction(currentApplication);
                    }
                };
                DialogManager.createConfirmationDialog(
                        "Attention",
                        "This game may require additional steps to launch and may not be able to launch automatically.",
                        booleanCallbackInterface,
                        "Go Back",
                        "Continue");
            } else {
                completeSelectApplicationAction(currentApplication);
            }
        };

        view.setOnClickListener(selectGame);
        Button playButton = view.findViewById(R.id.experience_play_button);
        playButton.setOnClickListener(selectGame);

        return view;
    }

    private void completeSelectApplicationAction(Application currentApplication) {
        if (stationId > 0) {
            Station station = ApplicationAdapter.mViewModel.getStationById(ApplicationAdapter.stationId);
            if (station != null) {
                NetworkService.sendMessage("Station," + ApplicationAdapter.stationId, "Experience", "Launch:" + currentApplication.id);
                SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
                DialogManager.awaitStationGameLaunch(new int[] { station.id }, currentApplication.name, false);
            }
        } else {
            mViewModel.selectSelectedApplication(currentApplication.id);
            SideMenuFragment.loadFragment(StationSelectionPageFragment.class, "notMenu");
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.rooms, RoomFragment.class, null)
                    .commitNow();

            StationSelectionPageFragment fragment = (StationSelectionPageFragment) MainActivity.fragmentManager.findFragmentById(R.id.main);
            View newView = fragment.getView();
            TextView textView = newView.findViewById(R.id.station_selection_game_name);
            textView.setText(currentApplication.name);
        }
    }
}
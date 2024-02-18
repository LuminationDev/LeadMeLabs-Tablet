package com.lumination.leadmelabs.ui.application;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardExperienceBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionPageFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ApplicationAdapter extends BaseAdapter {
    private final String TAG = "ApplicationAdapter";

    public ArrayList<Application> applicationList = new ArrayList<>();
    public static int stationId = 0;
    private final LayoutInflater mInflater;
    public static StationsViewModel mViewModel;
    private FragmentManager fragmentManager;
    private SideMenuFragment sideMenuFragment;

    ApplicationAdapter(Context context, FragmentManager fragmentManager, SideMenuFragment fragment) {
        this.mInflater = LayoutInflater.from(context);
        this.fragmentManager = fragmentManager;
        this.sideMenuFragment = fragment;
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
        CardExperienceBinding binding;
        if (view == null) {
            mInflater.inflate(R.layout.card_experience, null);
            binding = CardExperienceBinding.inflate(mInflater, parent, false);
            view = binding.getRoot();
            view.setTag(binding);
        } else {
            binding = (CardExperienceBinding) view.getTag();
        }

        Application currentApplication = getItem(position);
        Helpers.SetExperienceImage(currentApplication.type, currentApplication.name, currentApplication.id, view);
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
                        "Continue",
                        false);
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
            if (station == null) {
                return;
            }

            //Check if application has a shareCode subtype and load the next fragment instead
            if (currentApplication.HasCategory().equals(Constants.ShareCode)) {
                mViewModel.selectSelectedApplication(currentApplication.id);
                mViewModel.setSelectedApplication(currentApplication);
                loadSingleShareCodeFragment();
                return;
            }

            //BACKWARDS COMPATIBILITY - JSON Messaging system with fallback
            if (MainActivity.isNucJsonEnabled) {
                JSONObject message = new JSONObject();
                try {
                    message.put("Action", "Launch");
                    message.put("ExperienceId", currentApplication.id);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                NetworkService.sendMessage("Station," + ApplicationAdapter.stationId, "Experience", message.toString());
            }
            else {
                NetworkService.sendMessage("Station," + ApplicationAdapter.stationId, "Experience", "Launch:" + currentApplication.id);
            }

            sideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard", null);
            DialogManager.awaitStationGameLaunch(new int[] { station.id }, currentApplication.name, false);

        } else {
            mViewModel.selectSelectedApplication(currentApplication.id);
            mViewModel.setSelectedApplication(currentApplication);
            sideMenuFragment.loadFragment(StationSelectionPageFragment.class, "notMenu", null);
            fragmentManager.beginTransaction()
                    .replace(R.id.rooms, RoomFragment.class, null)
                    .commitNow();
        }
    }

    /**
     * The application has been detected as a Share code subtype. Load in the Share Code fragment
     * for users to input the unique code to be sent to the selected Station.
     */
    private void loadSingleShareCodeFragment() {
        fragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out)
                .replace(R.id.main, ApplicationShareCodeFragment.class, null)
                .addToBackStack("menu:dashboard:stationSingle:shareCode")
                .commit();
    }
}

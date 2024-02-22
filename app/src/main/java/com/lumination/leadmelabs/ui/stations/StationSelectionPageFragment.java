package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentPageStationSelectionBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.application.ApplicationSelectionFragment;
import com.lumination.leadmelabs.ui.application.ApplicationShareCodeFragment;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;
import com.lumination.leadmelabs.utilities.Identifier;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StationSelectionPageFragment extends Fragment {

    private FragmentPageStationSelectionBinding binding;

    public static FragmentManager childManager;
    public static StationsViewModel mViewModel;

    public static StationSelectionPageFragment instance;
    public static StationSelectionPageFragment getInstance() { return instance; }

    private EditText[] editTexts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page_station_selection, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);

        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);
        ArrayList<Station> stations = (ArrayList<Station>) mViewModel.getStations().getValue();
        stations = (ArrayList<Station>) stations.clone();
        for (Station station:stations) {
            station.selected = false;
            mViewModel.updateStationById(station.id, station);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setStations(mViewModel);

        //Specifically set the selected application
        Application selectedApplication = mViewModel.getSelectedApplication().getValue();
        binding.setSelectedApplication(selectedApplication);
        if (selectedApplication != null) {
            //TODO uncomment when application experiences have descriptions
            //Helpers.SetExperienceImage(selectedApplication.type, selectedApplication.name, selectedApplication.id, view);
        }
        //TODO uncomment when application experiences have descriptions
        //SetupEditText(view);

        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            ((SideMenuFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(HelpPageFragment.class, "help", null);
        });

        loadFragments();

        CheckBox selectCheckbox = view.findViewById(R.id.select_all_checkbox);
        selectCheckbox.setOnCheckedChangeListener((checkboxView, checked) -> {
            ArrayList<Station> stations = StationSelectionFragment.getInstance().getRoomStations();
            stations = (ArrayList<Station>) stations.clone();
            for (Station station:stations) {
                if (!station.status.equals("Off") && station.hasApplicationInstalled(mViewModel.getSelectedApplicationId())) {
                    station.selected = checked;
                    mViewModel.updateStationById(station.id, station);
                }
            }
        });

        Button backButton = view.findViewById(R.id.cancel_button);
        backButton.setOnClickListener(v -> {
            mViewModel.selectSelectedApplication("");
            ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(ApplicationSelectionFragment.class, "session", null);
        });

        Button playButton = view.findViewById(R.id.select_stations);
        playButton.setOnClickListener(v -> {
            int[] selectedIds = mViewModel.getSelectedStationIds();
            if (selectedIds.length > 0) {
                confirmLaunchGame(selectedIds, selectedApplication);
            }
        });

        View identifyStations = view.findViewById(R.id.identify_button);
        identifyStations.setOnClickListener(v -> {
            List<Station> stations = StationSelectionFragment.getInstance().getRoomStations();
            Identifier.identifyStations(stations);
        });

        instance = this;
    }

    public void confirmLaunchGame(int[] selectedIds, Application selectedApplication) {
        String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));
        String parameters = ApplicationShareCodeFragment.generateParameters(selectedApplication.name, editTexts);
        if (parameters.equals(Constants.Invalid)) {
            return;
        }

        //BACKWARDS COMPATIBILITY - JSON Messaging system with fallback
        if (MainActivity.isNucJsonEnabled) {
            JSONObject message = new JSONObject();
            try {
                message.put("Action", "Launch");
                message.put("ExperienceId", selectedApplication.id);
                if (!parameters.isEmpty()) {
                    message.put("Parameters", parameters);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            NetworkService.sendMessage("Station," + stationIds, "Experience", message.toString());
        }
        else {
            String additionalData = "Launch:" + selectedApplication.id;
            if (!parameters.isEmpty()) {
                additionalData += ":Parameters:" + parameters;
            }

            NetworkService.sendMessage("Station," + stationIds, "Experience", additionalData);
        }

        ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(DashboardPageFragment.class, "dashboard", null);
        DialogManager.awaitStationGameLaunch(selectedIds, ApplicationSelectionFragment.mViewModel.getSelectedApplicationName(selectedApplication.id), false);
    }

    /**
     * Sets up a pin-like input mechanism for a group of EditText fields.
     *
     * @param view The view containing the EditText fields.
     */
    private void SetupEditText(View view) {
        editTexts = new EditText[]{
                view.findViewById(R.id.et1),
                view.findViewById(R.id.et2),
                view.findViewById(R.id.et3),
                view.findViewById(R.id.et4),
                view.findViewById(R.id.et5),
                view.findViewById(R.id.et6)
        };

        for (int i = 0; i < editTexts.length - 1; i++) {
            int finalI = i;
            editTexts[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        editTexts[finalI + 1].requestFocus();
                    }
                }
            });
        }

        for (int i = 1; i < editTexts.length; i++) {
            int finalI = i;
            editTexts[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (editTexts[finalI].getText().toString().isEmpty()) {
                        editTexts[finalI - 1].requestFocus();
                        return true;
                    }
                }
                return false;
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);
        ArrayList<Station> stations = (ArrayList<Station>) mViewModel.getStations().getValue();
        stations = (ArrayList<Station>) stations.clone();
        for (Station station:stations) {
            station.selected = false;
            mViewModel.updateStationById(station.id, station);
        }
    }

    private void loadFragments() {
        childManager.beginTransaction()
                .replace(R.id.station_selection_list_container, StationSelectionFragment.class, null)
                .commitNow();
    }
}

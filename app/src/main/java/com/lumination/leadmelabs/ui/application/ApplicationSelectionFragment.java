package com.lumination.leadmelabs.ui.application;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentApplicationSelectionBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class ApplicationSelectionFragment extends Fragment {

    public static StationsViewModel mViewModel;
    public static ApplicationAdapter installedApplicationAdapter;
    private static ArrayList<Application> installedApplicationList;
    private static int stationId = 0;
    private static FragmentApplicationSelectionBinding binding;
    public static FragmentManager childManager;

    private static String currentSearch = "";

    public static void setStationId (int stationId) {
        ApplicationSelectionFragment.stationId = stationId;
        if (stationId > 0) {
            installedApplicationList = (ArrayList<Application>) mViewModel.getStationApplications(stationId);
        } else {
            installedApplicationList = (ArrayList<Application>) mViewModel.getAllApplications();
        }
        if (installedApplicationAdapter != null) {
            ApplicationAdapter.stationId = stationId;
            installedApplicationAdapter.applicationList = (ArrayList<Application>) installedApplicationList.clone();
            binding.setApplicationList(installedApplicationAdapter.applicationList);
            installedApplicationAdapter.notifyDataSetChanged();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_application_selection, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);
        return view;
    }

    /**
     * Updates the application list displayed in the provided GridView based on the given station ID.
     * If the new application list is the same as the currently installed application list, no update is performed.
     * If a station ID is provided, updates the installed application list with applications specific to that station;
     * otherwise, updates it with all applications. Then, sets the adapter's application list,
     * updates the UI, performs a search (if applicable), and sets the adapter to the provided GridView.
     *
     * @param stationId The ID of the station to retrieve applications for, Id = 0 retrieves all applications.
     * @param view The GridView to update with the application list.
     * @param onCreate A boolean representing if the fragment has just been created.
     */
    private void updateApplicationList(int stationId, GridView view, boolean onCreate) {
        ArrayList<Application> newApplicationList = (ArrayList<Application>) mViewModel.getAllApplications();
        if (!onCreate && newApplicationList.equals(installedApplicationList)) {
            return;
        }

        if (stationId > 0) {
            installedApplicationList = (ArrayList<Application>) mViewModel.getStationApplications(stationId);
        } else {
            installedApplicationList = (ArrayList<Application>) mViewModel.getAllApplications();
        }
        installedApplicationAdapter.applicationList = (ArrayList<Application>) installedApplicationList.clone();
        binding.setApplicationList(installedApplicationAdapter.applicationList);
        binding.setApplicationsLoaded(mViewModel.getAllApplications().size() > 0);

        //The list has been updated, perform the search again
        performSearch(currentSearch);
        view.setAdapter(installedApplicationAdapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            childManager.beginTransaction()
                    .replace(R.id.logo, LogoFragment.class, null)
                    .commitNow();
        }

        Bundle bundle = getArguments();
        String stationName = bundle != null ? bundle.getString("station") : null;

        TextView stationTitle = view.findViewById(R.id.selectedStation);
        stationTitle.setVisibility(stationName != null ? View.VISIBLE : View.GONE);
        stationTitle.setText(stationName != null ? MessageFormat.format(" - {0}", stationName) : "");

        GridView steamGridView = view.findViewById(R.id.experience_list);
        installedApplicationAdapter = new ApplicationAdapter(getContext(), getActivity().getSupportFragmentManager(), (SideMenuFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
        updateApplicationList(stationId, steamGridView, true);
        mViewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
            if (stationId > 0) {
                if (installedApplicationAdapter.applicationList.size() != mViewModel.getStationApplications(stationId).size()) {
                    updateApplicationList(stationId, steamGridView, false);
                }
            } else {
                if (installedApplicationAdapter.applicationList.size() != mViewModel.getAllApplications().size()) {
                    updateApplicationList(stationId, steamGridView, false);
                }
            }
        });

        EditText searchInput = view.findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String searchTerm = searchInput.getText().toString();
                currentSearch = searchTerm;
                performSearch(searchTerm);
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
        searchInput.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                String searchTerm = textView.getText().toString();
                performSearch(searchTerm);
                dismissKeyboard(searchInput);
            }
            return false;
        });
        FlexboxLayout steamListContainer = view.findViewById(R.id.experience_list_container);
        steamListContainer.setOnClickListener(v -> {
            dismissKeyboard(searchInput);
        });
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                dismissKeyboard(searchInput);
            }
        });
        steamGridView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dismissKeyboard(searchInput);
            }
            return false;
        });

        //Check if there are any experiences running or if the selected Station has an experience running
        Button refresh_btn = view.findViewById(R.id.refresh_experiences_btn);
        refresh_btn.setOnClickListener(v -> checkForRunningExperiences());

        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            ((SideMenuFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(HelpPageFragment.class, "help", null);
        });
    }

    private static void performSearch(String searchTerm) {
        ArrayList<Application> filteredSteamApplicationList = (ArrayList<Application>) installedApplicationList.clone();
        filteredSteamApplicationList.removeIf(currentApplication -> !currentApplication.name.toLowerCase(Locale.ROOT).contains(searchTerm.trim()));
        installedApplicationAdapter.applicationList = filteredSteamApplicationList;
        binding.setApplicationList(installedApplicationAdapter.applicationList);
        installedApplicationAdapter.notifyDataSetChanged();
    }

    private void dismissKeyboard(View searchInput) {
        InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(searchInput.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * Checks if there are any running experiences on the current station or other stations.
     * If running experiences are found, prompts the user with a confirmation dialog.
     * If no running experiences are found, proceeds to refresh the list of Steam games.
     */
    private void checkForRunningExperiences() {
        boolean showPrompt = false;
        String message = "";

        if (stationId > 0) {
            Station station = mViewModel.getStationById(stationId);
            if(!station.gameId.isEmpty()) {
                showPrompt = true;
                message = "Refreshing this experience list will stop the experience: " + station.gameName + ", running on Station " + stationId;
            }
        } else {
            ArrayList<Station> stations = (ArrayList<Station>) mViewModel.getStations().getValue();
            if (stations != null) {
                ArrayList<Integer> stationIds = stations.stream()
                        .filter(station -> !station.gameId.isEmpty())
                        .map(Station::getId)
                        .collect(Collectors.toCollection(ArrayList::new));

                if (!stationIds.isEmpty()) {
                    String joinedString = stationIds.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(","));

                    showPrompt = true;
                    message = "Refreshing this experience list will stop the currently open experiences on Stations: " + joinedString;
                }
            }
        }

        if (!showPrompt) {
            refreshSteamGamesList();
            return;
        }

        BooleanCallbackInterface booleanCallbackInterface = result -> {
            if (result) {
                refreshSteamGamesList();
            }
        };
        DialogManager.createConfirmationDialog(
                "Attention",
                message,
                booleanCallbackInterface,
                "Cancel",
                "Refresh",
                false);
    }

    /**
     * Refresh the list of steam applications on a particular computer or on all.
     */
    private void refreshSteamGamesList() {
        String stationIds;
        if(stationId > 0) {
            stationIds = String.valueOf(ApplicationAdapter.stationId);
        } else {
            stationIds = String.join(", ", Arrays.stream(mViewModel.getAllStationIds()).mapToObj(String::valueOf).toArray(String[]::new));
        }

        //BACKWARDS COMPATIBILITY - JSON Messaging system with fallback
        if (MainActivity.isNucJsonEnabled) {
            JSONObject message = new JSONObject();
            try {
                message.put("Action", "Refresh");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            NetworkService.sendMessage("Station," + stationIds, "Experience", message.toString());
        }
        else {
            NetworkService.sendMessage("Station," + stationIds, "Experience", "Refresh");
        }
    }
}
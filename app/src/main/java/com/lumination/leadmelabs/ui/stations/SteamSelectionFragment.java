package com.lumination.leadmelabs.ui.stations;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentSteamSelectionBinding;
import com.lumination.leadmelabs.models.SteamApplication;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.systemStatus.SystemStatusFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class SteamSelectionFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private static SteamApplicationAdapter steamApplicationAdapter;
    private static ArrayList<SteamApplication> steamApplicationList;
    private static int stationId = 0;
    private static FragmentSteamSelectionBinding binding;
    public static FragmentManager childManager;

    public static void setStationId (int stationId) {
        SteamSelectionFragment.stationId = stationId;
        if (stationId > 0) {
            steamApplicationList = (ArrayList<SteamApplication>) mViewModel.getStationSteamApplications(stationId);
        } else {
            steamApplicationList = (ArrayList<SteamApplication>) mViewModel.getAllSteamApplications();
        }
        if (steamApplicationAdapter != null) {
            SteamApplicationAdapter.stationId = stationId;
            steamApplicationAdapter.steamApplicationList = (ArrayList<SteamApplication>) steamApplicationList.clone();
            binding.setSteamApplicationList(steamApplicationAdapter.steamApplicationList);
            steamApplicationAdapter.notifyDataSetChanged();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_steam_selection, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);
        return view;
    }

    private void updateSteamApplicationList(int stationId, GridView view) {
        if (stationId > 0) {
            steamApplicationList = (ArrayList<SteamApplication>) mViewModel.getStationSteamApplications(stationId);
        } else {
            steamApplicationList = (ArrayList<SteamApplication>) mViewModel.getAllSteamApplications();
        }
        steamApplicationAdapter.steamApplicationList = (ArrayList<SteamApplication>) steamApplicationList.clone();
        binding.setSteamApplicationList(steamApplicationAdapter.steamApplicationList);
        binding.setSteamApplicationsLoaded(mViewModel.getAllSteamApplications().size() > 0);
        steamApplicationAdapter.notifyDataSetChanged();
        view.setAdapter(steamApplicationAdapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            childManager.beginTransaction()
                    .replace(R.id.logo, LogoFragment.class, null)
                    .replace(R.id.system_status, SystemStatusFragment.class, null)
                    .commitNow();
        }

        GridView steamGridView = (GridView) view.findViewById(R.id.steam_list);
        steamApplicationAdapter = new SteamApplicationAdapter(getContext());
        updateSteamApplicationList(stationId, steamGridView);
        mViewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
            if (stationId > 0) {
                if (steamApplicationAdapter.steamApplicationList.size() != mViewModel.getStationSteamApplications(stationId).size()) {
                    updateSteamApplicationList(stationId, steamGridView);
                }
            } else {
                if (steamApplicationAdapter.steamApplicationList.size() != mViewModel.getAllSteamApplications().size()) {
                    updateSteamApplicationList(stationId, steamGridView);
                }
            }
        });
        updateSteamApplicationList(stationId, steamGridView);

        EditText searchInput = view.findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String searchTerm = searchInput.getText().toString();
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
        FlexboxLayout steamListContainer = view.findViewById(R.id.steam_list_container);
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

        Button refresh_btn = view.findViewById(R.id.refresh_steam_btn);
        refresh_btn.setOnClickListener(v -> refreshSteamGamesList());
    }

    private void performSearch(String searchTerm) {
        ArrayList<SteamApplication> filteredSteamApplicationList = (ArrayList<SteamApplication>) steamApplicationList.clone();
        filteredSteamApplicationList.removeIf(steamApplication -> !steamApplication.name.toLowerCase(Locale.ROOT).contains(searchTerm.trim()));
        steamApplicationAdapter.steamApplicationList = filteredSteamApplicationList;
        binding.setSteamApplicationList(steamApplicationAdapter.steamApplicationList);
        steamApplicationAdapter.notifyDataSetChanged();
    }

    private void dismissKeyboard(View searchInput) {
        InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(searchInput.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * Refresh the list of steam applications on a particular computer or on all.
     */
    private void refreshSteamGamesList() {
        if(stationId > 0) {
            NetworkService.sendMessage("Station," + SteamApplicationAdapter.stationId, "Steam", "Refresh");
        } else {
            String stationIds = String.join(", ", Arrays.stream(mViewModel.getAllStationIds()).mapToObj(String::valueOf).toArray(String[]::new));
            NetworkService.sendMessage("Station," + stationIds, "Steam", "Refresh");
        }
    }
}
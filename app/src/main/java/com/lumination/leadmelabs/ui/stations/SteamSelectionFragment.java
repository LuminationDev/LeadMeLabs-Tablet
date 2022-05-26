package com.lumination.leadmelabs.ui.stations;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentSteamSelectionBinding;
import com.lumination.leadmelabs.models.SteamApplication;

import java.util.ArrayList;
import java.util.Locale;

public class SteamSelectionFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private View view;
    private SteamApplicationAdapter steamApplicationAdapter;
    private ArrayList<SteamApplication> steamApplicationList;
    private FragmentSteamSelectionBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_steam_selection, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GridView steamGridView = (GridView) view.findViewById(R.id.steam_list);
        steamApplicationAdapter = new SteamApplicationAdapter(getContext(), mViewModel);
        steamApplicationList = (ArrayList<SteamApplication>) mViewModel.getAllSteamApplications();
        steamApplicationAdapter.steamApplicationList = (ArrayList<SteamApplication>) steamApplicationList.clone();
        steamGridView.setAdapter(steamApplicationAdapter);


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
    }

    private void performSearch(String searchTerm) {
        ArrayList<SteamApplication> filteredSteamApplicationList = (ArrayList<SteamApplication>) steamApplicationList.clone();
        filteredSteamApplicationList.removeIf(steamApplication -> !steamApplication.name.toLowerCase(Locale.ROOT).contains(searchTerm.trim()));
        steamApplicationAdapter.steamApplicationList = filteredSteamApplicationList;
        steamApplicationAdapter.notifyDataSetChanged();
    }

    private void dismissKeyboard(View searchInput) {
        InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(searchInput.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
    }
}
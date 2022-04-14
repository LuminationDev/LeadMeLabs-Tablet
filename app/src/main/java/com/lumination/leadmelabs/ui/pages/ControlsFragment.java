package com.lumination.leadmelabs.ui.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.menu.SideMenuFragment;

public class ControlsFragment extends Fragment {
    private View view;
    private FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.page_controls, container, false);
        childManager = getChildFragmentManager();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            loadFragments();
        }
    }

    /**
     * Load in the initial fragments for the main view.
     */
    private void loadFragments() {
        childManager.beginTransaction()
                .replace(R.id.side_menu, SideMenuFragment.class, null)
                .replace(R.id.appliances, ApplianceFragment.class, null)
                .replace(R.id.logo, LogoFragment.class, null)
                .commitNow();
    }
}

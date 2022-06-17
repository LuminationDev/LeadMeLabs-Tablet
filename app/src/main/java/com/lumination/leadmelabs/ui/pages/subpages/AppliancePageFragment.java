package com.lumination.leadmelabs.ui.pages.subpages;

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

public class AppliancePageFragment extends Fragment {
    private FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.subpage_appliance, container, false);
        childManager = getChildFragmentManager();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            Bundle info = getArguments();
            loadFragments(info);
        }
    }

    /**
     * Load in the initial fragments for the main view.
     */
    private void loadFragments(Bundle args) {
        childManager.beginTransaction()
                .replace(R.id.appliances, ApplianceFragment.class, args)
                .commitNow();
    }
}

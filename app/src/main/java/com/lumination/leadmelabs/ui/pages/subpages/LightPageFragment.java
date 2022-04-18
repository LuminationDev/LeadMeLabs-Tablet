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
import com.lumination.leadmelabs.ui.appliance.LightFragment;

public class LightPageFragment extends Fragment {
    private View view;
    private FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.subpage_light, container, false);
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
                .replace(R.id.appliances, LightFragment.class, null)
                .commitNow();
    }
}

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
import com.lumination.leadmelabs.ui.room.RoomFragment;

public class ControlPageFragment extends Fragment {
    public static FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.page_controls, container, false);
        childManager = getChildFragmentManager();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            loadScenes();
        }
    }

    /**
     * Load in the initial fragments for the main view and replace the side menu.
     */
    private void loadScenes() {
        Bundle args = new Bundle();
        args.putString("title", "Scenes");
        args.putString("type", "scenes");

        childManager.beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.subpage, ApplianceFragment.class, args)
                .replace(R.id.logo, LogoFragment.class, null)
                .replace(R.id.rooms, RoomFragment.class, null)
                .addToBackStack("submenu:" + "scenes")
                .commit();

        childManager.executePendingTransactions();
    }
}

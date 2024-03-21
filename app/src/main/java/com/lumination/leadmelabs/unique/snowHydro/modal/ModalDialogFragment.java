package com.lumination.leadmelabs.unique.snowHydro.modal;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentModalBinding;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.unique.snowHydro.modal.backdrop.BackdropFragment;
import com.lumination.leadmelabs.unique.snowHydro.modal.layout.LayoutFragment;

public class ModalDialogFragment extends DialogFragment {
    public static StationsViewModel mViewModel;

    public FragmentModalBinding binding;
    public static FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modal, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setStations(mViewModel);

        //Specifically set the selected station - used to populate the layouts parts
        Station newlySelectedStation = mViewModel.getSelectedStation().getValue();
        binding.setSelectedStation(newlySelectedStation);

        //Specifically set the selected nested station(s) - used to populate the backdrop parts
        if (newlySelectedStation != null) {
            binding.setSelectedNestedStation(newlySelectedStation.getFirstNestedStationOrNull());
        }

        // Switch between the different sub libraries
        FlexboxLayout vrButton = view.findViewById(R.id.view_layouts_button);
        vrButton.setOnClickListener(v -> switchLayoutTab("layouts"));

        FlexboxLayout videoButton = view.findViewById(R.id.view_backdrops_button);
        videoButton.setOnClickListener(v -> switchLayoutTab("backdrops"));

        // Detect which tab to show by default
        String onLoadType = mViewModel.getLayoutTab().getValue();
        if(onLoadType == null){
            onLoadType = "layouts";
        }

        if (savedInstanceState == null) {
            switchLayoutTab(onLoadType);
        }

        // Close the modal
        FlexboxLayout closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> {
            dismiss();
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create a Dialog object with custom window attributes
        Dialog dialog = new Dialog(requireContext(), R.style.CurvedDialogTheme);

        // Set window dimensions or any other window attributes here
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.x = getResources().getDimensionPixelSize(R.dimen.dialog_x_offset); // Adjust the X position
            window.setAttributes(params);
        }

        // Return the created dialog
        return dialog;
    }

    /**
     * Switches the layouts based on the provided tab type.
     * Updates ViewModel properties and commits the transaction.
     *
     * @param tab The type of tab to switch to.
     */
    private void switchLayoutTab(String tab) {
        FragmentTransaction transaction = childManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out);

        switch (tab) {
            case "backdrops":
                transaction.replace(R.id.sub_layout, new BackdropFragment());
                break;

            case "layouts":
                transaction.replace(R.id.sub_layout, new LayoutFragment());
                break;
        }

        // Update the library type
        mViewModel.setLayoutTab(tab);

        transaction.commitNow();
    }
}

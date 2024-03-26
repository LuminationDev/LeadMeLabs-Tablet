package com.lumination.leadmelabs.unique.snowHydro.modal.layout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentModalLayoutsBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.unique.snowHydro.modal.ModalDialogFragment;
import com.lumination.leadmelabs.utilities.Constants;

import java.util.ArrayList;
import java.util.List;

public class LayoutFragment extends Fragment {
    private FragmentModalLayoutsBinding binding;

    public static LayoutAdapter layoutAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modal_layouts, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        //Specifically set the selected station - used to populate the layouts parts
        Station newlySelectedStation = ModalDialogFragment.mViewModel.getSelectedStation().getValue();
        binding.setSelectedStation(newlySelectedStation);

        //Set the adapter for backdrops
        if (newlySelectedStation != null) {
            GridView layoutGridView = view.findViewById(R.id.layout_section);
            layoutAdapter = new LayoutAdapter(getContext());
            reloadLayouts(); //Get the led wall (NovaStar) options
            layoutGridView.setAdapter(layoutAdapter);
        }

        //Observe any changes to the layout appliances
        ApplianceFragment.mViewModel.getAppliances().observe(getViewLifecycleOwner(), v -> reloadLayouts());
    }

    public void reloadLayouts() {
        ArrayList<Appliance.Option> allOptions = new ArrayList<>();

        List<Appliance> allAppliances = ApplianceFragment.mViewModel.getAppliances().getValue();
        if (allAppliances == null) return;

        for (Appliance appliance : allAppliances) {
            if (appliance.matchesDisplayCategory(Constants.LED_WALLS)) {
                allOptions.addAll(appliance.options);
            }
        }

        if (layoutAdapter != null) {
            layoutAdapter.setLayoutList(allOptions);
        }
    }
}

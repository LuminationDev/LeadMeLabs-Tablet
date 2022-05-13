package com.lumination.leadmelabs.ui.zones;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentZonesBinding;
import com.lumination.leadmelabs.models.Zone;

import java.util.ArrayList;

public class ZonesFragment extends Fragment {

    public static ZonesViewModel mViewModel;
    private View view;
    private ZoneAdapter zoneAdapter;
    private FragmentZonesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_zones, container, false);
        binding = DataBindingUtil.bind(view);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.zone_list);
        zoneAdapter = new ZoneAdapter(binding);
        zoneAdapter.zoneList = new ArrayList<>();
        recyclerView.setAdapter(zoneAdapter);

        Button backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            binding.selectedZone.setVisibility(View.GONE);
            binding.zoneSelection.setVisibility(View.VISIBLE);
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(ZonesViewModel.class);

        mViewModel.getZones().observe(getViewLifecycleOwner(), zones -> {
            zoneAdapter.zoneList = (ArrayList<Zone>) zones;
            zoneAdapter.notifyDataSetChanged();
            if (zoneAdapter.sceneAdapter != null) {
                zoneAdapter.sceneAdapter.animate = false;
                zoneAdapter.sceneAdapter.notifyDataSetChanged();
                new java.util.Timer().schedule( // turn animations back on after the scenes have updated
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                zoneAdapter.sceneAdapter.animate = true;
                            }
                        },
                        100
                );
            }
        });
    }
}
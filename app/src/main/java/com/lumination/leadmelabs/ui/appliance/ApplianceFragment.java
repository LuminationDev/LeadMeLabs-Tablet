package com.lumination.leadmelabs.ui.appliance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.Appliance;

import java.util.ArrayList;

public class ApplianceFragment extends Fragment {

    public static ApplianceViewModel mViewModel;
    private View view;
    private ApplianceAdapter applianceAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_appliances, container, false);
        GridView gridView = (GridView) view.findViewById(R.id.appliance_list);
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        applianceAdapter = new ApplianceAdapter(getContext(), gridView);
        applianceAdapter.applianceList = new ArrayList<>();
        gridView.setAdapter(applianceAdapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(ApplianceViewModel.class);

        mViewModel.getAppliances().observe(getViewLifecycleOwner(), appliances -> {
            applianceAdapter.applianceList = (ArrayList<Appliance>) appliances;
            applianceAdapter.notifyDataSetChanged();
        });
    }
}
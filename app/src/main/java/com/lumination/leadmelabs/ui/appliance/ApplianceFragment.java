package com.lumination.leadmelabs.ui.appliance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.Appliance;

import java.util.ArrayList;

public class ApplianceFragment extends Fragment {
    public static ApplianceViewModel mViewModel;
    private String type;
    private String title;
    private View view;
    private ApplianceAdapter applianceAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_appliance, container, false);

        Bundle bundle = getArguments();
        title = bundle != null ? bundle.getString("title") : null;
        type = bundle != null ? bundle.getString("type") : null;
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView titleView = view.findViewById(R.id.appliance_title);
        titleView.setText(title);

        GridView gridView = (GridView) view.findViewById(R.id.appliance_list);
        applianceAdapter = new ApplianceAdapter(getContext(), gridView);
        applianceAdapter.applianceList = new ArrayList<>();
        gridView.setAdapter(applianceAdapter);

        //Only add objects that are of the same sub type as the supplied argument
        mViewModel.getAppliances().observe(getViewLifecycleOwner(), appliances -> {
            ArrayList<Appliance> subtype = new ArrayList<>();

            for(Appliance appliance : appliances) {
                if(appliance.type.equals(type)) {
                    subtype.add(appliance);
                }
            }

            applianceAdapter.applianceList = (ArrayList<Appliance>) subtype;
            applianceAdapter.notifyDataSetChanged();
        });

        mViewModel.getActiveAppliances().observe(getViewLifecycleOwner(), active -> {
            ApplianceViewModel.activeApplianceList = (ArrayList<String>) active;
            applianceAdapter.notifyDataSetChanged();
        });
    }
}

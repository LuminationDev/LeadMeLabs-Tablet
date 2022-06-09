package com.lumination.leadmelabs.ui.appliance;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.room.RoomViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ApplianceFragment extends Fragment {
    public static ApplianceViewModel mViewModel;
    private String type;
    private String title;
    public ApplianceAdapter applianceAdapter;

    public static ApplianceFragment instance;
    public static ApplianceFragment getInstance() { return instance; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appliance, container, false);
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

        GridView gridView = view.findViewById(R.id.appliance_list);
        applianceAdapter = new ApplianceAdapter(getContext());
        applianceAdapter.applianceList = new ArrayList<>();
        gridView.setAdapter(applianceAdapter);

        //Only add objects that are of the same sub type as the supplied argument
        mViewModel.getAppliances().observe(getViewLifecycleOwner(), this::reloadData);

        mViewModel.getActiveAppliances().observe(getViewLifecycleOwner(), active -> {
            ApplianceViewModel.activeApplianceList = (HashSet<String>) active;
            applianceAdapter.notifyDataSetChanged();
        });

        instance = this;
    }

    /**
     * Reload the current appliance list when a room is changed.
     */
    public void notifyDataChange() {
        mViewModel.getAppliances().observe(getViewLifecycleOwner(), this::reloadData);
    }

    private void reloadData(List<Appliance> appliances) {
        ArrayList<Appliance> subtype = new ArrayList<>();

        String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();
        if(roomType == null) {
            roomType = "All";
        }

        for(Appliance appliance : appliances) {
            if(appliance.type.equals(type) && roomType.equals("All")) {
                subtype.add(appliance);
            } else if(appliance.type.equals(type) && appliance.room.equals(roomType)) {
                subtype.add(appliance);
            }
        }

        applianceAdapter.applianceList = subtype;
        applianceAdapter.notifyDataSetChanged();
    }
}

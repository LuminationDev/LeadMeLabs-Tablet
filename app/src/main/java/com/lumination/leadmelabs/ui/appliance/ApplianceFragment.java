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
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.room.RoomViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ApplianceFragment extends Fragment {
    public static ApplianceViewModel mViewModel;
    private String title;
    public static MutableLiveData<String> type = new MutableLiveData<>();
    public static MutableLiveData<Integer> applianceCount = new MutableLiveData<>(0);
    public ApplianceAdapter applianceAdapter;

    public FragmentApplianceBinding binding;

    public static ApplianceFragment instance;
    public static ApplianceFragment getInstance() { return instance; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appliance, container, false);
        binding = DataBindingUtil.bind(view);

        Bundle bundle = getArguments();
        title = bundle != null ? bundle.getString("title") : null;
        type.setValue(bundle != null ? bundle.getString("type") : null);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.setLifecycleOwner(this);

        TextView titleView = view.findViewById(R.id.appliance_title);
        titleView.setText(title);

        GridView gridView = view.findViewById(R.id.appliance_list);
        applianceAdapter = new ApplianceAdapter(getContext());
        applianceAdapter.applianceList = new ArrayList<>();
        gridView.setAdapter(applianceAdapter);

        applianceCount.setValue(applianceAdapter.applianceList.size());

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
            if(appliance.type.equals(type.getValue()) && roomType.equals("All")) {
                subtype.add(appliance);
            } else if(appliance.type.equals(type.getValue()) && appliance.room.equals(roomType)) {
                subtype.add(appliance);
            }
        }

        applianceCount.setValue(subtype.size());
        applianceAdapter.applianceList = subtype;
        applianceAdapter.notifyDataSetChanged();
    }
}

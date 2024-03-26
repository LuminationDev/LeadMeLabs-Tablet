package com.lumination.leadmelabs.unique.snowHydro.modal.layout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.databinding.CardLayoutBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import java.util.ArrayList;

public class LayoutAdapter extends BaseAdapter {
    public ArrayList<Appliance.Option> layoutList = new ArrayList<>();
    private final LayoutInflater mInflater;

    public static StationsViewModel mViewModel;

    public LayoutAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
        mViewModel = ViewModelProviders.of((FragmentActivity) context).get(StationsViewModel.class);
    }

    public void setLayoutList(ArrayList<Appliance.Option> newLayouts) {
        layoutList = newLayouts;
    }

    @Override
    public int getCount()  {
        return layoutList != null ? layoutList.size() : 0;
    }

    @Override
    public Appliance.Option getItem(int position)  {
        return layoutList.get(position);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            ViewDataBinding binding;
            binding = CardLayoutBinding.inflate(mInflater, parent, false);
            view = binding.getRoot();
            view.setTag(binding);
        }

//        ViewDataBinding binding = (ViewDataBinding) view.getTag();
//        Video currentVideo = getItem(position);
//        Helpers.SetVideoImage(currentVideo.getId(), view);
//        binding.setVariable(BR.video, currentVideo);
//
//        Station station = mViewModel.getSelectedStation().getValue();
//        if (station != null) {
//            Station nestedStation = station.getFirstNestedStationOrNull();
//            binding.setVariable(BR.selectedNestedStation, nestedStation);
//            view.setOnClickListener(v -> {
//                if (nestedStation != null) {
//                    nestedStation.checkForVideoPlayer(currentVideo);
//                }
//            });
//        }

        return view;
    }
}

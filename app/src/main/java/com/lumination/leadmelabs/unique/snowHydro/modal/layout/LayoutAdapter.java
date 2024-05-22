package com.lumination.leadmelabs.unique.snowHydro.modal.layout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import androidx.databinding.ViewDataBinding;
import androidx.databinding.library.baseAdapters.BR;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardLayoutBinding;
import com.lumination.leadmelabs.models.Option;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Helpers;
import com.lumination.leadmelabs.utilities.Interlinking;

import java.util.ArrayList;

public class LayoutAdapter extends BaseAdapter {
    public ArrayList<Option> layoutList = new ArrayList<>();
    private final LayoutInflater mInflater;

    public static StationsViewModel mViewModel;

    public LayoutAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
        mViewModel = ViewModelProviders.of((FragmentActivity) context).get(StationsViewModel.class);
    }

    public void setLayoutList(ArrayList<Option> newLayouts) {
        layoutList = newLayouts;
    }

    @Override
    public int getCount()  {
        return layoutList != null ? layoutList.size() : 0;
    }

    @Override
    public Option getItem(int position)  {
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

        ViewDataBinding binding = (ViewDataBinding) view.getTag();
        Option currentOption = getItem(position);
        Helpers.SetOptionImage(currentOption.getName(), view.findViewById(R.id.placeholder_image), view);
        binding.setVariable(BR.option, currentOption);

        view.setOnClickListener(v -> {
            //Launch a backdrop on the nested Station(s) when switching between the layouts
            //(this may be the rings or logo video)
            Interlinking.performAssociatedLayoutActions(mViewModel.getSelectedStation().getValue(), currentOption.getName());

            //additionalData break down
            //Action : [cbus unit : group address : id address : value] : [type : room : id appliance]
            NetworkService.sendMessage("NUC",
                    "Automation",
                    "Set" + ":"
                            + currentOption.getParentId() + ":"
                            + currentOption.getId() + ":"
                            + NetworkService.getIPAddress());
        });

        return view;
    }
}

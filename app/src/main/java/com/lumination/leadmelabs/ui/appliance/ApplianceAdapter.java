package com.lumination.leadmelabs.ui.appliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Appliance;

import java.util.ArrayList;

/**
 * Use this adapter for scripts in the future. Acts as a singleton when individual rooms are
 * displayed. When 'All' rooms are chosen it acts as a regular class access through references in
 * the parent adapter class.
 */
public class ApplianceAdapter extends BaseAdapter {
    private final ApplianceController applianceController = new ApplianceController();

    public ArrayList<Appliance> applianceList = new ArrayList<>();


    public class ApplianceViewHolder extends BaseAdapter.BaseViewHolder {
        private final CardApplianceBinding binding;
        private boolean recentlyClicked = false;

        public ApplianceViewHolder(@NonNull CardApplianceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Appliance appliance) {
            binding.setAppliance(appliance);
            View finalResult = binding.getRoot().findViewById(R.id.appliance_card);

            //Load what appliance is active or not
            String status = applianceController.determineIfActive(appliance, finalResult);
            binding.setStatus(new MutableLiveData<>(status));
            ApplianceController.setIcon(binding);

            finalResult.setOnClickListener(v -> {
                int timeout = 500;
                String title = "Warning";
                String content = "The automation system performs best when appliances are not repeatedly turned on and off. Try waiting half a second before toggling an appliance.";
                if (appliance.type.equals("projectors")) {
                    timeout = 10000;
                    content = "The automation system performs best when appliances are not repeatedly turned on and off. Projectors need up to 10 seconds between turning on and off.";
                }
                if (appliance.type.equals("computers")) {
                    timeout = 20000;
                    content = "The automation system performs best when appliances are not repeatedly turned on and off. Computers need up to 20 seconds between turning on and off.";
                }
                if (recentlyClicked) {
                    DialogManager.createBasicDialog(title, content);
                    return;
                }
                recentlyClicked = true;
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() { recentlyClicked = false; }
                        },
                        timeout
                );

                applianceController.strategyType(binding, appliance, finalResult);
            });
        }
    }

    @Override
    public ArrayList<Appliance> getApplianceList() {
        return applianceList;
    }
    @Override
    public void setApplianceList(ArrayList<Appliance> newAppliances) {
        applianceList = newAppliances;
    }

    @NonNull
    @Override
    public ApplianceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        CardApplianceBinding binding = CardApplianceBinding.inflate(layoutInflater, parent, false);
        return new ApplianceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        ApplianceViewHolder applianceViewHolder = (ApplianceViewHolder) holder;
        Appliance appliance = getItem(position);
        applianceViewHolder.bind(appliance);
    }
}

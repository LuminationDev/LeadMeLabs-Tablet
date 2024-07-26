package com.lumination.leadmelabs.ui.appliance.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardApplianceSceneBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.ui.appliance.controllers.ApplianceController;
import com.lumination.leadmelabs.ui.dashboard.DashboardFragment;
import com.lumination.leadmelabs.utilities.Constants;

import java.util.ArrayList;

/**
 * Use this adapter for scripts in the future. Acts as a singleton when individual rooms are
 * displayed. When 'All' rooms are chosen it acts as a regular class access through references in
 * the parent adapter class.
 */
public class SceneAdapter extends BaseAdapter {
    private final ApplianceController applianceController = new ApplianceController();

    public ArrayList<Appliance> applianceList = new ArrayList<>();

    public class ApplianceViewHolder extends BaseViewHolder {
        private final CardApplianceSceneBinding binding;
        private boolean recentlyClicked = false;

        public ApplianceViewHolder(@NonNull CardApplianceSceneBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Appliance appliance) {
            binding.setAppliance(appliance);
            View finalResult = binding.getRoot().findViewById(R.id.appliance_card);

            //Load what appliance is active or not
            String status = applianceController.determineIfActive(appliance, finalResult);
            binding.setStatus(new MutableLiveData<>(status));
            ApplianceController.setIcon(binding, Constants.SCENE);

            finalResult.setOnClickListener(v -> {
                String sceneStatus = binding.getStatus().getValue();
                if(sceneStatus != null && sceneStatus.equals(Constants.DISABLED)) {
                    DashboardFragment.getInstance().changingModePrompt(binding.getAppliance().name);
                    return;
                }

                int timeout = 500;
                String title = "Warning";
                String content = "The automation system performs best when appliances are not repeatedly turned on and off. Try waiting half a second before toggling an appliance.";
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
        CardApplianceSceneBinding binding = CardApplianceSceneBinding.inflate(layoutInflater, parent, false);
        return new ApplianceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        ApplianceViewHolder applianceViewHolder = (ApplianceViewHolder) holder;
        Appliance appliance = getItem(position);
        applianceViewHolder.bind(appliance);
    }
}

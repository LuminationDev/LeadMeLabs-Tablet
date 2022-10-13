package com.lumination.leadmelabs.ui.appliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.utilities.Constants;

import java.util.ArrayList;

/**
 * Use this adapter for scripts in the future. Acts as a singleton when individual rooms are
 * displayed. When 'All' rooms are chosen it acts as a regular class access through references in
 * the parent adapter class.
 */
public class ApplianceAdapter extends RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder> {
    public static ApplianceAdapter instance;
    public static ApplianceAdapter getInstance() { return instance; }
    private final ApplianceController applianceController = new ApplianceController();

    public ArrayList<Appliance> applianceList = new ArrayList<>();

    public ApplianceAdapter() {
        instance = this;
    }

    public class ApplianceViewHolder extends RecyclerView.ViewHolder {
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

    @NonNull
    @Override
    public ApplianceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        CardApplianceBinding binding = CardApplianceBinding.inflate(layoutInflater, parent, false);
        return new ApplianceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplianceViewHolder holder, int position) {
        Appliance appliance = getItem(position);
        holder.bind(appliance);
    }

    //Accessors
    @Override
    public int getItemCount() {
        return applianceList != null ? applianceList.size() : 0;
    }

    public Appliance getItem(int position) {
        return applianceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    /**
     * Update the data set only if the card with the supplied ID is visible otherwise it will be
     * update when it is next visible automatically with the new ViewHolder creation.
     * @param id A string representing the ID of the appliance.
     */
    public void updateIfVisible(String id) {
        for(int i=0; i < applianceList.size(); i++) {
            if(applianceList.get(i).id.equals(id)) {
                int finalI = i;
                MainActivity.runOnUI(() ->
                    notifyItemChanged(finalI)
                );
            }
        }
    }
}

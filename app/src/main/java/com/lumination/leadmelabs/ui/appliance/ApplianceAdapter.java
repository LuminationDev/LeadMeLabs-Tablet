package com.lumination.leadmelabs.ui.appliance;

import android.graphics.drawable.TransitionDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;

import java.util.ArrayList;

/**
 * Use this adapter for scripts in the future. Acts as a singleton when individual rooms are
 * displayed. When 'All' rooms are chosen it acts as a regular class access through references in
 * the parent adapter class.
 */
public class ApplianceAdapter extends RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder> {
    public static ApplianceAdapter instance;
    public static ApplianceAdapter getInstance() { return instance; }

    public ArrayList<Appliance> applianceList = new ArrayList<>();
    public static ArrayList<String> latestOn = new ArrayList<>();
    public static ArrayList<String> latestOff = new ArrayList<>();

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

        public void bind(Appliance appliance, int position) {
            binding.setAppliance(appliance);
            View finalResult = binding.getRoot().findViewById(R.id.appliance_card);
            String id = String.valueOf(appliance.id);

            boolean active;

            //Determine if the appliance is active
            if(getItemType(position).equals("scenes")) {
                //This is applicable when first starting up the application
                if(ApplianceViewModel.activeScenes.getValue() != null) {
                    if (ApplianceViewModel.activeScenes.getValue().contains(String.valueOf(getItem(position).id))) {
                        ApplianceViewModel.activeSceneList.put(getItemRoom(position),getItem(position));
                        ApplianceViewModel.activeScenes.getValue().remove(String.valueOf(getItem(position).id));
                    }
                }

                active = ApplianceViewModel.activeSceneList.containsValue(getItem(position));
                sceneTransition(active, id, finalResult);

            } else {
                active = ApplianceViewModel.activeApplianceList.contains(id);

                TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
                if(active) {
                    transition.startTransition(200);
                } else {
                    transition.resetTransition();
                }
            }

            //Load what appliance is active or not
            setIcon(binding, active);
            binding.setIsActive(new MutableLiveData<>(active));

            finalResult.setOnClickListener(v -> {
                String type = getItemType(position);
                int timeout = 500;
                String title = "Warning";
                String content = "The automation system performs best when appliances are not repeatedly turned on and off. Try waiting half a second before toggling an appliance.";
                if (type.equals("projectors")) {
                    timeout = 10000;
                    content = "The automation system performs best when appliances are not repeatedly turned on and off. Projectors need up to 10 seconds between turning on and off.";
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

                switch (type) {
                    case "scenes":
                        sceneStrategy(binding, appliance);
                        break;
                    case "blinds":
                        //Open the blind widget when that is built for now just act as a toggle
                    case "source":
                        //Toggle the source when that is implemented for now just act as a toggle
                    case "LED rings":
                        toggleStrategy(binding, appliance, finalResult, "153", "0");
                        break;
                    default:
                        toggleStrategy(binding, appliance, finalResult);
                        break;
                }
            });
        }
    }

    @NonNull
    @Override
    public ApplianceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        CardApplianceBinding binding = CardApplianceBinding.inflate(layoutInflater, parent, false);
        return new ApplianceAdapter.ApplianceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplianceViewHolder holder, int position) {
        Appliance appliance = getItem(position);
        ((ApplianceViewHolder) holder).bind(appliance, position);
    }

    //Accessors
    @Override
    public int getItemCount() {
        return applianceList != null ? applianceList.size() : 0;
    }

    public Appliance getItem(int position) {
        return applianceList.get(position);
    }

    public String getItemRoom(int position) {
        return applianceList.get(position).room;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public String getItemType(int position) { return applianceList.get(position).type; }

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

    /**
     * Depending on an appliances type add an icon.
     * @param binding A SceneCardBinding relating associated with the current scene.
     * @param active A Boolean representing if the appliance is currently on or off
     */
    private void setIcon(CardApplianceBinding binding, Boolean active) {
        if(binding.getAppliance().type.equals("scenes")) {
            determineSceneType(binding, active);
        } else {
            determineApplianceType(binding, active);
        }
    }

    /**
     * Determine what icon a scene needs depending on their name.
     */
    private void determineSceneType(CardApplianceBinding binding, Boolean active) {
        MutableLiveData<Integer> icon;

        switch (binding.getAppliance().name) {
            case "Classroom":
                icon = active ? new MutableLiveData<>(R.drawable.icon_scene_classroommode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_classroommode_off);
                break;
            case "VR Mode":
                icon = active ? new MutableLiveData<>(R.drawable.icon_scene_vrmode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_vrmode_off);
                break;
            case "Theatre":
                icon = active ? new MutableLiveData<>(R.drawable.icon_scene_theatremode_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_theatremode_off);
                break;
            case "Off":
                icon = active ? new MutableLiveData<>(R.drawable.icon_scene_power_on) :
                        new MutableLiveData<>(R.drawable.icon_scene_power_off);
                break;
            default:
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
        }

        binding.setIcon(icon);
    }

    /**
     * Determine what icon an appliance needs depending on their type.
     */
    private void determineApplianceType(CardApplianceBinding binding, Boolean active) {
        MutableLiveData<Integer> icon;

        //Add to this in the future
        switch(binding.getAppliance().type) {
            case "lights":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_light_bulb_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_light_bulb_off);
                break;
            case "blinds":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_blind_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_blind_off);
                break;
            case "projectors":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_projector_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_projector_off);
                break;
            case "LED rings":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_ring_on) :
                        new MutableLiveData<>(R.drawable.icon_appliance_ring_off);
                break;
            case "sources":
                icon = active ? new MutableLiveData<>(R.drawable.icon_appliance_source_2) :
                        new MutableLiveData<>(R.drawable.icon_appliance_source_1);
                break;
            default:
                icon = new MutableLiveData<>(R.drawable.icon_settings);
                break;
        }

        binding.setIcon(icon);
    }

    private void sceneTransition(boolean active, String id, View finalResult) {
        // just turned on
        if (latestOn.contains(id)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_fade, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(200);
            latestOn.remove(id);

            //just turned off
        } else if (latestOff.contains(id)) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_fade_active, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.startTransition(200);
            latestOff.remove(id);

            //already active
        } else if(active) {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_fade_active, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.resetTransition();

            //not active
        } else {
            finalResult.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_fade, null));
            TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();
            transition.resetTransition();
        }
    }

    //Strategies to control the units on the CBUS, there should be toggle and dimmer
    private void toggleStrategy(CardApplianceBinding binding, Appliance appliance, View finalResult) {
        toggleStrategy(binding, appliance, finalResult, "255", "0");
    }

    //Strategies to control the units on the CBUS, there should be toggle and dimmer
    private void toggleStrategy(CardApplianceBinding binding, Appliance appliance, View finalResult, String onValue, String offValue) {
        String type = "appliance";
        String value;
        TransitionDrawable transition = (TransitionDrawable) finalResult.getBackground();

        if(ApplianceViewModel.activeApplianceList.contains(String.valueOf(appliance.id))) {
            transition.reverseTransition(200);
            binding.setIsActive(new MutableLiveData<>(false));
            ApplianceViewModel.activeApplianceList.remove(String.valueOf(appliance.id));
            value = offValue;

        } else {
            binding.setIsActive(new MutableLiveData<>(true));
            transition.startTransition(200);
            ApplianceViewModel.activeApplianceList.add(String.valueOf(appliance.id));
            value = onValue;
        }

        //Set the new icon and send a message to the NUC
        setIcon(binding, ApplianceViewModel.activeApplianceList.contains(String.valueOf(appliance.id)));

        //additionalData break down
        //Action : [cbus unit : group address : id address : value] : [type : room : id appliance]
        NetworkService.sendMessage("NUC",
                "Automation",
                "Set" + ":"                         //[0] Action
                        + "0" + ":"                             //[1] CBUS unit number
                        + appliance.automationGroup + ":"       //[2] CBUS group address
                        + appliance.automationId  + ":"         //[3] CBUS unit address
                        + value + ":"                           //[4] New value for address
                        + type + ":"                            //[5] Object type (computer, appliance, scene)
                        + appliance.room + ":"                  //[6] Appliance room
                        + appliance.id);                        //[7] CBUS object id/doubles as card id
    }

    /**
     * A scene can be re-triggered if active, as sub elements can be changed while a scene is
     * active. If nothing has changed then the Cbus disregards the message.
     */
    private void sceneStrategy(CardApplianceBinding binding, Appliance scene) {
        String type = "scene";

        //Set the new icon and send a message to the NUC
        setIcon(binding, ApplianceViewModel.activeSceneList.containsValue(scene));

        binding.setIsActive(new MutableLiveData<>(true));
        Appliance last = ApplianceViewModel.activeSceneList.put(scene.room, scene);

        if(last != null && last != scene) {
            latestOff.add(last.id);
            updateIfVisible(last.id);
        }
        latestOn.add(scene.id);

        updateIfVisible(scene.id);

        //Action : [cbus unit : group address : id address : scene value] : [type : room : id scene]
        NetworkService.sendMessage("NUC",
                "Automation",
                "Set" + ":"                         //[0] Action
                        + "0" + ":"                             //[1] CBUS unit number
                        + scene.automationGroup + ":"           //[2] CBUS group address
                        + scene.automationId  + ":"             //[3] CBUS unit address
                        + scene.automationValue + ":"           //[4] New value for address
                        + type + ":"                            //[5] Object type (computer, appliance, scene)
                        + scene.room + ":"                      //[6] Appliance room
                        + scene.id);                            //[7] CBUS object id/doubles as card id


        //Cancel/start the timer to get the latest updated cards
        ApplianceViewModel.delayLoadCall();
    }
}

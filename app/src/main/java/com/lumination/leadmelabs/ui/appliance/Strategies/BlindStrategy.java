package com.lumination.leadmelabs.ui.appliance.Strategies;

import android.annotation.SuppressLint;
import android.graphics.drawable.TransitionDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.abstractClasses.AbstractApplianceStrategy;
import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceController;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;

import java.util.HashMap;
import java.util.Objects;

/**
 * Show the expanded blind element for the currently selected blind.
 * Create a transparent background layout that covers the entire page then add the blindCard
 * onto that. Create an on touch listener on the background page to remove both the blindCard
 * and background view when not clicking the blindCard.
 */
public class BlindStrategy extends AbstractApplianceStrategy {
    private View finalResult; //The underlying card view the extended blind xml is attached to.
    private TextView statusTitle; //The textview that the current status is to be displayed on.
    private CardApplianceBinding binding; //The underlying card model the extended blind xml is attached to.
    private Appliance appliance; //The populate appliance model.
    private View blindCard; // The inflated view for controlling the blinds.
    private static boolean isSceneCard;
    private String openValue = Constants.APPLIANCE_ON_VALUE;
    private String stopValue = Constants.BLIND_STOPPED_VALUE;
    private String closeValue = Constants.APPLIANCE_OFF_VALUE;

    public BlindStrategy(boolean isSceneCard) {
        BlindStrategy.isSceneCard = isSceneCard;
        if(isSceneCard) {
            openValue = "2";
            stopValue = "1";
            closeValue = "0";
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void trigger(CardApplianceBinding binding, Appliance appliance, View finalResult) {
        this.finalResult = finalResult;
        this.binding = binding;
        this.appliance = appliance;

        //Get the overall main root view
        ViewGroup mainView = (ViewGroup) MainActivity.getInstance().getWindow().getDecorView().findViewById(R.id.main).getRootView();

        //Inflate the background layer
        View background = ApplianceFragment.fragmentInflater.inflate(R.layout.background_placeholder, mainView, false);
        background.setClickable(true);
        mainView.addView(background);

        //Setup the blind card
        ViewGroup insertion = createBlindCard(mainView);
        determineStatus();
        setupButtons();

        //Start the fade in transition
        TransitionDrawable transition = (TransitionDrawable) blindCard.findViewById(R.id.appliance_card_blind).getBackground();
        transition.startTransition(ApplianceController.fadeTime);

        ImageView image = blindCard.findViewById(R.id.icon_appliance);

        View.OnTouchListener closeListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                transition.reverseTransition(ApplianceController.fadeTime);
                insertion.postDelayed(() -> insertion.removeView(blindCard), ApplianceController.fadeTime);

                //Remove the invisible background
                mainView.postDelayed(() -> mainView.removeView(background), ApplianceController.fadeTime);
            }

            return false;
        };

        //If anywhere besides the expanded blind card is touched or blind icon, remove the view
        image.setOnTouchListener(closeListener);
        background.setOnTouchListener(closeListener);
    }

    /**
     * Create the expanded blind card with the generated parameters to appear directly over the
     * row the underlying appliance card was located in, return the ViewGroup that it is added to.
     */
    private ViewGroup createBlindCard(ViewGroup root) {
        //Get the absolute position of the card within the window
        int[] out = new int[2];
        binding.getRoot().getLocationInWindow(out);

        //Inflate the expanded card view
        blindCard = ApplianceFragment.fragmentInflater.inflate(R.layout.card_appliance_blind, root, false);
        blindCard.setClickable(true);

        //Set the text of the selected card
        TextView name = blindCard.findViewById(R.id.blind_name);
        name.setText(appliance.name);

        //Set the layout parameters for where to add the view
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, Helpers.convertDpToPx(190));
        if(SettingsFragment.mViewModel.getHideStationControls().getValue()) {
            params.leftMargin = Helpers.convertDpToPx(110) + 275 + 70; //sub menu + regular menu + card spacing
        } else {
            params.leftMargin = Helpers.convertDpToPx(110) + 175 + 70; //sub menu + full regular menu + card spacing
        }

        params.rightMargin = 45 + 125; //card spacing + container end margin
        params.topMargin = out[1];

        //Add the blind card to the background view
        ViewGroup insertion = root.findViewById(R.id.background_placeholder);
        insertion.addView(blindCard, params);

        return insertion;
    }

    /**
     * Determine the description for the card.
     */
    private void determineStatus() {
        String statusContent;
        if (Objects.equals(binding.getStatus().getValue(), Constants.ACTIVE)) {
            statusContent = appliance.description.get(0);
        } else if (Objects.equals(binding.getStatus().getValue(), Constants.STOPPED)) {
            statusContent = appliance.description.get(2);
        } else {
            statusContent = appliance.description.get(1);
        }
        statusTitle = blindCard.findViewById(R.id.blind_status);
        statusTitle.setText(statusContent);
    }

    /**
     * Set the buttons to send messages to the NUC and the underlying card view
     */
    private void setupButtons() {
        Button openButton = blindCard.findViewById(R.id.blind_open);
        openButton.setOnClickListener(v -> {
            blindNetworkCall(appliance, openValue);

            if(!Objects.equals(ApplianceViewModel.activeApplianceList.get(appliance.id), Constants.APPLIANCE_ON_VALUE)) {
                if(isSceneCard) {
                    ApplianceViewModel.activeSceneList.put(appliance.name, appliance);
                }

                ApplianceViewModel.activeApplianceList.put(appliance.id, Constants.APPLIANCE_ON_VALUE);
                coordinateVisuals(R.drawable.transition_appliance_fade, appliance.description.get(0), Constants.ACTIVE);
            }
        });

        Button stopButton = blindCard.findViewById(R.id.blind_stop);
        stopButton.setOnClickListener(v -> {
            blindNetworkCall(appliance, stopValue);

            if(!Objects.equals(ApplianceViewModel.activeApplianceList.get(appliance.id), Constants.BLIND_STOPPED_VALUE)) {
                if(isSceneCard) {
                    ApplianceViewModel.activeSceneList.put(appliance.name, appliance);
                }

                ApplianceViewModel.activeApplianceList.put(appliance.id, Constants.BLIND_STOPPED_VALUE);
                coordinateVisuals(R.drawable.transition_blind_stopped, appliance.description.get(2), Constants.STOPPED);
            }
        });

        Button closeButton = blindCard.findViewById(R.id.blind_close);
        closeButton.setOnClickListener(v -> {
            blindNetworkCall(appliance, closeValue);

            if(ApplianceViewModel.activeApplianceList.containsKey(appliance.id)) {
                if(isSceneCard) {
                    ApplianceViewModel.activeSceneList.remove(appliance.name);
                }

                ApplianceViewModel.activeApplianceList.remove(appliance.id);
                coordinateVisuals(R.drawable.transition_appliance_fade_active, appliance.description.get(1), Constants.INACTIVE);
            }
        });
    }

    /**
     * Change the background depending on what the currently active status is.
     * @param transitionDrawable The transition that is required to happen.
     * @param current The text to be displayed to the user.
     * @param activeStatus The active status (active, inactive or stopped).
     */
    private void coordinateVisuals(int transitionDrawable, String current, String activeStatus) {
        ApplianceController.applianceTransition(finalResult, transitionDrawable);
        statusTitle.setText(current);
        binding.setStatus(new MutableLiveData<>(activeStatus));
    }

    /**
     * Make a custom network call for the blind appliance. Value needs to be supplied as it can be
     * one of three values, rather than the standard 2.
     * @param value An integer representing whether to open(255), stop(5) or close(0) a blind.
     */
    private static void blindNetworkCall(Appliance blind, String value) {
        String type = "appliance";

        //Action : [cbus unit : group address : id address : scene value] : [type : room : id scene]
        NetworkService.sendMessage("NUC",
                "Automation",
                "Set" + ":"                         //[0] Action
                        + blind.automationBase + ":"            //[1] CBUS unit number
                        + blind.automationGroup + ":"           //[2] CBUS group address
                        + blind.automationId  + ":"             //[3] CBUS unit address
                        + value + ":"                           //[4] New value for address
                        + type + ":"                            //[5] Object type (computer, appliance, scene)
                        + blind.room + ":"                      //[6] Appliance room
                        + blind.id + ":"                        //[7] CBUS object id/doubles as card id
                        + NetworkService.getIPAddress());       //[8] The IP address of the tablet

        if(isSceneCard) {
            //Cancel/start the timer to get the latest updated cards
            ApplianceViewModel.delayLoadCall();
        }

        HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
            put("appliance_type", blind.type);
            put("appliance_room", blind.room);
            put("appliance_new_value", blind.value);
            put("appliance_action_type", "radio");
        }};
        FirebaseManager.logAnalyticEvent("appliance_value_changed", analyticsAttributes);
    }
}

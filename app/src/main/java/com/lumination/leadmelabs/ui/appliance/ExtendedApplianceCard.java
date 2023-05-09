package com.lumination.leadmelabs.ui.appliance;

import android.annotation.SuppressLint;
import android.graphics.drawable.TransitionDrawable;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.MutableLiveData;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.abstractClasses.AbstractApplianceStrategy;
import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;

import java.util.HashMap;
import java.util.Objects;

public class ExtendedApplianceCard extends AbstractApplianceStrategy {
    private View finalResult; //The underlying card view the extended blind xml is attached to.
    private TextView statusTitle; //The textview that the current status is to be displayed on.
    private CardApplianceBinding binding; //The underlying card model the extended blind xml is attached to.
    private Appliance appliance; //The populate appliance model.
    protected View cardView; // The inflated view for controlling the blinds.
    private FlexboxLayout cardInsert;
    protected boolean isSceneCard;

    public ExtendedApplianceCard(boolean isSceneCard) {
        this.isSceneCard = isSceneCard;
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
        ViewGroup insertion = createExtendedCard(mainView);
        cardInsert = cardView.findViewById(R.id.appliance_expanded_card);
        String status = determineStatus();

        //Load the correct background and start the fade in transition
        loadBackgroundDrawable(status);

        ImageView image = cardView.findViewById(R.id.icon_appliance);
        determineApplianceType(image, appliance.type, status);

        View.OnTouchListener closeListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                resetTransition();

                insertion.postDelayed(() -> insertion.removeView(cardView), ApplianceController.fadeTime);

                //Remove the invisible background
                mainView.postDelayed(() -> mainView.removeView(background), ApplianceController.fadeTime);
            }

            return false;
        };

        //If anywhere besides the expanded blind card is touched or blind icon, remove the view
        cardInsert.setOnTouchListener(closeListener);
        image.setOnTouchListener(closeListener);
        background.setOnTouchListener(closeListener);
    }

    protected void setupButton(int index, String applianceValue, String applianceDescription) {
        FlexboxLayout buttonLayout = cardView.findViewById(R.id.button_layout);
        Button mb = new Button(new ContextThemeWrapper(buttonLayout.getContext(), R.style.BlindButton));

        mb.setText(applianceDescription);
        mb.setOnClickListener(v -> {
            if(this.isSceneCard) {
                ApplianceViewModel.activeSceneList.put(appliance.name, appliance);
            }

            String drawableConstant = Constants.ACTIVE;
            int drawableTransition = R.drawable.transition_appliance_grey_to_blue;
            if (index == 1) {
                drawableConstant = Constants.INACTIVE;
                drawableTransition = R.drawable.transition_appliance_blue_to_grey;
            }
            if (index == 2) {
                drawableConstant = Constants.STOPPED;
                drawableTransition = R.drawable.transition_appliance_none_to_navy;
            }

            interChangeDrawable(binding.getStatus().getValue(), drawableConstant);
            applianceNetworkCall(appliance, applianceValue);

            ApplianceViewModel.activeApplianceList.put(appliance.id, applianceValue);
            coordinateVisuals(drawableTransition, applianceDescription, drawableConstant);
        });

        buttonLayout.addView(mb);
    }

    /**
     * Create the expanded blind card with the generated parameters to appear directly over the
     * row the underlying appliance card was located in, return the ViewGroup that it is added to.
     */
    private ViewGroup createExtendedCard(ViewGroup root) {
        //Get the absolute position of the card within the window
        int[] out = new int[2];
        binding.getRoot().getLocationInWindow(out);

        //Inflate the expanded card view
        cardView = ApplianceFragment.fragmentInflater.inflate(R.layout.card_appliance_hdmi, root, false);
        cardView.setClickable(true);

        //Set the text of the selected card
        TextView name = cardView.findViewById(R.id.appliance_name);
        name.setText(appliance.name);

        //Set the layout parameters for where to add the view
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, Helpers.convertDpToPx(190));
        params.leftMargin = Helpers.convertDpToPx(120) + 200 + 70; //sub menu + regular menu + card spacing
        params.rightMargin = 45 + 125; //card spacing + container end margin
        params.topMargin = out[1];

        //Add the blind card to the background view
        ViewGroup insertion = root.findViewById(R.id.background_placeholder);
        insertion.addView(cardView, params);

        return insertion;
    }

    /**
     * Determine the description for the card.
     */
    private String determineStatus() {
        String statusContent;
        String status;

        if (Objects.equals(binding.getStatus().getValue(), Constants.ACTIVE)) {
            if (appliance.options != null) {
                statusContent = appliance.options.get(0).name;
            } else {
                statusContent = appliance.description.get(0);
            }
            status = Constants.ACTIVE;
        } else if (Objects.equals(binding.getStatus().getValue(), Constants.INACTIVE)) {
            if (appliance.options != null) {
                statusContent = appliance.options.get(1).name;
            } else {
                statusContent = appliance.description.get(1);
            }
            status = Constants.INACTIVE;
        } else {
            if (appliance.options != null) {
                statusContent = appliance.options.get(2).name;
            } else {
                statusContent = appliance.description.get(2);
            }
            status = Constants.STOPPED;
        }
        statusTitle = cardView.findViewById(R.id.appliance_status);
        statusTitle.setText(statusContent);

        return status;
    }

    private void loadBackgroundDrawable(String status) {
        switch(status) {
            case Constants.ACTIVE:
                ApplianceController.applianceTransition(cardInsert, R.drawable.transition_appliance_none_to_blue);
                break;
            case Constants.INACTIVE:
                ApplianceController.applianceTransition(cardInsert, R.drawable.transition_appliance_none_to_grey);
                break;
            case Constants.STOPPED:
                ApplianceController.applianceTransition(cardInsert, R.drawable.transition_appliance_none_to_navy);
                break;
        }
    }

    private void resetTransition() {
        switch(binding.getStatus().getValue()) {
            case Constants.ACTIVE:
                cardInsert.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_blue_to_none, null));
                break;
            case Constants.INACTIVE:
                cardInsert.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_grey_to_none, null));
                break;
            case Constants.STOPPED:
                cardInsert.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_appliance_navy_to_none, null));
        }

        TransitionDrawable transition = (TransitionDrawable) cardInsert.getBackground();
        transition.setCrossFadeEnabled(true);
        transition.startTransition(ApplianceController.fadeTime);
    }

    /**
     * Determine what icon an appliance needs depending on their type.
     */
    private void determineApplianceType(ImageView imageView, String applianceType, String status) {
        int iconResource;

        //Add to this in the future
        switch(applianceType) {
            case "blinds":
                if(status.equals(Constants.ACTIVE)) {
                    iconResource = R.drawable.icon_appliance_blind_open;
                } else if(status.equals(Constants.STOPPED)) {
                    iconResource = R.drawable.icon_appliance_blind_closed;
                } else {
                    iconResource = R.drawable.icon_appliance_blind_closed;
                }
                break;

            case "sources":
                iconResource = status.equals(Constants.ACTIVE) ? R.drawable.icon_appliance_source_2 :
                        R.drawable.icon_appliance_source_1;
                break;

            default:
                iconResource = R.drawable.icon_settings;
                break;
        }

        imageView.setBackgroundResource(iconResource);
    }

    //Switch between the different transitions then set the overall fade as the original.
    protected void interChangeDrawable(String oldStatus, String newStatus) {
        if(oldStatus.equals(newStatus)) {
            return;
        }

        //Grey to Blue - inactive, active
        if(oldStatus.equals(Constants.INACTIVE) && newStatus.equals(Constants.ACTIVE)){
            ApplianceController.applianceTransition(cardInsert, R.drawable.transition_appliance_grey_to_blue);
        }

        //Grey to Navy inactive, stopped
        else if(oldStatus.equals(Constants.INACTIVE) && newStatus.equals(Constants.STOPPED)){
            ApplianceController.applianceTransition(cardInsert, R.drawable.transition_appliance_grey_to_navy);
        }

        //Blue to Grey active, inactive
        else if(oldStatus.equals(Constants.ACTIVE) && newStatus.equals(Constants.INACTIVE)){
            ApplianceController.applianceTransition(cardInsert, R.drawable.transition_appliance_blue_to_grey);
        }

        //Blue to Navy, active, stopped
        else if(oldStatus.equals(Constants.ACTIVE) && newStatus.equals(Constants.STOPPED)){
            ApplianceController.applianceTransition(cardInsert, R.drawable.transition_appliance_blue_to_navy);
        }

        //Navy to Grey stopped, inactive
        else if(oldStatus.equals(Constants.STOPPED) && newStatus.equals(Constants.INACTIVE)){
            ApplianceController.applianceTransition(cardInsert, R.drawable.transition_appliance_navy_to_grey);
        }

        //Navy to Blue stopped, active
        else if(oldStatus.equals(Constants.STOPPED) && newStatus.equals(Constants.ACTIVE)){
            ApplianceController.applianceTransition(cardInsert, R.drawable.transition_appliance_navy_to_blue);
        }
    }

    /**
     * Change the background depending on what the currently active status is.
     * @param transitionDrawable The transition that is required to happen.
     * @param current The text to be displayed to the user.
     * @param activeStatus The active status (active, inactive or stopped).
     */
    protected void coordinateVisuals(int transitionDrawable, String current, String activeStatus) {
        ApplianceController.applianceTransition(finalResult, transitionDrawable);
        statusTitle.setText(current);
        binding.setStatus(new MutableLiveData<>(activeStatus));
    }

    /**
     * Make a custom network call for the blind appliance. Value needs to be supplied as it can be
     * one of three values, rather than the standard 2.
     * @param value An integer representing whether to open(255), stop(5) or close(0) a blind.
     */
    protected void applianceNetworkCall(Appliance appliance, String value) {
        //Action : [cbus unit : group address : id address : scene value] : [type : room : id scene]
        NetworkService.sendMessage("NUC",
                "Automation",
                "Set" + ":"                         //[0] Action
                        + appliance.id + ":"
                        + value + ":"                           //[4] New value for address
                        + NetworkService.getIPAddress());       //[8] The IP address of the tablet

        HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
            put("appliance_type", appliance.type);
            put("appliance_room", appliance.room);
            put("appliance_new_value", appliance.value);
            put("appliance_action_type", "radio");
        }};
        FirebaseManager.logAnalyticEvent("appliance_value_changed", analyticsAttributes);
    }
}

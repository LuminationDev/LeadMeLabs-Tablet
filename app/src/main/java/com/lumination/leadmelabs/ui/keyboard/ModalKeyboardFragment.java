package com.lumination.leadmelabs.ui.keyboard;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentModalKeyboardBinding;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

import org.json.JSONException;
import org.json.JSONObject;

public class ModalKeyboardFragment extends DialogFragment {
    public FragmentModalKeyboardBinding binding;
    public static FragmentManager childManager;
    public static KeyboardViewModel mViewModel;

    private float sensitivity = 1.0f;
    private float lastX, lastY;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modal_keyboard, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setKeyboard(mViewModel);

        //Setup the keyboard components
        setupTrackpad(view);
        setupMouseButtons(view);
        setupSensitivitySlider(view);

        // Close the modal
        FlexboxLayout closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dismiss());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create a Dialog object with custom window attributes
        Dialog dialog = new Dialog(requireContext(), R.style.CurvedDialogTheme);

        // Set window dimensions or any other window attributes here
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.x = getResources().getDimensionPixelSize(R.dimen.dialog_x_offset); // Adjust the X position
            window.setAttributes(params);
        }

        // Return the created dialog
        return dialog;
    }

    //region Mouse Controls
    private void setupSensitivitySlider(View view) {
        SeekBar sensitivitySeekBar = view.findViewById(R.id.sensitivity_seekbar);
        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivity = progress / 100.0f; // Convert to a multiplier (0.0 to 2.0)
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupTrackpad(View view) {
        View trackpadView = view.findViewById(R.id.trackpad_view);
        trackpadView.setOnTouchListener(new View.OnTouchListener() {
            private static final int CLICK_THRESHOLD = 5; // Define a threshold for detecting clicks
            private static final long MOVE_COOLDOWN_MS = 50;
            private float startX;
            private float startY;

            private final Handler handler = new Handler(Looper.getMainLooper());
            private boolean canSendMove = true;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = x;
                        startY = y;
                        lastX = x;
                        lastY = y;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (canSendMove) {
                            float deltaX = (x - lastX) * sensitivity;
                            float deltaY = (y - lastY) * sensitivity;
                            if (deltaY != 0 && deltaX != 0) {
                                sendMouseMove(deltaX, deltaY);
                                canSendMove = false;

                                // Set a cooldown period before the next sendMouseMove can be called
                                handler.postDelayed(() -> canSendMove = true, MOVE_COOLDOWN_MS);
                            }
                            lastX = x;
                            lastY = y;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        float distanceX = Math.abs(x - startX);
                        float distanceY = Math.abs(y - startY);

                        if (distanceX < CLICK_THRESHOLD && distanceY < CLICK_THRESHOLD) {
                            sendMouseClick("Left");
                        }
                        break;
                }
                return true;
            }
        });
    }

    private void setupMouseButtons(View view) {
        FlexboxLayout leftClick = view.findViewById(R.id.left_click_button);
        leftClick.setOnClickListener(v -> sendMouseClick("Left"));
        FlexboxLayout rightClick = view.findViewById(R.id.right_click_button);
        rightClick.setOnClickListener(v -> sendMouseClick("Right"));
    }

    private void sendMouseClick(String button) {
        JSONObject message = new JSONObject();
        try {
            JSONObject details = new JSONObject();
            details.put("Button", button);

            message.put("Details", details);
            message.put("Action", "Click");
            message.put("Component", "Mouse");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        int id = getStationId();
        if (id != -1) {
            NetworkService.sendMessage("Station," + id, "Keyboard", message.toString());
        }
    }

    private void sendMouseMove(float deltaX, float deltaY) {
        JSONObject message = new JSONObject();
        try {
            JSONObject details = new JSONObject();
            details.put("MoveX", deltaX);
            details.put("MoveY", deltaY);

            message.put("Details", details);
            message.put("Action", "Move");
            message.put("Component", "Mouse");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        int id = getStationId();
        if (id != -1) {
            NetworkService.sendMessage("Station," + id, "Keyboard", message.toString());
        }
    }
    //endregion

    /**
     * Get the Id of the currently selected station, otherwise return -1.
     * @return An int of the current station or -1
     */
    private int getStationId() {
        if (StationsFragment.mViewModel == null) return -1;
        if (StationsFragment.mViewModel.getSelectedStation() == null) return -1;
        if (StationsFragment.mViewModel.getSelectedStation().getValue() == null) return -1;

        return StationsFragment.mViewModel.getSelectedStation().getValue().getId();
    }
}

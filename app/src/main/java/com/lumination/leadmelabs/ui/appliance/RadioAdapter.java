package com.lumination.leadmelabs.ui.appliance;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.RadioApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;

import java.util.ArrayList;

/**
 * Use this adapter for scripts in the future. Acts as a singleton when individual rooms are
 * displayed. When 'All' rooms are chosen it acts as a regular class access through references in
 * the parent adapter class.
 */
public class RadioAdapter extends BaseAdapter {
    private final ApplianceController applianceController = new ApplianceController();

    public ArrayList<Appliance> applianceList = new ArrayList<>();


    public class RadioApplianceViewHolder extends BaseAdapter.BaseViewHolder {
        private final RadioApplianceBinding binding;

        public RadioApplianceViewHolder(@NonNull RadioApplianceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Appliance appliance) {
            binding.setAppliance(appliance);
            RadioGroup radioGroup = binding.getRoot().findViewById(R.id.radio_group);
            int size = appliance.options.size();
            for (int i = 0; i < size; i++) {
                Appliance.Option option = appliance.options.get(i);
                RadioButton radioButton = new RadioButton(radioGroup.getContext());
                radioButton.setId(Integer.parseInt(option.id));
                radioButton.setText(option.name);
                radioButton.setTextColor(ContextCompat.getColor(MainActivity.getInstance(), R.color.white));
                radioButton.setTextSize(20);
                radioButton.setButtonTintList(new ColorStateList(new int[][]
                        {
                                new int[]{-android.R.attr.state_enabled}, // Disabled
                                new int[]{android.R.attr.state_enabled}   // Enabled
                        },
                        new int[]
                                {
                                        Color.WHITE, // disabled
                                        Color.WHITE   // enabled
                                }));
                RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 15);
                radioButton.setLayoutParams(params);
                radioButton.setPadding(0, 10, 30, 10);

                if (appliance.value != null && appliance.value.length() > 0 && appliance.value == option.id) {
                    radioButton.setChecked(true);
                    radioButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.on_text,0);
                    radioButton.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_radio_blue_to_grey, null));
                } else {
                    radioButton.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_radio_grey_to_blue, null));
                    radioButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.off_text,0);
                }

                radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        if (checked) {
                            radioButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.on_text,0);
                            compoundButton.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_radio_blue_to_grey, null));
                            applianceController.triggerRadioAppliance(appliance, compoundButton.getId() + "");
                        } else {
                            radioButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.off_text,0);
                            compoundButton.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.transition_radio_grey_to_blue, null));
                        }
                    }
                });

                radioGroup.addView(radioButton);
            }
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
    public RadioApplianceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        RadioApplianceBinding binding = RadioApplianceBinding.inflate(layoutInflater, parent, false);
        return new RadioApplianceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        RadioApplianceViewHolder RadioApplianceViewHolder = (RadioApplianceViewHolder) holder;
        Appliance appliance = getItem(position);
        RadioApplianceViewHolder.bind(appliance);
    }
}

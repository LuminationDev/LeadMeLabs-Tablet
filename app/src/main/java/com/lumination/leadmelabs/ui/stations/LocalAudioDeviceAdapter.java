package com.lumination.leadmelabs.ui.stations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.LocalAudioDevice;

import java.util.List;

public class LocalAudioDeviceAdapter extends ArrayAdapter<LocalAudioDevice> {
    private final List<LocalAudioDevice> devices;

    public LocalAudioDeviceAdapter(Context context, List<LocalAudioDevice> devices) {
        super(context, android.R.layout.simple_spinner_item, devices);
        this.devices = devices;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    public List<LocalAudioDevice> getAudioDevices() {
        return this.devices;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_layout_spinner, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.spinner_item_text);
        LocalAudioDevice device = getItem(position);

        if (device != null) {
            textView.setText(device.getName());
        }

        return convertView;
    }
}

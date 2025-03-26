package com.lumination.leadmelabs.ui.stations.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.VrOption;

import java.util.List;

public class VrVideoOptionAdapter extends ArrayAdapter<VrOption> {
    private List<VrOption> options;

    public VrVideoOptionAdapter(Context context, List<VrOption> options) {
        super(context, android.R.layout.simple_spinner_item, options);
        this.options = options;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    public void setOptions(List<VrOption> options) {
        this.options = options;
        MainActivity.runOnUI(this::notifyDataSetChanged);
    }

    public List<VrOption> getVrOptions() {
        return this.options;
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
        VrOption option = getItem(position);

        if (option != null) {
            textView.setText(option.getName());
        }

        return convertView;
    }
}

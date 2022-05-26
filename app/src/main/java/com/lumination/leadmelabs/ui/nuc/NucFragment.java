package com.lumination.leadmelabs.ui.nuc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.services.NetworkService;

public class NucFragment extends Fragment {

    public static NucViewModel mViewModel;
    private View view;
    private AlertDialog nucDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_nuc, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        buildSetNucDialog();

        Button set_button = view.findViewById(R.id.set_nuc_address);
        set_button.setOnClickListener(v -> {
            nucDialog.show();
        });

        Button how_to_button = view.findViewById(R.id.how_to_button);
        how_to_button.setOnClickListener(v -> {
            // todo - open the guide
        });
    }

    private void buildSetNucDialog() {
        View view = View.inflate(getContext(), R.layout.dialog_set_nuc, null);
        mViewModel.getNuc().observe(getViewLifecycleOwner(), nucAddress -> {
            TextView textView = view.findViewById(R.id.nuc_address);
            textView.setText(nucAddress);
        });
        Button scan_button = view.findViewById(R.id.scan_nuc_address);
        scan_button.setOnClickListener(v -> {
            NetworkService.broadcast("Android");
        });
        EditText newAddress = view.findViewById(R.id.nuc_address_input);
        Button setAddress = view.findViewById(R.id.set_nuc_button);
        setAddress.setOnClickListener(v -> {
            mViewModel.setNucAddress(newAddress.getText().toString());
            nucDialog.dismiss();
        });
        nucDialog = new AlertDialog.Builder(getContext()).setView(view).create();
    }
}
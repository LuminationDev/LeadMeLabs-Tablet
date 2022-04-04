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
import androidx.lifecycle.ViewModelProvider;

import com.lumination.leadmelabs.R;

public class NucFragment extends Fragment {

    private NucViewModel mViewModel;
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

        mViewModel = new ViewModelProvider(requireActivity()).get(NucViewModel.class);
        mViewModel.getNuc().observe(getViewLifecycleOwner(), nucAddress -> {
            TextView textView = view.findViewById(R.id.nuc_address);
            textView.setText(nucAddress);
        });

        buildSetNucDialog();

        Button button = view.findViewById(R.id.set_nuc_address);
        button.setOnClickListener(v -> {
            nucDialog.show();
        });
    }

    private void buildSetNucDialog() {
        View view = View.inflate(getContext(), R.layout.dialog_set_nuc, null);
        EditText newAddress = view.findViewById(R.id.nuc_address_input);
        Button setAddress = view.findViewById(R.id.set_nuc_button);
        setAddress.setOnClickListener(v -> {
            this.mViewModel.setNucAddress(newAddress.getText().toString());
            nucDialog.dismiss();
        });
        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            nucDialog.dismiss();
        });
        nucDialog = new AlertDialog.Builder(getContext()).setView(view).create();
    }
}
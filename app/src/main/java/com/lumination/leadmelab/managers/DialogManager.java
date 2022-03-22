package com.lumination.leadmelab.managers;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.lumination.leadmelab.R;

/**
 * A class to handle all dialogs that are needed while the application is running. Sets up the
 * required alerts and displays them when called.
 */
public class DialogManager {
    private final String TAG = "DialogManager";
    private final StationManager manager;
    private final Context context;

    private AlertDialog clientDialog, NUCDialog;
    private View newClientDialogView, newNUCDialogView;

    public DialogManager(StationManager manager, Context context) {
        this.manager = manager;
        this.context = context;

        setupNUCDialog();
        setupNewClientDialog();
    }

    /**
     * Prepare the dialog for adding the NUC's ipaddress.
     */
    private void setupNUCDialog() {
        newNUCDialogView = View.inflate(context, R.layout.a__set_nuc, null);
        EditText newNUCAddress = newNUCDialogView.findViewById(R.id.nuc_ipaddress_input);

        Button addNUCBtn = newNUCDialogView.findViewById(R.id.manual_set_nuc);
        addNUCBtn.setOnClickListener(v -> {
            StationManager.NUCIPAddress = newNUCAddress.getText().toString();

            NUCDialog.dismiss();
        });

        Button backBtn = newNUCDialogView.findViewById(R.id.manual_back);
        backBtn.setOnClickListener(v -> NUCDialog.dismiss());

        NUCDialog = new AlertDialog.Builder(context)
                .setView(newNUCDialogView)
                .create();

        NUCDialog.setCancelable(false);

        NUCDialog.setOnDismissListener(dialog -> Log.d(TAG, "Dismissed"));
    }

    /**
     * Show the NUC dialog.
     */
    public void showNUCDialog() {
        if(NUCDialog == null) {
            setupNewClientDialog();
        }

        NUCDialog.show();

        EditText newNUCAddress = newNUCDialogView.findViewById(R.id.nuc_ipaddress_input);
        newNUCAddress.requestFocus();
    }

    /**
     * Prepare the dialog view for adding new clients to the Clients hashmap. Has two edit texts, one
     * for adding a computer nickname and another for the computers address.
     */
    private void setupNewClientDialog() {
        newClientDialogView = View.inflate(context, R.layout.a__new_station, null);
        EditText newComputerName = newClientDialogView.findViewById(R.id.computer_name_input);
        EditText newComputerAddress = newClientDialogView.findViewById(R.id.computer_ipaddress_input);

        Button addClientBtn = newClientDialogView.findViewById(R.id.manual_create_station);
        addClientBtn.setOnClickListener(v -> {
            String name = newComputerName.getText().toString();
            String IpAddress = newComputerAddress.getText().toString();

            addClient(name, IpAddress);

            clientDialog.dismiss();
        });

        Button backBtn = newClientDialogView.findViewById(R.id.manual_back);
        backBtn.setOnClickListener(v -> clientDialog.dismiss());

        clientDialog = new AlertDialog.Builder(context)
                .setView(newClientDialogView)
                .create();

        clientDialog.setCancelable(false);

        clientDialog.setOnDismissListener(dialog -> Log.d(TAG, "Dismissed"));
    }

    /**
     * Display a dialog for added a new computer to the grid layout with a name and an Ip Address.
     */
    public void showNewClientDialog() {
        if(clientDialog == null) {
            setupNewClientDialog();
        }

        clientDialog.show();

        EditText newComputerName = newClientDialogView.findViewById(R.id.computer_name_input);
        newComputerName.requestFocus();
    }

    /**
     * Make sure the inputs are not null and validate the contents. If all is correct then call the
     * static createNewClient function from the main activity to add the new client to the static
     * Clients hashmap.
     * @param name A String representing the nickname for a new client computer.
     * @param number A String representing the number of the new client computer.
     */
    private void addClient(String name, String number) {
        Log.d(TAG, "Name: " + name);
        Log.d(TAG, "Number: " + number);

        manager.createNewClient(name, number);
    }
}

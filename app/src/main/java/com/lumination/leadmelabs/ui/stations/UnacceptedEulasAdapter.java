package com.lumination.leadmelabs.ui.stations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;

import java.util.ArrayList;

public class UnacceptedEulasAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;

    public static StationsViewModel mViewModel;
    public ArrayList<String> unacceptedEulas;

    public UnacceptedEulasAdapter(Context context, ArrayList<String> unacceptedEulas) {
        this.mInflater = LayoutInflater.from(context);
        this.unacceptedEulas = unacceptedEulas;
    }
    @Override
    public int getCount()  {
        return unacceptedEulas != null ? unacceptedEulas.size() : 0;
    }

    @Override
    public String getItem(int position)  {
        return unacceptedEulas.get(position); // todo split
    }

    @Override
    public long getItemId(int i) {
        return 0; // todo split
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = mInflater.inflate(R.layout.item_unaccepted_eula, parent, false);
        }

        String[] text = getItem(position).split(":", 4);

        TextView view1 = view.findViewById(R.id.eula_experience_name);
        view1.setText(text[3]);

        View finalView = view;
        view.setOnClickListener(l -> {
            DialogManager.buildWebViewDialog(finalView.getContext(), "https://store.steampowered.com/eula/" + text[1]);
        });


        return view;
    }
}

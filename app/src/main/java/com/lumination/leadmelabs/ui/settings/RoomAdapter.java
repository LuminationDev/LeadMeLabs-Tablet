package com.lumination.leadmelabs.ui.settings;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.ViewHolder> {
    private final ArrayList<String> mData;
    private final HashSet<String> mSelectedRooms;
    private final TextView mPreview;

    public RoomAdapter(TextView preview, HashSet<String> data) {
        mData = new ArrayList<>(data);
        mSelectedRooms = new HashSet<>();
        mPreview = preview;

        //Do not want to collect the 'All' rooms
        mData.remove("All");

        Collections.sort(mData);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout_room_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(mData.get(position));
        String hashSetString = mPreview.getText().toString();

        HashSet<String> entries = new HashSet<>(Arrays.asList(hashSetString
                    .replace("[", "")
                    .replace("]", "")
                    .replace(", ", ",")
                    .split(",")));

        if(entries.contains(mData.get(position))) {
            mSelectedRooms.add(mData.get(position));
        }

        if (mSelectedRooms.contains(mData.get(position))) {
            holder.textView.setBackgroundColor(ContextCompat.getColor(MainActivity.getInstance(), R.color.blue));
        } else {
            holder.textView.setBackgroundColor(ContextCompat.getColor(MainActivity.getInstance(), R.color.grey_card));
        }

        holder.textView.setOnClickListener(v -> {
            //Show the animation of the button before changing
            new Handler().postDelayed(this::notifyDataSetChanged, 200); // Delay of 200 milliseconds

            if(mSelectedRooms.contains(mData.get(position))) {
                mSelectedRooms.remove(mData.get(position));

                //Show the animation of the button before changing
                new Handler().postDelayed(this::notifyDataSetChanged, 200); // Delay of 200 milliseconds

                mPreview.setText(mSelectedRooms.toString());
            } else {
                mSelectedRooms.add(mData.get(position));
                mPreview.setText(mSelectedRooms.toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.room_button);
        }
    }
}

package com.lumination.leadmelabs.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.LocalFile;

import java.util.ArrayList;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private ArrayList<LocalFile> dataList;
    private final LayoutInflater inflater;
    TextView selectedItemTextView;

    private int selectedPosition = RecyclerView.NO_POSITION;
    private String selectedFileName;
    private String selectedFilePath;

    public FileAdapter(Context context, TextView selectedItemTextView, ArrayList<LocalFile> dataList) {
        this.dataList = dataList;
        this.selectedItemTextView = selectedItemTextView;
        inflater = LayoutInflater.from(context);
    }

    public String getSelectedFileName() {
        return this.selectedFileName;
    }

    public String getSelectedFilePath() {
        return this.selectedFilePath;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_layout_local_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textViewItem.setText(dataList.get(position).getName());
        holder.itemLayout.setSelected(position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewItem;
        LinearLayout itemLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewItem = itemView.findViewById(R.id.textViewItem);
            itemLayout = itemView.findViewById(R.id.itemLayout);

            itemLayout.setOnClickListener(v -> {
                // Handle item click here, e.g., show a toast or start a new activity
                int position = getAdapterPosition();

                if (position != RecyclerView.NO_POSITION) {
                    // Toggle selection
                    if (position == selectedPosition) {
                        selectedFileName = null;
                        selectedFilePath = null;
                        selectedPosition = RecyclerView.NO_POSITION;
                    } else {
                        selectedFileName = dataList.get(position).getName();
                        selectedFilePath = dataList.get(position).getPath();
                        selectedPosition = position;
                    }

                    notifyDataSetChanged(); // Notify adapter that dataset has changed

                    // Update the TextView with the selected item text
                    selectedItemTextView.setText(String.format("Selected Item: %s", selectedFileName == null ? "None" : selectedFileName));
                }
            });
        }
    }

    /**
     * Update the adapters file list and run the notifyDataSetChanged to update the UI.
     */
    public void Update(ArrayList<LocalFile> dataList) {
        this.dataList = dataList;
        selectedPosition = RecyclerView.NO_POSITION;
        MainActivity.runOnUI(() -> notifyDataSetChanged());
    }
}

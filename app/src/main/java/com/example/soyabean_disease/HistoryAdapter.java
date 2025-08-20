package com.example.soyabean_disease;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final Context context;
    private List<PredictionEntry> data;

    public HistoryAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<PredictionEntry> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PredictionEntry entry = data.get(position);

        Glide.with(context)
                .load(entry.imagePath)
                .placeholder(R.drawable.placeholder_leaf)
                .centerCrop()
                .into(holder.historyImage);

        holder.historyResult.setText(context.getString(R.string.result) + "\n" + entry.result);

        String formattedTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(new Date(entry.timestamp));

        holder.historyConfidence.setText(String.format(Locale.US,
                context.getString(R.string.confidence_and_time),
                entry.confidence * 100, formattedTime));
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView historyImage;
        TextView historyResult;
        TextView historyConfidence;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            historyImage = itemView.findViewById(R.id.historyImage);
            historyResult = itemView.findViewById(R.id.historyResult);
            historyConfidence = itemView.findViewById(R.id.historyConfidence);
        }
    }
}

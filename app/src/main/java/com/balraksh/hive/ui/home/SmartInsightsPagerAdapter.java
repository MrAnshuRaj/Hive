package com.balraksh.hive.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import com.balraksh.hive.R;
import com.balraksh.hive.data.SmartInsightItem;

public class SmartInsightsPagerAdapter extends RecyclerView.Adapter<SmartInsightsPagerAdapter.InsightViewHolder> {

    public interface Callback {
        void onInsightAction(@NonNull SmartInsightItem item);
    }

    private final Callback callback;
    private final List<SmartInsightItem> items = new ArrayList<>();

    public SmartInsightsPagerAdapter(@NonNull Callback callback) {
        this.callback = callback;
    }

    public void setItems(@NonNull List<SmartInsightItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    public SmartInsightItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public InsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_smart_insight, parent, false);
        return new InsightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InsightViewHolder holder, int position) {
        SmartInsightItem item = items.get(position);
        holder.iconView.setImageResource(item.getIconResId());
        holder.titleView.setText(item.getTitle());
        holder.subtitleView.setText(item.getSubtitle());
        holder.actionButton.setText(item.getCtaLabel());
        holder.itemView.setOnClickListener(v -> callback.onInsightAction(item));
        holder.actionButton.setOnClickListener(v -> callback.onInsightAction(item));
    }

    static final class InsightViewHolder extends RecyclerView.ViewHolder {
        final ImageView iconView;
        final TextView titleView;
        final TextView subtitleView;
        final MaterialButton actionButton;

        InsightViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.imageInsightIcon);
            titleView = itemView.findViewById(R.id.textInsightTitle);
            subtitleView = itemView.findViewById(R.id.textInsightSubtitle);
            actionButton = itemView.findViewById(R.id.buttonInsightAction);
        }
    }
}

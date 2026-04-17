package com.balraksh.hive.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.balraksh.hive.R;
import com.balraksh.hive.data.BucketOption;
import com.balraksh.hive.utils.FormatUtils;
import com.google.android.material.card.MaterialCardView;

public class FolderOptionAdapter extends RecyclerView.Adapter<FolderOptionAdapter.FolderViewHolder> {

    public interface Listener {
        void onSelectionChanged(@NonNull List<BucketOption> selectedBuckets);
    }

    private final List<BucketOption> items = new ArrayList<>();
    private final Set<Long> selectedBucketIds = new LinkedHashSet<>();
    private final Listener listener;

    public FolderOptionAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<BucketOption> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_option, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        BucketOption item = items.get(position);
        boolean selected = selectedBucketIds.contains(item.getBucketId());
        int titleColor = holder.itemView.getContext().getColor(
                selected ? R.color.color_scan_gold : R.color.color_scan_text_primary
        );
        holder.title.setText(item.getBucketName());
        holder.title.setTextColor(titleColor);
        holder.subtitle.setText(formatMeta(item));
        holder.icon.setImageResource(resolveIcon(item));
        holder.icon.setColorFilter(titleColor);
        holder.iconTile.setBackgroundResource(selected
                ? R.drawable.bg_folder_option_icon_tile_selected
                : R.drawable.bg_folder_option_icon_tile);
        holder.indicator.setBackgroundResource(selected
                ? R.drawable.bg_folder_option_indicator_selected
                : R.drawable.bg_folder_option_indicator);
        holder.card.setStrokeColor(holder.itemView.getContext().getColor(
                selected ? R.color.color_scan_gold : R.color.color_scan_border
        ));
        holder.card.setCardBackgroundColor(holder.itemView.getContext().getColor(
                selected ? R.color.color_scan_surface_active : R.color.color_scan_surface_alt
        ));
        holder.itemView.setOnClickListener(v -> {
            if (selectedBucketIds.contains(item.getBucketId())) {
                selectedBucketIds.remove(item.getBucketId());
            } else {
                selectedBucketIds.add(item.getBucketId());
            }
            notifyItemChanged(position);
            listener.onSelectionChanged(getSelectedBuckets());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private List<BucketOption> getSelectedBuckets() {
        List<BucketOption> selectedBuckets = new ArrayList<>();
        for (BucketOption item : items) {
            if (selectedBucketIds.contains(item.getBucketId())) {
                selectedBuckets.add(item);
            }
        }
        return selectedBuckets;
    }

    private String formatMeta(@NonNull BucketOption item) {
        String formattedCount = NumberFormat.getIntegerInstance(Locale.getDefault()).format(item.getItemCount());
        return formattedCount + " " + (item.getItemCount() == 1 ? "item" : "items")
                + " • " + FormatUtils.formatStorage(item.getTotalBytes());
    }

    private int resolveIcon(@NonNull BucketOption item) {
        String name = item.getBucketName() == null ? "" : item.getBucketName().toLowerCase(Locale.US);
        if (name.contains("whatsapp")) {
            return R.drawable.ic_whatsapp;
        }
        if (name.contains("snapchat")) {
            return R.drawable.ic_snapchat;
        }
        if (name.contains("download")) {
            return R.drawable.ic_download;
        }
        if (name.contains("camera")) {
            return R.drawable.ic_photo;
        }
        if (name.contains("instagram")) {
            return R.drawable.ic_instagram;
        }
        return R.drawable.ic_folder;
    }

    static final class FolderViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final View iconTile;
        private final ImageView icon;
        private final View indicator;
        private final TextView title;
        private final TextView subtitle;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardFolder);
            iconTile = itemView.findViewById(R.id.layoutFolderIconTile);
            icon = itemView.findViewById(R.id.imageFolderIcon);
            indicator = itemView.findViewById(R.id.viewSelectionIndicator);
            title = itemView.findViewById(R.id.textFolderName);
            subtitle = itemView.findViewById(R.id.textFolderMeta);
        }
    }
}

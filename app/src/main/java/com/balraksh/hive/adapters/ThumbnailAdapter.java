package com.balraksh.hive.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import com.balraksh.hive.R;
import com.balraksh.hive.data.MediaGroup;
import com.balraksh.hive.data.MediaImageItem;
import com.balraksh.hive.repository.ScanSessionStore;
import com.balraksh.hive.utils.FormatUtils;

public class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.ThumbnailViewHolder> {

    public interface OnThumbnailClickListener {
        void onThumbnailClick(MediaImageItem item, boolean willSelect);
    }

    private final MediaGroup group;
    private final OnThumbnailClickListener listener;
    private final List<MediaImageItem> items = new ArrayList<>();

    public ThumbnailAdapter(MediaGroup group, OnThumbnailClickListener listener) {
        this.group = group;
        this.listener = listener;
        this.items.addAll(group.getItems());
    }

    @NonNull
    @Override
    public ThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thumbnail, parent, false);
        return new ThumbnailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position) {
        MediaImageItem item = items.get(position);
        boolean isBest = item.getId() == group.getBestItemId();
        boolean isSelected = ScanSessionStore.getInstance().isSelected(item.getId());

        Glide.with(holder.imageView)
                .load(item.getUri())
                .centerCrop()
                .into(holder.imageView);

        holder.card.setStrokeColor(holder.itemView.getContext().getColor(
                isSelected ? R.color.color_scan_gold : android.R.color.transparent
        ));
        holder.selectionIcon.setBackgroundResource(
                isSelected ? R.drawable.bg_scan_results_indicator_selected : R.drawable.bg_scan_results_indicator
        );
        holder.selectionIcon.setImageResource(isSelected ? R.drawable.ic_check_small : 0);
        holder.selectionIcon.setColorFilter(holder.itemView.getContext().getColor(R.color.white));
        holder.selectionIcon.setAlpha(isBest ? 0.95f : 1f);
        holder.selectedOverlay.setAlpha(isSelected ? 1f : 0f);
        holder.imageView.setAlpha(isSelected ? 0.94f : 1f);
        holder.sizeText.setText(FormatUtils.formatStorage(item.getSizeBytes()));
        holder.itemView.setOnClickListener(v -> {
            if (!isBest && listener != null) {
                v.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(90L)
                        .withEndAction(() -> v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120L)
                                .start())
                        .start();
                listener.onThumbnailClick(item, !ScanSessionStore.getInstance().isSelected(item.getId()));
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ThumbnailViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final ImageView imageView;
        private final TextView sizeText;
        private final ImageView selectionIcon;
        private final View selectedOverlay;

        ThumbnailViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardThumbnail);
            imageView = itemView.findViewById(R.id.imageThumbnail);
            sizeText = itemView.findViewById(R.id.textSizeBadge);
            selectionIcon = itemView.findViewById(R.id.imageSelection);
            selectedOverlay = itemView.findViewById(R.id.viewSelectedOverlay);
        }
    }
}


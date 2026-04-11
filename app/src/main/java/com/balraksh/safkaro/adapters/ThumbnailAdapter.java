package com.balraksh.safkaro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.data.MediaGroup;
import com.balraksh.safkaro.data.MediaImageItem;
import com.balraksh.safkaro.repository.ScanSessionStore;
import com.balraksh.safkaro.utils.FormatUtils;

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

        holder.bestBadge.setVisibility(isBest ? View.VISIBLE : View.GONE);
        holder.selectionIcon.setImageResource(isSelected ? R.drawable.ic_check_filled : R.drawable.ic_circle_outline);
        holder.selectionIcon.setVisibility(isBest ? View.GONE : View.VISIBLE);
        holder.imageView.setAlpha(isBest || isSelected ? 1f : 0.72f);
        holder.sizeText.setText(FormatUtils.formatStorage(item.getSizeBytes()));
        holder.itemView.setOnClickListener(v -> {
            if (!isBest && listener != null) {
                listener.onThumbnailClick(item, !ScanSessionStore.getInstance().isSelected(item.getId()));
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ThumbnailViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final TextView bestBadge;
        private final TextView sizeText;
        private final ImageView selectionIcon;

        ThumbnailViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageThumbnail);
            bestBadge = itemView.findViewById(R.id.textBestBadge);
            sizeText = itemView.findViewById(R.id.textSizeBadge);
            selectionIcon = itemView.findViewById(R.id.imageSelection);
        }
    }
}

package com.balraksh.hive.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;

import com.balraksh.hive.R;
import com.balraksh.hive.data.VideoItem;
import com.balraksh.hive.utils.FormatUtils;

import java.util.HashSet;
import java.util.Set;

public class VideoListAdapter extends ListAdapter<VideoItem, VideoListAdapter.VideoViewHolder> {

    public interface Listener {
        void onVideoClicked(@NonNull VideoItem item);

        void onVideoLongPressed(@NonNull VideoItem item, @NonNull View anchor);

        void onDeleteClicked(@NonNull VideoItem item);
    }

    private final Listener listener;
    private final Set<String> selectedUris = new HashSet<>();

    public VideoListAdapter(@NonNull Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setSelectedUris(@NonNull Set<String> uris) {
        selectedUris.clear();
        selectedUris.addAll(uris);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_select, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem item = getItem(position);
        boolean isSelected = selectedUris.contains(item.getUriString());
        holder.bind(item, isSelected, listener);
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final ImageView thumbnailView;
        private final ImageView selectedView;
        private final ImageView deleteView;
        private final TextView durationView;
        private final TextView nameView;
        private final TextView sizeView;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardVideo);
            thumbnailView = itemView.findViewById(R.id.imageThumbnail);
            selectedView = itemView.findViewById(R.id.imageSelected);
            deleteView = itemView.findViewById(R.id.buttonDelete);
            durationView = itemView.findViewById(R.id.textDuration);
            nameView = itemView.findViewById(R.id.textName);
            sizeView = itemView.findViewById(R.id.textSize);
        }

        void bind(@NonNull VideoItem item, boolean isSelected, @NonNull Listener listener) {
            nameView.setText(item.getDisplayName());
            sizeView.setText(FormatUtils.formatStorage(item.getSizeBytes()));
            durationView.setText(FormatUtils.formatDuration(item.getDurationMs()));
            selectedView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            int strokeColor = ContextCompat.getColor(
                    itemView.getContext(),
                    isSelected ? R.color.color_primary : R.color.color_border
            );
            cardView.setStrokeColor(strokeColor);
            cardView.setStrokeWidth((int) itemView.getResources().getDisplayMetrics().density * (isSelected ? 2 : 1));
            Glide.with(thumbnailView)
                    .load(item.getUri())
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(thumbnailView);
            itemView.setOnClickListener(v -> listener.onVideoClicked(item));
            itemView.setOnLongClickListener(v -> {
                listener.onVideoLongPressed(item, v);
                return true;
            });
            deleteView.setOnClickListener(v -> listener.onDeleteClicked(item));
        }
    }

    private static final DiffUtil.ItemCallback<VideoItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<VideoItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull VideoItem oldItem, @NonNull VideoItem newItem) {
                    return oldItem.getUriString().equals(newItem.getUriString());
                }

                @Override
                public boolean areContentsTheSame(@NonNull VideoItem oldItem, @NonNull VideoItem newItem) {
                    return oldItem.equals(newItem)
                            && oldItem.getDisplayName().equals(newItem.getDisplayName())
                            && oldItem.getDurationMs() == newItem.getDurationMs()
                            && oldItem.getSizeBytes() == newItem.getSizeBytes();
                }
            };
}


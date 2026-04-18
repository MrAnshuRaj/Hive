package com.balraksh.hive.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Paint;

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
import com.balraksh.hive.video.VideoCompressionEstimateUtils;
import com.balraksh.hive.video.VideoCompressionRequest;

import java.util.HashSet;
import java.util.Set;

public class VideoListAdapter extends ListAdapter<VideoItem, VideoListAdapter.VideoViewHolder> {

    public interface Listener {
        void onVideoClicked(@NonNull VideoItem item);

        void onPlayClicked(@NonNull VideoItem item);

        void onVideoLongPressed(@NonNull VideoItem item, @NonNull View anchor);
    }

    private final Listener listener;
    private final Set<String> selectedUris = new HashSet<>();
    private VideoCompressionRequest compressionRequest;

    public VideoListAdapter(@NonNull Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setSelectedUris(@NonNull Set<String> uris) {
        selectedUris.clear();
        selectedUris.addAll(uris);
        notifyDataSetChanged();
    }

    public void setCompressionRequest(@NonNull VideoCompressionRequest compressionRequest) {
        this.compressionRequest = compressionRequest;
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
        holder.bind(item, isSelected, compressionRequest, listener);
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final ImageView thumbnailView;
        private final ImageView playView;
        private final ImageView selectedView;
        private final FrameLayout selectionLayout;
        private final TextView durationView;
        private final TextView dateView;
        private final TextView nameView;
        private final TextView sizeView;
        private final TextView originalSizeView;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardVideo);
            thumbnailView = itemView.findViewById(R.id.imageThumbnail);
            playView = itemView.findViewById(R.id.buttonPlay);
            selectedView = itemView.findViewById(R.id.imageSelected);
            selectionLayout = itemView.findViewById(R.id.layoutSelection);
            durationView = itemView.findViewById(R.id.textDuration);
            dateView = itemView.findViewById(R.id.textDate);
            nameView = itemView.findViewById(R.id.textName);
            sizeView = itemView.findViewById(R.id.textSize);
            originalSizeView = itemView.findViewById(R.id.textOriginalSize);
        }

        void bind(
                @NonNull VideoItem item,
                boolean isSelected,
                @NonNull VideoCompressionRequest compressionRequest,
                @NonNull Listener listener
        ) {
            nameView.setText(item.getDisplayName());
            dateView.setText(FormatUtils.formatShortDate(item.getDateAddedMs()));
            durationView.setText(FormatUtils.formatDuration(item.getDurationMs()));
            selectedView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            selectionLayout.setBackgroundResource(isSelected
                    ? R.drawable.bg_compress_button_circle_active
                    : R.drawable.bg_compress_button_circle);
            int strokeColor = ContextCompat.getColor(
                    itemView.getContext(),
                    isSelected ? R.color.color_scan_gold : R.color.color_scan_border
            );
            cardView.setStrokeColor(strokeColor);
            cardView.setStrokeWidth((int) (itemView.getResources().getDisplayMetrics().density * (isSelected ? 2 : 1)));
            if (isSelected) {
                originalSizeView.setVisibility(View.VISIBLE);
                originalSizeView.setPaintFlags(originalSizeView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                originalSizeView.setText(FormatUtils.formatStorage(item.getSizeBytes()));
                long estimatedCompressedBytes = VideoCompressionEstimateUtils.estimateCompressedBytes(item, compressionRequest);
                sizeView.setText(FormatUtils.formatStorage(estimatedCompressedBytes));
                sizeView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_scan_gold));
            } else {
                originalSizeView.setVisibility(View.GONE);
                originalSizeView.setPaintFlags(originalSizeView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                sizeView.setText(FormatUtils.formatStorage(item.getSizeBytes()));
                sizeView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            }
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
            playView.setOnClickListener(v -> listener.onPlayClicked(item));
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


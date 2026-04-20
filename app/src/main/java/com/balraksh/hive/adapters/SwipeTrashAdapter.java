package com.balraksh.hive.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.balraksh.hive.R;
import com.balraksh.hive.data.SwipeMediaItem;
import com.balraksh.hive.utils.FormatUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SwipeTrashAdapter extends RecyclerView.Adapter<SwipeTrashAdapter.TrashViewHolder> {

    public interface Listener {
        void onItemClicked(@NonNull SwipeMediaItem item);
    }

    private final Listener listener;
    private final List<SwipeMediaItem> items = new ArrayList<>();
    private final Set<String> selectedKeys = new LinkedHashSet<>();

    public SwipeTrashAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<SwipeMediaItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setSelectedKeys(@NonNull Set<String> keys) {
        selectedKeys.clear();
        selectedKeys.addAll(keys);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_swipe_trash, parent, false);
        return new TrashViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashViewHolder holder, int position) {
        SwipeMediaItem item = items.get(position);
        boolean selected = selectedKeys.contains(item.getStableKey());
        Glide.with(holder.preview)
                .load(item.getUri())
                .centerCrop()
                .into(holder.preview);
        holder.sizeText.setText(FormatUtils.formatStorage(item.getSizeBytes()));
        holder.typeIcon.setImageResource(item.isVideo() ? R.drawable.ic_video : R.drawable.ic_photo);
        holder.selectionBadge.setImageResource(selected
                ? R.drawable.ic_check_filled
                : R.drawable.ic_circle_outline);
        holder.selectionBadge.setBackgroundResource(selected
                ? R.drawable.bg_swipe_trash_check
                : android.R.color.transparent);
        holder.overlay.setAlpha(selected ? 0.16f : 0.34f);
        holder.itemView.setOnClickListener(v -> listener.onItemClicked(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class TrashViewHolder extends RecyclerView.ViewHolder {

        private final ImageView preview;
        private final View overlay;
        private final ImageView selectionBadge;
        private final TextView sizeText;
        private final ImageView typeIcon;

        TrashViewHolder(@NonNull View itemView) {
            super(itemView);
            preview = itemView.findViewById(R.id.imageTrashPreview);
            overlay = itemView.findViewById(R.id.viewTrashOverlay);
            selectionBadge = itemView.findViewById(R.id.imageTrashSelection);
            sizeText = itemView.findViewById(R.id.textTrashSize);
            typeIcon = itemView.findViewById(R.id.imageTrashType);
        }
    }
}

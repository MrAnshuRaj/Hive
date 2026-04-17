package com.balraksh.hive.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.balraksh.hive.R;
import com.balraksh.hive.data.DuplicateGroup;
import com.balraksh.hive.data.MediaGroup;
import com.balraksh.hive.data.MediaImageItem;
import com.balraksh.hive.data.SimilarGroup;
import com.balraksh.hive.repository.ScanSessionStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultGroupAdapter extends RecyclerView.Adapter<ResultGroupAdapter.GroupViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged();
    }

    private final List<MediaGroup> groups = new ArrayList<>();
    private final OnSelectionChangedListener listener;
    private int lastAnimatedPosition = -1;

    public ResultGroupAdapter(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public void setGroups(List<? extends MediaGroup> newGroups) {
        groups.clear();
        groups.addAll(newGroups);
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        MediaGroup group = groups.get(position);
        Context context = holder.itemView.getContext();
        holder.titleText.setText(buildTitle(context, group, position));
        holder.subtitleText.setText(buildSubtitle(group));
        holder.countBadgeText.setText(context.getResources().getQuantityString(
                R.plurals.result_items_count,
                group.getItems().size(),
                group.getItems().size()
        ));

        if (holder.recyclerView.getAdapter() == null) {
            holder.recyclerView.setLayoutManager(
                    new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            );
        }
        holder.recyclerView.setAdapter(new ThumbnailAdapter(group, (item, willSelect) -> {
            ScanSessionStore.getInstance().setSelected(item.getId(), willSelect);
            notifyItemChanged(position, "selection");
            if (listener != null) {
                listener.onSelectionChanged();
            }
        }));

        if (position > lastAnimatedPosition) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(28f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(260L)
                    .setStartDelay(position * 70L)
                    .start();
            lastAnimatedPosition = position;
        } else {
            holder.itemView.setAlpha(1f);
            holder.itemView.setTranslationY(0f);
        }
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    private String buildTitle(@NonNull Context context, @NonNull MediaGroup group, int position) {
        String bucketName = findBucketName(group);
        if (group instanceof SimilarGroup) {
            if (!TextUtils.isEmpty(bucketName)) {
                return context.getString(R.string.result_group_similar_bucket_title, bucketName);
            }
            return context.getString(R.string.result_group_similar_fallback, position + 1);
        }
        if (group instanceof DuplicateGroup) {
            if (!TextUtils.isEmpty(bucketName)) {
                return context.getString(R.string.result_group_duplicate_bucket_title, bucketName);
            }
            return context.getString(R.string.result_group_duplicate_fallback, position + 1);
        }
        return context.getString(R.string.group_title, position + 1);
    }

    private String findBucketName(@NonNull MediaGroup group) {
        for (MediaImageItem item : group.getItems()) {
            if (!TextUtils.isEmpty(item.getBucketName())) {
                return item.getBucketName();
            }
        }
        return null;
    }

    private String buildSubtitle(@NonNull MediaGroup group) {
        MediaImageItem bestItem = group.getBestItem();
        long timestamp = bestItem == null ? 0L : bestItem.getDateTaken();
        if (timestamp <= 0L) {
            return "";
        }
        String dayLabel;
        if (DateUtils.isToday(timestamp)) {
            dayLabel = "Today";
        } else if (DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS)) {
            dayLabel = "Yesterday";
        } else {
            dayLabel = new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(timestamp));
        }
        String timeLabel = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(timestamp));
        return dayLabel + " • " + timeLabel;
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleText;
        private final TextView subtitleText;
        private final TextView countBadgeText;
        private final RecyclerView recyclerView;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.textGroupTitle);
            subtitleText = itemView.findViewById(R.id.textGroupSubtitle);
            countBadgeText = itemView.findViewById(R.id.textItemCountBadge);
            recyclerView = itemView.findViewById(R.id.recyclerThumbnails);
        }
    }
}

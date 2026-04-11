package com.balraksh.safkaro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.data.MediaGroup;
import com.balraksh.safkaro.data.MediaImageItem;
import com.balraksh.safkaro.repository.ScanSessionStore;
import com.balraksh.safkaro.utils.FormatUtils;

public class ResultGroupAdapter extends RecyclerView.Adapter<ResultGroupAdapter.GroupViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged();
    }

    private final List<MediaGroup> groups = new ArrayList<>();
    private final OnSelectionChangedListener listener;

    public ResultGroupAdapter(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public void setGroups(List<? extends MediaGroup> newGroups) {
        groups.clear();
        groups.addAll(newGroups);
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
        holder.titleText.setText(holder.itemView.getContext().getString(R.string.group_title, position + 1));
        holder.spaceText.setText(holder.itemView.getContext().getString(
                R.string.space_to_save,
                FormatUtils.formatStorage(ScanSessionStore.getInstance().getGroupSelectedBytes(group))
        ));

        if (holder.recyclerView.getAdapter() == null) {
            holder.recyclerView.setLayoutManager(
                    new LinearLayoutManager(holder.itemView.getContext(), RecyclerView.HORIZONTAL, false)
            );
        }
        holder.recyclerView.setAdapter(new ThumbnailAdapter(group, (item, willSelect) -> {
            ScanSessionStore.getInstance().setSelected(item.getId(), willSelect);
            notifyItemChanged(position);
            if (listener != null) {
                listener.onSelectionChanged();
            }
        }));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleText;
        private final RecyclerView recyclerView;
        private final TextView spaceText;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.textGroupTitle);
            recyclerView = itemView.findViewById(R.id.recyclerThumbnails);
            spaceText = itemView.findViewById(R.id.textSpaceToSave);
        }
    }
}

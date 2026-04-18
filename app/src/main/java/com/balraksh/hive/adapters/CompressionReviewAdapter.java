package com.balraksh.hive.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import com.balraksh.hive.R;
import com.balraksh.hive.utils.FormatUtils;
import com.balraksh.hive.video.VideoCompressionResult;

import java.util.HashMap;
import java.util.Map;

public class CompressionReviewAdapter extends ListAdapter<VideoCompressionResult, CompressionReviewAdapter.ResultViewHolder> {

    public enum VideoAction {
        REPLACE,
        KEEP_BOTH,
        DELETE
    }

    public interface Listener {
        void onPlayClicked(@NonNull VideoCompressionResult result);

        void onActionSelected(@NonNull VideoCompressionResult result, @NonNull VideoAction action);
    }

    private final Listener listener;
    private final Map<String, VideoAction> actionMap = new HashMap<>();

    public CompressionReviewAdapter(@NonNull Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setActionMap(@NonNull Map<String, VideoAction> actions) {
        actionMap.clear();
        actionMap.putAll(actions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_review, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        VideoCompressionResult result = getItem(position);
        holder.bind(result, actionMap.get(result.getSourceUri().toString()), listener);
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {

        private final ImageView previewView;
        private final ImageView playButton;
        private final TextView failedBadgeView;
        private final TextView nameView;
        private final TextView metaView;
        private final View successInfoLayout;
        private final TextView originalSizeView;
        private final TextView compressedSizeView;
        private final TextView savedPercentView;
        private final TextView savedBytesView;
        private final TextView errorMessageView;
        private final View actionsLayout;
        private final LinearLayout replaceButton;
        private final LinearLayout keepButton;
        private final LinearLayout deleteButton;
        private final MaterialCardView replaceCard;
        private final MaterialCardView keepCard;
        private final MaterialCardView deleteCard;
        private final ImageView replaceIcon;
        private final ImageView keepIcon;
        private final ImageView deleteIcon;
        private final TextView replaceLabel;
        private final TextView keepLabel;
        private final TextView deleteLabel;

        ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            previewView = itemView.findViewById(R.id.imagePreview);
            playButton = itemView.findViewById(R.id.buttonPlay);
            failedBadgeView = itemView.findViewById(R.id.textFailedBadge);
            nameView = itemView.findViewById(R.id.textName);
            metaView = itemView.findViewById(R.id.textMeta);
            successInfoLayout = itemView.findViewById(R.id.layoutSuccessInfo);
            originalSizeView = itemView.findViewById(R.id.textOriginalSize);
            compressedSizeView = itemView.findViewById(R.id.textCompressedSize);
            savedPercentView = itemView.findViewById(R.id.textSavedPercent);
            savedBytesView = itemView.findViewById(R.id.textSavedBytes);
            errorMessageView = itemView.findViewById(R.id.textErrorMessage);
            actionsLayout = itemView.findViewById(R.id.layoutActions);
            replaceButton = itemView.findViewById(R.id.buttonActionReplace);
            keepButton = itemView.findViewById(R.id.buttonActionKeep);
            deleteButton = itemView.findViewById(R.id.buttonActionDelete);
            replaceCard = itemView.findViewById(R.id.cardActionReplace);
            keepCard = itemView.findViewById(R.id.cardActionKeep);
            deleteCard = itemView.findViewById(R.id.cardActionDelete);
            replaceIcon = itemView.findViewById(R.id.iconActionReplace);
            keepIcon = itemView.findViewById(R.id.iconActionKeep);
            deleteIcon = itemView.findViewById(R.id.iconActionDelete);
            replaceLabel = itemView.findViewById(R.id.textActionReplace);
            keepLabel = itemView.findViewById(R.id.textActionKeep);
            deleteLabel = itemView.findViewById(R.id.textActionDelete);
        }

        void bind(
                @NonNull VideoCompressionResult result,
                VideoAction currentAction,
                @NonNull Listener listener
        ) {
            nameView.setText(result.getSourceName());
            metaView.setText(FormatUtils.formatDuration(result.getDurationMs()));
            Glide.with(previewView)
                    .load(result.isSuccess() && result.getOutputUri() != null ? result.getOutputUri() : result.getSourceUri())
                    .centerCrop()
                    .into(previewView);

            playButton.setOnClickListener(v -> listener.onPlayClicked(result));

            if (result.isSuccess()) {
                failedBadgeView.setVisibility(View.GONE);
                successInfoLayout.setVisibility(View.VISIBLE);
                actionsLayout.setVisibility(View.VISIBLE);
                errorMessageView.setVisibility(View.GONE);

                originalSizeView.setText(FormatUtils.formatStorage(result.getInputSizeBytes()));
                compressedSizeView.setText(FormatUtils.formatStorage(result.getOutputSizeBytes()));
                savedPercentView.setText("-" + result.getSavedPercent() + "%");
                savedBytesView.setText("Saves " + FormatUtils.formatStorage(result.getSavedBytes()));

                VideoAction resolvedAction = currentAction == null ? VideoAction.REPLACE : currentAction;
                bindActionState(replaceCard, replaceIcon, replaceLabel, resolvedAction == VideoAction.REPLACE,
                        R.color.color_cleanup_success_green, R.color.white, R.color.color_cleanup_success_green);
                bindActionState(keepCard, keepIcon, keepLabel, resolvedAction == VideoAction.KEEP_BOTH,
                        R.color.color_scan_gold, R.color.color_scan_bg, R.color.color_scan_gold);
                bindActionState(deleteCard, deleteIcon, deleteLabel, resolvedAction == VideoAction.DELETE,
                        R.color.color_error, R.color.white, R.color.color_error);

                replaceButton.setOnClickListener(v -> listener.onActionSelected(result, VideoAction.REPLACE));
                keepButton.setOnClickListener(v -> listener.onActionSelected(result, VideoAction.KEEP_BOTH));
                deleteButton.setOnClickListener(v -> listener.onActionSelected(result, VideoAction.DELETE));
            } else {
                failedBadgeView.setVisibility(View.VISIBLE);
                successInfoLayout.setVisibility(View.GONE);
                actionsLayout.setVisibility(View.GONE);
                errorMessageView.setVisibility(View.VISIBLE);
                errorMessageView.setText(result.getErrorMessage());
            }
        }

        private void bindActionState(
                @NonNull MaterialCardView cardView,
                @NonNull ImageView iconView,
                @NonNull TextView labelView,
                boolean selected,
                @ColorRes int activeBackgroundColor,
                @ColorRes int activeIconColor,
                @ColorRes int activeLabelColor
        ) {
            if (selected) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), activeBackgroundColor));
                cardView.setStrokeWidth((int) (itemView.getResources().getDisplayMetrics().density * 2));
                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), activeBackgroundColor));
                iconView.setColorFilter(ContextCompat.getColor(itemView.getContext(), activeIconColor));
                labelView.setTextColor(ContextCompat.getColor(itemView.getContext(), activeLabelColor));
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.color_scan_surface));
                cardView.setStrokeWidth((int) itemView.getResources().getDisplayMetrics().density);
                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.color_scan_border));
                iconView.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.color_scan_text_secondary));
                labelView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_scan_text_secondary));
            }
        }
    }

    private static final DiffUtil.ItemCallback<VideoCompressionResult> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<VideoCompressionResult>() {
                @Override
                public boolean areItemsTheSame(@NonNull VideoCompressionResult oldItem, @NonNull VideoCompressionResult newItem) {
                    return oldItem.getSourceUri().equals(newItem.getSourceUri());
                }

                @Override
                public boolean areContentsTheSame(@NonNull VideoCompressionResult oldItem, @NonNull VideoCompressionResult newItem) {
                    return oldItem.getSourceUri().equals(newItem.getSourceUri())
                            && oldItem.isSuccess() == newItem.isSuccess()
                            && oldItem.getInputSizeBytes() == newItem.getInputSizeBytes()
                            && oldItem.getOutputSizeBytes() == newItem.getOutputSizeBytes();
                }
            };
}

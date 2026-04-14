package hcmute.edu.vn.documentfileeditor.Adapter;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.documentfileeditor.Model.Entity.ChatMessage;
import hcmute.edu.vn.documentfileeditor.R;

/**
 * RecyclerView Adapter for displaying chat messages between user and AI assistant.
 * Shared by both ChatFragment and DocumentEditorActivity's chat bottom sheet.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout layout = new LinearLayout(parent.getContext());
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(0, 16, 0, 16);

        LinearLayout iconContainer = new LinearLayout(parent.getContext());
        int iconSize = (int) (40 * parent.getContext().getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconContainer.setLayoutParams(iconParams);
        iconContainer.setGravity(Gravity.CENTER);

        TextView tvContent = new TextView(parent.getContext());
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textParams.setMargins(24, 0, 24, 0);
        tvContent.setLayoutParams(textParams);
        tvContent.setPadding(32, 32, 32, 32);
        tvContent.setTextSize(14f);

        layout.addView(iconContainer);
        layout.addView(tvContent);

        return new ChatViewHolder(layout, iconContainer, tvContent);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.tvContent.setText(msg.getContent());
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvContent.getLayoutParams();

        if ("user".equals(msg.getRole())) {
            holder.layout.setGravity(Gravity.END);
            params.setMargins(100, 0, 0, 0);
            holder.tvContent.setBackgroundResource(R.drawable.bg_blue_500_rounded);
            holder.tvContent.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            holder.iconContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
        } else {
            holder.layout.setGravity(Gravity.START);
            params.setMargins(0, 0, 100, 0);
            holder.tvContent.setBackgroundResource(R.drawable.bg_neutral_100_rounded);
            holder.tvContent.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.black));
            holder.iconContainer.setBackgroundResource(R.drawable.bg_purple_500_rounded);
        }
        holder.tvContent.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout layout;
        final LinearLayout iconContainer;
        final TextView tvContent;

        ChatViewHolder(View itemView, LinearLayout iconContainer, TextView tvContent) {
            super(itemView);
            this.layout = (LinearLayout) itemView;
            this.iconContainer = iconContainer;
            this.tvContent = tvContent;
        }
    }
}

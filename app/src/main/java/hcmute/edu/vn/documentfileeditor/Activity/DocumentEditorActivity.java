package hcmute.edu.vn.documentfileeditor.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.R;

public class DocumentEditorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_editor);

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Save button
        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> Toast.makeText(this, "Document saved!", Toast.LENGTH_SHORT).show());

        // More button
        ImageView btnMore = findViewById(R.id.btn_more);
        btnMore.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);
            popup.getMenu().add("Share");
            popup.getMenu().add("Export as PDF");
            popup.getMenu().add("Download");
            popup.setOnMenuItemClickListener(item -> {
                Toast.makeText(this, item.getTitle() + " coming soon", Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });

        // Formatting buttons with toggle highlight
        setupFormatToggle(R.id.btn_bold);
        setupFormatToggle(R.id.btn_italic);
        setupFormatToggle(R.id.btn_underline);

        // Non-toggle formatting
        int[] simpleButtons = {
                R.id.btn_align_left, R.id.btn_align_center, R.id.btn_align_right,
                R.id.btn_list, R.id.btn_list_ordered, R.id.btn_insert_image
        };
        for (int id : simpleButtons) {
            findViewById(id).setOnClickListener(v -> Toast.makeText(this, "Format action", Toast.LENGTH_SHORT).show());
        }

        // AI FAB
        FrameLayout fabAi = findViewById(R.id.fab_ai);
        fabAi.setOnClickListener(v -> showAIBottomSheet());
    }

    private void setupFormatToggle(int viewId) {
        ImageView btn = findViewById(viewId);
        btn.setOnClickListener(v -> {
            boolean selected = !v.isSelected();
            v.setSelected(selected);
            if (selected) {
                v.setBackgroundColor(getResources().getColor(R.color.blue_100, null));
            } else {
                v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        });
    }

    private void showAIBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ai_assistant, null);
        dialog.setContentView(sheetView);

        LinearLayout suggestionArea = sheetView.findViewById(R.id.ai_suggestion_area);
        TextView tvSuggestion = sheetView.findViewById(R.id.tv_ai_suggestion);

        // Open Chat button
        sheetView.findViewById(R.id.btn_open_chat).setOnClickListener(v -> {
            dialog.dismiss();
            openChatBottomSheet();
        });

        // AI Action buttons
        sheetView.findViewById(R.id.btn_ai_improve).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText(
                    "Enhanced version:\n\nThis document serves as an exemplary template. Feel free to modify this content and leverage advanced AI capabilities to refine your composition.\n\nAccess the AI assistant to receive professional writing guidance, comprehensive grammar corrections, and intelligent content recommendations.");
        });

        sheetView.findViewById(R.id.btn_ai_fix).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText(
                    "Grammar corrections applied:\n- Fixed punctuation\n- Corrected spelling\n- Improved sentence structure\n\nYour text is now grammatically correct!");
        });

        sheetView.findViewById(R.id.btn_ai_shorten).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText(
                    "Condensed version:\n\nEditable sample document. Use AI features for writing improvements.\n\nClick AI button for assistance, corrections, and suggestions.");
        });

        sheetView.findViewById(R.id.btn_ai_expand).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText(
                    "Expanded version:\n\nThis comprehensive document serves as a detailed sample template for your editing needs. You have complete freedom to modify and customize this text according to your requirements. Additionally, you can harness the power of artificial intelligence features to significantly enhance and improve the quality of your writing.");
        });

        sheetView.findViewById(R.id.btn_ai_translate).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText(
                    "Translated to Vietnamese:\n\nĐây là một tài liệu mẫu. Bạn có thể chỉnh sửa văn bản này và sử dụng các tính năng AI để cải thiện bài viết của mình.\n\nNhấp vào nút AI để nhận trợ giúp viết, sửa lỗi ngữ pháp và đề xuất nội dung.");
        });

        sheetView.findViewById(R.id.btn_ai_edit_image).setOnClickListener(v -> {
            Toast.makeText(this, "Edit Image coming soon", Toast.LENGTH_SHORT).show();
        });

        // Apply suggestion
        sheetView.findViewById(R.id.btn_apply_suggestion).setOnClickListener(v -> {
            String suggestion = tvSuggestion.getText().toString();
            if (!suggestion.isEmpty()) {
                android.widget.EditText editor = findViewById(R.id.et_editor);
                String[] lines = suggestion.split("\n", 2);
                if (lines.length > 1) {
                    editor.setText(lines[1].trim());
                } else {
                    editor.setText(suggestion);
                }
                Toast.makeText(this, "AI suggestion applied!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // Dismiss suggestion
        sheetView.findViewById(R.id.btn_dismiss_suggestion).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.GONE);
            tvSuggestion.setText("");
        });

        dialog.show();
    }

    private void openChatBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View sheetView = getLayoutInflater().inflate(R.layout.fragment_chat, null);
        dialog.setContentView(sheetView);

        // Make bottom sheet expand fully
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }

        RecyclerView rvMessages = sheetView.findViewById(R.id.rv_messages);
        EditText etInput = sheetView.findViewById(R.id.et_input);
        ImageButton btnSend = sheetView.findViewById(R.id.btn_send);

        List<ChatMessage> messageList = new ArrayList<>();
        // Add initial bot message
        messageList.add(new ChatMessage("assistant", "Hello! I'm your AI assistant. How can I help you improve your document today?"));

        ChatAdapter adapter = new ChatAdapter(messageList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String input = etInput.getText().toString().trim();
            if (input.isEmpty()) return;

            // Add user message
            messageList.add(new ChatMessage("user", input));
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
            etInput.setText("");

            // Simulate thinking and response
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                String[] responses = {
                    "I can certainly help with that formatting.",
                    "Here's what I suggest for your document: ...",
                    "Based on your text, I think we should expand the second paragraph.",
                    "Let me fix the grammar for you right away!"
                };
                String reply = responses[(int)(Math.random() * responses.length)];
                
                messageList.add(new ChatMessage("assistant", reply));
                adapter.notifyItemInserted(messageList.size() - 1);
                rvMessages.scrollToPosition(messageList.size() - 1);
            }, 1000);
        });

        dialog.show();
    }

    // Chat Message Model
    private static class ChatMessage {
        String role;
        String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    // Chat Adapter logic matching fragment_chat.xml style
    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
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
            iconContainer.setId(View.generateViewId());
            iconContainer.setGravity(android.view.Gravity.CENTER);

            TextView tvContent = new TextView(parent.getContext());
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            textParams.setMargins(24, 0, 24, 0);
            tvContent.setLayoutParams(textParams);
            tvContent.setId(View.generateViewId());
            tvContent.setPadding(32, 32, 32, 32);
            tvContent.setTextSize(14f);

            layout.addView(iconContainer);
            layout.addView(tvContent);

            return new ChatViewHolder(layout, iconContainer, tvContent);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            holder.tvContent.setText(msg.content);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvContent.getLayoutParams();

            if ("user".equals(msg.role)) {
                holder.layout.setGravity(android.view.Gravity.END);
                params.setMargins(100, 0, 0, 0);
                holder.tvContent.setLayoutParams(params);

                holder.tvContent.setBackgroundResource(R.drawable.bg_blue_500_rounded);
                holder.tvContent.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
                holder.iconContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

            } else {
                holder.layout.setGravity(android.view.Gravity.START);
                params.setMargins(0, 0, 100, 0);
                holder.tvContent.setLayoutParams(params);

                holder.tvContent.setBackgroundResource(R.drawable.bg_neutral_100_rounded);
                holder.tvContent.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.black));
                holder.iconContainer.setBackgroundResource(R.drawable.bg_purple_500_rounded);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class ChatViewHolder extends RecyclerView.ViewHolder {
            LinearLayout layout;
            LinearLayout iconContainer;
            TextView tvContent;

            ChatViewHolder(View itemView, LinearLayout iconContainer, TextView tvContent) {
                super(itemView);
                this.layout = (LinearLayout) itemView;
                this.iconContainer = iconContainer;
                this.tvContent = tvContent;
            }
        }
    }
}

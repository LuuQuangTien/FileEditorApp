package hcmute.edu.vn.documentfileeditor.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.R;

public class ChatFragment extends Fragment {

    private RecyclerView rvMessages;
    private EditText etInput;
    private ImageButton btnSend;

    private ChatAdapter adapter;
    private List<ChatMessage> messageList;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        rvMessages = view.findViewById(R.id.rv_messages);
        etInput = view.findViewById(R.id.et_input);
        btnSend = view.findViewById(R.id.btn_send);

        messageList = new ArrayList<>();
        // Add initial bot message
        messageList.add(new ChatMessage("assistant", "Hello! I'm your AI assistant for document management. I can help you with:\n\n• Searching through your documents\n• Summarizing PDFs and long documents\n• Answering questions about your files\n• Organizing and categorizing documents\n• Extracting specific information\n\nHow can I help you today?"));

        adapter = new ChatAdapter(messageList);
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        return view;
    }

    private void sendMessage() {
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
                "I can help you with that! Based on your documents, here's what I found...",
                "That's a great question. Let me analyze your documents to provide you with the most accurate information.",
                "I've searched through your document library. Here are the relevant findings...",
                "Based on the content of your files, I can provide you with the following insights..."
            };
            String reply = responses[(int)(Math.random() * responses.length)];
            
            messageList.add(new ChatMessage("assistant", reply));
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
            
        }, 1500);
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

    // Modern RecyclerView Adapter for Chat
    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

        private final List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // For a simpler approach natively creating the view
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(0, 16, 0, 16);

            // Icon background container
            LinearLayout iconContainer = new LinearLayout(parent.getContext());
            int iconSize = (int) (40 * parent.getContext().getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconContainer.setLayoutParams(iconParams);
            iconContainer.setId(View.generateViewId());
            iconContainer.setGravity(android.view.Gravity.CENTER);

            // Message text container
            TextView tvContent = new TextView(parent.getContext());
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            textParams.setMargins(24, 0, 24, 0); // 8dp approx
            tvContent.setLayoutParams(textParams);
            tvContent.setId(View.generateViewId());
            tvContent.setPadding(32, 32, 32, 32);
            tvContent.setTextSize(14);

            layout.addView(iconContainer);
            layout.addView(tvContent);

            return new ChatViewHolder(layout, iconContainer, tvContent);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            holder.tvContent.setText(msg.content);

            if ("user".equals(msg.role)) {
                // User styles
                holder.layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                holder.tvContent.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                
                holder.tvContent.setBackgroundResource(R.drawable.badge_green_bg); 
                holder.tvContent.getBackground().setTint(0xFF2563EB); // blue-600
                holder.tvContent.setTextColor(0xFFFFFFFF);
                
                holder.iconContainer.setBackgroundResource(R.drawable.bg_edittext); // reuse rounded
                holder.iconContainer.getBackground().setTint(0xFFE5E5E5);
                
            } else {
                // Assistant styles
                holder.layout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                holder.tvContent.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                
                holder.tvContent.setBackgroundResource(R.drawable.bg_edittext);
                holder.tvContent.getBackground().setTint(0xFFFFFFFF);
                holder.tvContent.setTextColor(0xFF171717);
                
                holder.iconContainer.setBackgroundResource(R.drawable.badge_green_bg);
                holder.iconContainer.getBackground().setTint(0xFFDBEAFE); // blue-100
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

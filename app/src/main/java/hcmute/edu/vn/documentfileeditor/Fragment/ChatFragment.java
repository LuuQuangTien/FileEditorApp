package hcmute.edu.vn.documentfileeditor.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Adapter.ChatAdapter;
import hcmute.edu.vn.documentfileeditor.Model.Entity.ChatMessage;
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
        rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
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
}

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Dao.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;

public class DocumentEditorActivity extends AppCompatActivity {
    public static final String EXTRA_DOCUMENT_ID = "extra_document_id";
    public static final String EXTRA_DOCUMENT_NAME = "extra_document_name";
    public static final String EXTRA_LOCAL_PATH = "extra_local_path";
    public static final String EXTRA_CLOUD_URL = "extra_cloud_url";
    public static final String EXTRA_FILE_TYPE = "extra_file_type";

    private DocumentRepository documentRepository;
    private DocumentFB currentDocument;
    private EditText editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_editor);

        documentRepository = DocumentRepository.getInstance(this);
        editor = findViewById(R.id.et_editor);
        bindDocumentInfo();

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveDocument());

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

        setupFormatToggle(R.id.btn_bold);
        setupFormatToggle(R.id.btn_italic);
        setupFormatToggle(R.id.btn_underline);

        int[] simpleButtons = {
                R.id.btn_align_left, R.id.btn_align_center, R.id.btn_align_right,
                R.id.btn_list, R.id.btn_list_ordered, R.id.btn_insert_image
        };
        for (int id : simpleButtons) {
            findViewById(id).setOnClickListener(v -> Toast.makeText(this, "Format action", Toast.LENGTH_SHORT).show());
        }

        FrameLayout fabAi = findViewById(R.id.fab_ai);
        fabAi.setOnClickListener(v -> showAIBottomSheet());
    }

    private void bindDocumentInfo() {
        TextView tvTitle = findViewById(R.id.tv_doc_title);

        currentDocument = new DocumentFB();
        currentDocument.setId(getIntent().getStringExtra(EXTRA_DOCUMENT_ID));
        currentDocument.setUserId(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null);
        currentDocument.setFileName(getIntent().getStringExtra(EXTRA_DOCUMENT_NAME));
        currentDocument.setLocalPath(getIntent().getStringExtra(EXTRA_LOCAL_PATH));
        currentDocument.setCloudStorageUrl(getIntent().getStringExtra(EXTRA_CLOUD_URL));

        String fileTypeName = getIntent().getStringExtra(EXTRA_FILE_TYPE);
        try {
            currentDocument.setFileType(fileTypeName != null ? FileType.valueOf(fileTypeName) : FileType.WORD);
        } catch (IllegalArgumentException e) {
            currentDocument.setFileType(FileType.WORD);
        }

        if (currentDocument.getFileName() != null && !currentDocument.getFileName().isEmpty()) {
            tvTitle.setText(currentDocument.getFileName());
        }

        String content = readTextFromLocalFile(currentDocument.getLocalPath());
        editor.setText(content == null || content.isEmpty() ? "Start writing here..." : content);
    }

    private String readTextFromLocalFile(String localPath) {
        if (localPath == null || localPath.isEmpty()) {
            return null;
        }
        try {
            File file = new File(localPath);
            if (!file.exists()) {
                return null;
            }
            InputStream input = getContentResolver().openInputStream(android.net.Uri.fromFile(file));
            if (input == null) {
                return null;
            }
            byte[] bytes = new byte[(int) file.length()];
            int read = input.read(bytes);
            input.close();
            if (read <= 0) {
                return null;
            }
            return new String(bytes, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private void saveDocument() {
        if (currentDocument == null || currentDocument.getLocalPath() == null || currentDocument.getLocalPath().isEmpty()) {
            Toast.makeText(this, "Document path is unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            FileOutputStream output = new FileOutputStream(new File(currentDocument.getLocalPath()), false);
            output.write(editor.getText().toString().getBytes(StandardCharsets.UTF_8));
            output.close();
        } catch (Exception e) {
            Toast.makeText(this, "Could not save local file.", Toast.LENGTH_SHORT).show();
            return;
        }

        documentRepository.saveDocument(currentDocument, new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB documentFB) {
                currentDocument = documentFB;
                Toast.makeText(DocumentEditorActivity.this, "Document saved locally. Cloud sync continues in background.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProgress(int progressPercentage) {
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DocumentEditorActivity.this, "Save failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFormatToggle(int viewId) {
        ImageView btn = findViewById(viewId);
        btn.setOnClickListener(v -> {
            boolean selected = !v.isSelected();
            v.setSelected(selected);
            v.setBackgroundColor(selected ? getResources().getColor(R.color.blue_100, null) : android.graphics.Color.TRANSPARENT);
        });
    }

    private void showAIBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ai_assistant, null);
        dialog.setContentView(sheetView);

        LinearLayout suggestionArea = sheetView.findViewById(R.id.ai_suggestion_area);
        TextView tvSuggestion = sheetView.findViewById(R.id.tv_ai_suggestion);

        sheetView.findViewById(R.id.btn_open_chat).setOnClickListener(v -> {
            dialog.dismiss();
            openChatBottomSheet();
        });

        sheetView.findViewById(R.id.btn_ai_improve).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText("Enhanced version:\n\nImprove clarity, structure, and tone for this document.");
        });
        sheetView.findViewById(R.id.btn_ai_fix).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText("Grammar corrections applied:\n- Fixed punctuation\n- Corrected spelling\n- Improved sentence structure");
        });
        sheetView.findViewById(R.id.btn_ai_shorten).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText("Condensed version:\n\nShorter and more direct content.");
        });
        sheetView.findViewById(R.id.btn_ai_expand).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText("Expanded version:\n\nMore detailed content with fuller explanations.");
        });
        sheetView.findViewById(R.id.btn_ai_translate).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText("Translated text:\n\nBan dich se hien thi tai day.");
        });
        sheetView.findViewById(R.id.btn_ai_edit_image).setOnClickListener(v ->
                Toast.makeText(this, "Edit Image coming soon", Toast.LENGTH_SHORT).show());

        sheetView.findViewById(R.id.btn_apply_suggestion).setOnClickListener(v -> {
            String suggestion = tvSuggestion.getText().toString();
            if (!suggestion.isEmpty()) {
                String[] lines = suggestion.split("\n", 2);
                editor.setText(lines.length > 1 ? lines[1].trim() : suggestion);
                Toast.makeText(this, "AI suggestion applied!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

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
        messageList.add(new ChatMessage("assistant", "Hello! I'm your AI assistant. How can I help you improve your document today?"));

        ChatAdapter adapter = new ChatAdapter(messageList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String input = etInput.getText().toString().trim();
            if (input.isEmpty()) {
                return;
            }
            messageList.add(new ChatMessage("user", input));
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
            etInput.setText("");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                messageList.add(new ChatMessage("assistant", "I can help revise that section."));
                adapter.notifyItemInserted(messageList.size() - 1);
                rvMessages.scrollToPosition(messageList.size() - 1);
            }, 800);
        });

        dialog.show();
    }

    private static class ChatMessage {
        String role;
        String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(0, 16, 0, 16);

            LinearLayout iconContainer = new LinearLayout(parent.getContext());
            int iconSize = (int) (40 * parent.getContext().getResources().getDisplayMetrics().density);
            iconContainer.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
            iconContainer.setGravity(android.view.Gravity.CENTER);

            TextView tvContent = new TextView(parent.getContext());
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
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
            holder.tvContent.setText(msg.content);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvContent.getLayoutParams();

            if ("user".equals(msg.role)) {
                holder.layout.setGravity(android.view.Gravity.END);
                params.setMargins(100, 0, 0, 0);
                holder.tvContent.setBackgroundResource(R.drawable.bg_blue_500_rounded);
                holder.tvContent.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
                holder.iconContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            } else {
                holder.layout.setGravity(android.view.Gravity.START);
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
}

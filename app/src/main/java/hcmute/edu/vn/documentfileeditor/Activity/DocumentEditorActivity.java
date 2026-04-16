package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Adapter.ChatAdapter;
import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.ChatMessage;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.AuthService;
import hcmute.edu.vn.documentfileeditor.Service.DocumentService;
import hcmute.edu.vn.documentfileeditor.Service.GeminiService;

public class DocumentEditorActivity extends AppCompatActivity {
    public static final String EXTRA_DOCUMENT_ID = "extra_document_id";
    public static final String EXTRA_DOCUMENT_NAME = "extra_document_name";
    public static final String EXTRA_LOCAL_PATH = "extra_local_path";
    public static final String EXTRA_CLOUD_URL = "extra_cloud_url";
    public static final String EXTRA_FILE_TYPE = "extra_file_type";

    private DocumentRepository documentRepository;
    private DocumentService documentService;
    private GeminiService geminiService;
    private AuthService authService;
    private DocumentFB currentDocument;
    private EditText editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_editor);

        documentRepository = DocumentRepository.getInstance(this);
        documentService = new DocumentService();
        geminiService = new GeminiService();
        authService = new AuthService();
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

        FrameLayout fabAi = findViewById(R.id.fab_ai);
        fabAi.setOnClickListener(v -> showAIBottomSheet());
    }

    private void bindDocumentInfo() {
        TextView tvTitle = findViewById(R.id.tv_doc_title);
        currentDocument = new DocumentFB();
        currentDocument.setId(getIntent().getStringExtra(EXTRA_DOCUMENT_ID));
        currentDocument.setUserId(authService.getCurrentUserId());
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

        String content = documentService.readTextFromLocalFile(this, currentDocument.getLocalPath());
        editor.setText(content == null || content.isEmpty() ? "" : content);
    }

    private void showAIBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ai_assistant, null);
        dialog.setContentView(sheetView);

        LinearLayout suggestionArea = sheetView.findViewById(R.id.ai_suggestion_area);
        TextView tvSuggestion = sheetView.findViewById(R.id.tv_ai_suggestion);
        
        // Lấy văn bản đang được bôi đen (selection) hoặc toàn bộ văn bản
        String contextText = getSelectedOrAllText();

        sheetView.findViewById(R.id.btn_open_chat).setOnClickListener(v -> {
            dialog.dismiss();
            openChatBottomSheet();
        });

        // Improve Writing
        sheetView.findViewById(R.id.btn_ai_improve).setOnClickListener(v -> 
                callAiAction("Improve the clarity, flow, and professional tone of this text", contextText, suggestionArea, tvSuggestion));

        // Fix Grammar
        sheetView.findViewById(R.id.btn_ai_fix).setOnClickListener(v -> 
                callAiAction("Fix all grammar, spelling, and punctuation mistakes in this text", contextText, suggestionArea, tvSuggestion));

        // Make Shorter
        sheetView.findViewById(R.id.btn_ai_shorten).setOnClickListener(v -> 
                callAiAction("Rewrite this text to be more concise and shorter while keeping key information", contextText, suggestionArea, tvSuggestion));

        // Make Longer
        sheetView.findViewById(R.id.btn_ai_expand).setOnClickListener(v -> 
                callAiAction("Expand this text with more details and elaborate on the points mentioned", contextText, suggestionArea, tvSuggestion));

        // Translate (Auto detect to Vietnamese)
        sheetView.findViewById(R.id.btn_ai_translate).setOnClickListener(v -> 
                callAiAction("Translate this text accurately into Vietnamese", contextText, suggestionArea, tvSuggestion));

        // Edit Image
        sheetView.findViewById(R.id.btn_ai_edit_image).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ImageFilterActivity.class));
        });

        // Apply Suggestion
        sheetView.findViewById(R.id.btn_apply_suggestion).setOnClickListener(v -> {
            String suggestion = tvSuggestion.getText().toString();
            if (!suggestion.isEmpty()) {
                applyTextToEditor(suggestion);
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

    private String getSelectedOrAllText() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (start != end && start >= 0 && end > start) {
            return editor.getText().toString().substring(start, end);
        }
        return editor.getText().toString();
    }

    private void applyTextToEditor(String newText) {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (start != end && start >= 0 && end > start) {
            editor.getText().replace(start, end, newText);
        } else {
            editor.setText(newText);
        }
    }

    private void callAiAction(String prompt, String content, LinearLayout area, TextView resultView) {
        if (content.trim().isEmpty()) {
            Toast.makeText(this, "Please enter some text for AI to process", Toast.LENGTH_SHORT).show();
            return;
        }

        resultView.setText("AI is working...");
        area.setVisibility(View.VISIBLE);

        geminiService.processText(prompt, content, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> resultView.setText(result));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    resultView.setText("Error: " + error);
                    Toast.makeText(DocumentEditorActivity.this, "Failed to call AI", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void openChatBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View sheetView = getLayoutInflater().inflate(R.layout.fragment_chat, null);
        dialog.setContentView(sheetView);

        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        RecyclerView rvMessages = sheetView.findViewById(R.id.rv_messages);
        EditText etInput = sheetView.findViewById(R.id.et_input);
        ImageButton btnSend = sheetView.findViewById(R.id.btn_send);

        List<ChatMessage> messageList = new ArrayList<>();
        messageList.add(new ChatMessage("assistant", "How can I help with your document?"));
        ChatAdapter adapter = new ChatAdapter(messageList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String input = etInput.getText().toString().trim();
            if (input.isEmpty()) return;

            messageList.add(new ChatMessage("user", input));
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
            etInput.setText("");

            geminiService.processText("Help the user with their request regarding the document", input, new GeminiService.GeminiCallback() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        messageList.add(new ChatMessage("assistant", result));
                        adapter.notifyItemInserted(messageList.size() - 1);
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    });
                }
                @Override public void onError(String error) {}
            });
        });
        dialog.show();
    }

    private void saveDocument() {
        if (currentDocument == null || currentDocument.getLocalPath() == null) return;
        if (!documentService.saveTextToLocalFile(currentDocument.getLocalPath(), editor.getText().toString())) return;
        documentRepository.saveDocument(currentDocument, new DocumentCallback.UploadCallback() {
            @Override public void onSuccess(DocumentFB doc) { Toast.makeText(DocumentEditorActivity.this, "Saved", Toast.LENGTH_SHORT).show(); }
            @Override public void onProgress(int p) {}
            @Override public void onFailure(Exception e) {}
        });
    }

    private void setupFormatToggle(int viewId) {
        ImageView btn = findViewById(viewId);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                v.setSelected(!v.isSelected());
                v.setBackgroundColor(v.isSelected() ? getResources().getColor(R.color.blue_100, null) : android.graphics.Color.TRANSPARENT);
            });
        }
    }
}

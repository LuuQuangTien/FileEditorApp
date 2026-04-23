package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.MotionEvent;
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
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
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
    private static final int MAX_AI_CHARS_PER_REQUEST = 6000;
    private int aiSelectionStart = -1;
    private int aiSelectionEnd = -1;

    // Word-style formatting flags
    private boolean isBold = false;
    private boolean isItalic = false;
    private boolean isUnderline = false;

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
            popup.getMenu().add("Share Document");
            popup.getMenu().add("Export as PDF");
            popup.getMenu().add("Download to Storage");
            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.contains("Share")) {
                    shareDocumentContent();
                } else if (title.contains("PDF")) {
                    exportAsPdf();
                } else if (title.contains("Download")) {
                    downloadToStorage();
                }
                return true;
            });
            popup.show();
        });

        // Initialize Toolbar and Typing logic
        setupToolbarButtons();
        setupTextWatcher();

        FrameLayout fabAi = findViewById(R.id.fab_ai);
        fabAi.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                captureAiSelection();
            }
            return false;
        });
        fabAi.setOnClickListener(v -> showAIBottomSheet());
    }

    private void setupToolbarButtons() {
        // Bold Button
        View btnBold = findViewById(R.id.btn_bold);
        btnBold.setOnClickListener(v -> {
            isBold = !isBold;
            updateButtonState(btnBold, isBold);
            applyStyleToSelection(Typeface.BOLD, isBold);
        });

        // Italic Button
        View btnItalic = findViewById(R.id.btn_italic);
        btnItalic.setOnClickListener(v -> {
            isItalic = !isItalic;
            updateButtonState(btnItalic, isItalic);
            applyStyleToSelection(Typeface.ITALIC, isItalic);
        });

        // Underline Button
        View btnUnderline = findViewById(R.id.btn_underline);
        btnUnderline.setOnClickListener(v -> {
            isUnderline = !isUnderline;
            updateButtonState(btnUnderline, isUnderline);
            applyUnderlineToSelection(isUnderline);
        });
        
        // Alignment Buttons
        findViewById(R.id.btn_align_left).setOnClickListener(v -> applyAlignment(Layout.Alignment.ALIGN_NORMAL));
        findViewById(R.id.btn_align_center).setOnClickListener(v -> applyAlignment(Layout.Alignment.ALIGN_CENTER));
        findViewById(R.id.btn_align_right).setOnClickListener(v -> applyAlignment(Layout.Alignment.ALIGN_OPPOSITE));

        // List Buttons
        findViewById(R.id.btn_list).setOnClickListener(v -> insertListSymbol("• "));
        findViewById(R.id.btn_list_ordered).setOnClickListener(v -> insertListSymbol("1. "));

        // Insert Image Button
        findViewById(R.id.btn_insert_image).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 1001);
        });
    }

    private void updateButtonState(View view, boolean isActive) {
        view.setBackgroundColor(isActive ? getResources().getColor(R.color.blue_100, null) : android.graphics.Color.TRANSPARENT);
    }

    private void setupTextWatcher() {
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > before) { // New text added
                    int end = start + count;
                    Editable text = editor.getText();
                    if (isBold) text.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (isItalic) text.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (isUnderline) text.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void applyStyleToSelection(int style, boolean active) {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (start < end) {
            Editable text = editor.getText();
            if (active) {
                text.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                // Remove matching StyleSpans in the selection range
                StyleSpan[] spans = text.getSpans(start, end, StyleSpan.class);
                for (StyleSpan span : spans) {
                    if (span.getStyle() == style) {
                        int spanStart = text.getSpanStart(span);
                        int spanEnd = text.getSpanEnd(span);
                        text.removeSpan(span);
                        // Re-apply span to portions outside the selection
                        if (spanStart < start) {
                            text.setSpan(new StyleSpan(style), spanStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        if (spanEnd > end) {
                            text.setSpan(new StyleSpan(style), end, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            }
        }
    }

    private void applyUnderlineToSelection(boolean active) {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (start < end) {
            Editable text = editor.getText();
            if (active) {
                text.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                // Remove UnderlineSpans in the selection range
                UnderlineSpan[] spans = text.getSpans(start, end, UnderlineSpan.class);
                for (UnderlineSpan span : spans) {
                    int spanStart = text.getSpanStart(span);
                    int spanEnd = text.getSpanEnd(span);
                    text.removeSpan(span);
                    // Re-apply span to portions outside the selection
                    if (spanStart < start) {
                        text.setSpan(new UnderlineSpan(), spanStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (spanEnd > end) {
                        text.setSpan(new UnderlineSpan(), end, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
    }

    private void applyAlignment(Layout.Alignment alignment) {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        editor.getText().setSpan(new AlignmentSpan.Standard(alignment), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void insertListSymbol(String symbol) {
        int cursor = editor.getSelectionStart();
        editor.getText().insert(cursor, "\n" + symbol);
    }

    private void shareDocumentContent() {
        try {
            // Ensure local file is saved with latest content
            ensureLocalPath();
            if (currentDocument.getLocalPath() != null && !currentDocument.getLocalPath().isEmpty()) {
                documentService.saveTextToLocalFile(currentDocument.getLocalPath(), editor.getText().toString());
                File fileToShare = new File(currentDocument.getLocalPath());
                if (fileToShare.exists()) {
                    Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", fileToShare);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    String mimeType = getMimeTypeForFile(currentDocument.getFileName());
                    intent.setType(mimeType);
                    intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Share Document"));
                    return;
                }
            }
            // Fallback: share as plain text if file operations fail
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, editor.getText().toString());
            startActivity(Intent.createChooser(intent, "Share Document"));
        } catch (Exception e) {
            Log.e("DocumentEditor", "Failed to share file, falling back to text", e);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, editor.getText().toString());
            startActivity(Intent.createChooser(intent, "Share Document"));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            insertImageAtCursor(imageUri);
        }
    }

    private void insertImageAtCursor(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            
            // Resize bitmap to fit screen width roughly
            int width = editor.getWidth() - editor.getPaddingLeft() - editor.getPaddingRight();
            if (width <= 0) width = 800;
            float ratio = (float) bitmap.getHeight() / bitmap.getWidth();
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, (int)(width * ratio), true);

            ImageSpan span = new ImageSpan(this, scaled);
            int start = editor.getSelectionStart();
            
            // We use a dummy space character to hold the image span
            editor.getText().insert(start, "\n "); 
            editor.getText().setSpan(span, start + 1, start + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            editor.getText().insert(start + 2, "\n");
            
            Toast.makeText(this, "Image inserted successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("DocumentEditor", "Failed to insert image", e);
            Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
        }
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

        // Ensure we always have a file name
        if (currentDocument.getFileName() == null || currentDocument.getFileName().isEmpty()) {
            currentDocument.setFileName("Document.docx");
        }
        tvTitle.setText(currentDocument.getFileName());

        // Ensure we always have a valid local path
        ensureLocalPath();

        String content = documentService.readTextFromLocalFile(this, currentDocument.getLocalPath());
        editor.setText(content == null || content.isEmpty() ? "" : content);
    }

    /**
     * Ensures that currentDocument has a valid local file path.
     * If localPath is null/empty or the file doesn't exist, creates a new local file
     * in the app's documents directory.
     */
    private void ensureLocalPath() {
        if (currentDocument == null) return;

        String localPath = currentDocument.getLocalPath();
        if (localPath != null && !localPath.isEmpty() && new File(localPath).exists()) {
            return; // Local path is valid and file exists
        }

        // Create a local file in the documents directory
        String fileName = currentDocument.getFileName();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "Document.docx";
            currentDocument.setFileName(fileName);
        }

        File docDir = new File(getFilesDir(), "documents");
        if (!docDir.exists()) {
            docDir.mkdirs();
        }
        File localFile = new File(docDir, fileName);

        // If file doesn't exist yet, create an empty one
        if (!localFile.exists()) {
            try {
                localFile.createNewFile();
            } catch (Exception e) {
                Log.e("DocumentEditor", "Failed to create local file", e);
            }
        }

        currentDocument.setLocalPath(localFile.getAbsolutePath());
        Log.d("DocumentEditor", "Local path set to: " + localFile.getAbsolutePath());
    }

    private void saveDocument() {
        if (currentDocument == null) {
            Toast.makeText(this, "No document to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Auto-create local path if missing
        ensureLocalPath();

        if (currentDocument.getLocalPath() == null || currentDocument.getLocalPath().isEmpty()) {
            Toast.makeText(this, "Could not create document file.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!documentService.saveTextToLocalFile(currentDocument.getLocalPath(), editor.getText().toString())) {
            Toast.makeText(this, "Could not save local file.", Toast.LENGTH_SHORT).show();
            return;
        }

        documentRepository.saveDocument(currentDocument, new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB documentFB) {
                currentDocument = documentFB;
                Toast.makeText(DocumentEditorActivity.this, "Document saved successfully!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onProgress(int progressPercentage) {}
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DocumentEditorActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAIBottomSheet() {
        captureAiSelection();
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ai_assistant, null);
        dialog.setContentView(sheetView);

        LinearLayout suggestionArea = sheetView.findViewById(R.id.ai_suggestion_area);
        TextView tvSuggestion = sheetView.findViewById(R.id.tv_ai_suggestion);
        TextView tvContextInfo = sheetView.findViewById(R.id.tv_ai_context_info);
        TextView tvProgress = sheetView.findViewById(R.id.tv_ai_progress);
        
        String contextText = getSelectedOrAllText();
        tvContextInfo.setText(buildAiContextInfo(contextText));
        tvProgress.setText(buildAiProgressText(contextText, splitContentForAi(contextText, MAX_AI_CHARS_PER_REQUEST), 0, false));

        sheetView.findViewById(R.id.btn_open_chat).setOnClickListener(v -> {
            dialog.dismiss();
            openChatBottomSheet();
        });

        sheetView.findViewById(R.id.btn_ai_improve).setOnClickListener(v ->
                callAiAction("Improve writing", contextText, suggestionArea, tvSuggestion, tvProgress));
        sheetView.findViewById(R.id.btn_ai_fix).setOnClickListener(v ->
                callAiAction("Fix grammar", contextText, suggestionArea, tvSuggestion, tvProgress));
        sheetView.findViewById(R.id.btn_ai_shorten).setOnClickListener(v ->
                callAiAction("Make shorter", contextText, suggestionArea, tvSuggestion, tvProgress));
        sheetView.findViewById(R.id.btn_ai_expand).setOnClickListener(v ->
                callAiAction("Make longer", contextText, suggestionArea, tvSuggestion, tvProgress));
        sheetView.findViewById(R.id.btn_ai_translate).setOnClickListener(v ->
                callAiAction("Translate to Vietnamese", contextText, suggestionArea, tvSuggestion, tvProgress));

        sheetView.findViewById(R.id.btn_ai_edit_image).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ImageFilterActivity.class));
        });

        sheetView.findViewById(R.id.btn_apply_suggestion).setOnClickListener(v -> {
            applyTextToEditor(tvSuggestion.getText().toString(), aiSelectionStart, aiSelectionEnd);
            dialog.dismiss();
        });

        sheetView.findViewById(R.id.btn_dismiss_suggestion).setOnClickListener(v -> suggestionArea.setVisibility(View.GONE));
        dialog.show();
    }

    private String getSelectedOrAllText() {
        int start = getSafeSelectionStart();
        int end = getSafeSelectionEnd();
        if (start != end && start >= 0 && end > start) return editor.getText().toString().substring(start, end);
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

    private void applyTextToEditor(String newText, int start, int end) {
        if (newText == null) {
            return;
        }

        Editable text = editor.getText();
        int textLength = text.length();
        int safeStart = Math.max(0, Math.min(start, textLength));
        int safeEnd = Math.max(0, Math.min(end, textLength));

        if (safeStart < safeEnd) {
            text.replace(safeStart, safeEnd, newText);
            editor.setSelection(Math.min(safeStart + newText.length(), editor.getText().length()));
            return;
        }

        applyTextToEditor(newText);
    }

    private void captureAiSelection() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (start >= 0 && end >= 0 && start != end) {
            aiSelectionStart = Math.min(start, end);
            aiSelectionEnd = Math.max(start, end);
            return;
        }

        aiSelectionStart = -1;
        aiSelectionEnd = -1;
    }

    private int getSafeSelectionStart() {
        if (aiSelectionStart >= 0 && aiSelectionEnd > aiSelectionStart) {
            return aiSelectionStart;
        }
        return editor.getSelectionStart();
    }

    private int getSafeSelectionEnd() {
        if (aiSelectionStart >= 0 && aiSelectionEnd > aiSelectionStart) {
            return aiSelectionEnd;
        }
        return editor.getSelectionEnd();
    }

    private void callAiAction(String prompt, String content, LinearLayout area, TextView resultView,
                              TextView progressView) {
        if (content.trim().isEmpty()) return;
        resultView.setText("AI is working...");
        area.setVisibility(View.VISIBLE);
        String effectivePrompt = buildSingleAnswerPrompt(prompt, content);
        List<String> chunks = splitContentForAi(content, MAX_AI_CHARS_PER_REQUEST);
        progressView.setText(buildAiProgressText(content, chunks, 0, true));
        processAiChunks(effectivePrompt, chunks, 0, new StringBuilder(), resultView, progressView, content);
    }

    private void processAiChunks(String prompt, List<String> chunks, int index, StringBuilder combinedResult,
                                 TextView resultView, TextView progressView, String originalContent) {
        if (index >= chunks.size()) {
            runOnUiThread(() -> {
                String finalResult = sanitizeAiRewriteResult(combinedResult.toString());
                if (finalResult.isEmpty()) {
                    progressView.setText("Progress: failed - empty AI response");
                    resultView.setText("Error: AI returned an empty response.");
                    return;
                }
                progressView.setText(buildAiCompletionText(originalContent, chunks));
                resultView.setText(finalResult);
            });
            return;
        }

        String progressMessage = chunks.size() > 1
                ? "AI is working... (" + (index + 1) + "/" + chunks.size() + ")"
                : "AI is working...";
        runOnUiThread(() -> {
            resultView.setText(progressMessage);
            progressView.setText(buildAiProgressText(chunks.get(index), chunks, index, true));
        });

        geminiService.processText(prompt, chunks.get(index), new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                if (combinedResult.length() > 0) {
                    combinedResult.append("\n\n");
                }
                combinedResult.append(result.trim());
                processAiChunks(prompt, chunks, index + 1, combinedResult, resultView, progressView,
                        originalContent);
            }

            @Override
            public void onTokenUsage(int totalTokenCount) {}

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressView.setText("Progress: failed at chunk " + (index + 1) + "/" + chunks.size()
                            + " - " + error);
                    resultView.setText("Error: " + error);
                });
            }
        });
    }

    private String buildAiContextInfo(String contextText) {
        boolean hasSelection = aiSelectionStart >= 0 && aiSelectionEnd > aiSelectionStart;
        String source = hasSelection ? "selected text" : "full document";
        return "Context: using " + source + " (" + contextText.length() + " chars) - \""
                + buildPreviewText(contextText) + "\"";
    }

    private String buildAiProgressText(String currentChunk, List<String> chunks, int index, boolean running) {
        if (chunks == null || chunks.isEmpty()) {
            return "Progress: no content";
        }

        if (!running) {
            return "Progress: ready - " + chunks.size() + " chunk(s), first chunk " + chunks.get(0).length()
                    + " chars";
        }

        int safeIndex = Math.max(0, Math.min(index, chunks.size() - 1));
        return String.format(Locale.US, "Progress: chunk %d/%d - %d chars - \"%s\"",
                safeIndex + 1, chunks.size(), currentChunk.length(), buildPreviewText(currentChunk));
    }

    private String buildAiCompletionText(String originalContent, List<String> chunks) {
        return "Progress: done - processed " + originalContent.length() + " chars in "
                + chunks.size() + " chunk(s)";
    }

    private String buildSingleAnswerPrompt(String action, String content) {
        boolean hasSelection = aiSelectionStart >= 0 && aiSelectionEnd > aiSelectionStart;
        String scope = hasSelection ? "selected text" : "document text";
        return "You are editing " + scope + ". Task: " + action + ". "
                + "Return exactly one final rewritten version only. "
                + "Do not provide multiple options. "
                + "Do not explain. "
                + "Do not use bullet points, numbering, headings, markdown, labels, or quotation marks. "
                + "Keep the response concise and ready to paste directly into the document. "
                + "If the input is a single sentence, return a single sentence only.";
    }

    private String sanitizeAiRewriteResult(String rawResult) {
        if (rawResult == null) {
            return "";
        }

        String trimmed = rawResult.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        String[] lines = trimmed.split("\\r?\\n");
        for (String line : lines) {
            String candidate = line.trim();
            if (candidate.isEmpty()) {
                continue;
            }

            if (candidate.startsWith("#") || candidate.startsWith("*")) {
                continue;
            }

            candidate = candidate.replaceFirst("^\\d+[.)]\\s*", "");
            candidate = candidate.replaceFirst("^[-*•]\\s*", "");

            if (!candidate.isEmpty()) {
                return stripWrappingQuotes(candidate);
            }
        }

        return stripWrappingQuotes(trimmed);
    }

    private String stripWrappingQuotes(String text) {
        String normalized = text == null ? "" : text.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            return normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private String buildPreviewText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text.replace("\n", "\\n").trim();
        if (normalized.length() <= 40) {
            return normalized;
        }
        return normalized.substring(0, 37) + "...";
    }

    private List<String> splitContentForAi(String content, int maxCharsPerChunk) {
        List<String> chunks = new ArrayList<>();
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            return chunks;
        }

        if (normalizedContent.length() <= maxCharsPerChunk) {
            chunks.add(normalizedContent);
            return chunks;
        }

        int start = 0;
        while (start < normalizedContent.length()) {
            int end = Math.min(start + maxCharsPerChunk, normalizedContent.length());
            if (end < normalizedContent.length()) {
                int paragraphBreak = normalizedContent.lastIndexOf("\n\n", end);
                if (paragraphBreak > start + (maxCharsPerChunk / 2)) {
                    end = paragraphBreak;
                } else {
                    int lineBreak = normalizedContent.lastIndexOf('\n', end);
                    if (lineBreak > start + (maxCharsPerChunk / 2)) {
                        end = lineBreak;
                    } else {
                        int spaceBreak = normalizedContent.lastIndexOf(' ', end);
                        if (spaceBreak > start + (maxCharsPerChunk / 2)) {
                            end = spaceBreak;
                        }
                    }
                }
            }

            String chunk = normalizedContent.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = end;
            while (start < normalizedContent.length() && Character.isWhitespace(normalizedContent.charAt(start))) {
                start++;
            }
        }

        return chunks;
    }

    private void openChatBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View sheetView = getLayoutInflater().inflate(R.layout.fragment_chat, null);
        dialog.setContentView(sheetView);

        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);

        RecyclerView rvMessages = sheetView.findViewById(R.id.rv_messages);
        EditText etInput = sheetView.findViewById(R.id.et_input);
        ImageButton btnSend = sheetView.findViewById(R.id.btn_send);

        List<ChatMessage> messageList = new ArrayList<>();
        messageList.add(new ChatMessage("assistant", "How can I help?"));
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
            geminiService.processText("Chat", input, new GeminiService.GeminiCallback() {
                @Override public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        messageList.add(new ChatMessage("assistant", result));
                        adapter.notifyItemInserted(messageList.size() - 1);
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    });
                }
                @Override public void onTokenUsage(int t) {}
                @Override public void onError(String e) {}
            });
        });
        dialog.show();
    }

    /**
     * Export the current editor content as a PDF file, then open a share/save chooser.
     * Uses Android's PdfDocument API with StaticLayout for proper text wrapping and pagination.
     */
    private void exportAsPdf() {
        String content = editor.getText().toString();
        if (content.trim().isEmpty()) {
            Toast.makeText(this, "Nothing to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // PDF page dimensions (A4 in points: 595 x 842)
            int pageWidth = 595;
            int pageHeight = 842;
            int marginLeft = 50;
            int marginTop = 50;
            int marginRight = 50;
            int marginBottom = 50;
            int usableWidth = pageWidth - marginLeft - marginRight;
            int usableHeight = pageHeight - marginTop - marginBottom;

            // Set up text paint for the PDF
            TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(12f);
            textPaint.setColor(android.graphics.Color.BLACK);

            // Build a StaticLayout to measure and draw text with wrapping
            StaticLayout fullLayout;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                fullLayout = StaticLayout.Builder.obtain(content, 0, content.length(), textPaint, usableWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(4f, 1.0f)
                        .setIncludePad(true)
                        .build();
            } else {
                fullLayout = new StaticLayout(content, textPaint, usableWidth,
                        Layout.Alignment.ALIGN_NORMAL, 1.0f, 4f, true);
            }

            int totalHeight = fullLayout.getHeight();
            PdfDocument pdfDocument = new PdfDocument();
            int yOffset = 0;
            int pageNumber = 1;

            while (yOffset < totalHeight) {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                canvas.save();
                canvas.translate(marginLeft, marginTop - yOffset);
                // Clip to usable area to prevent overflow
                canvas.clipRect(0, yOffset, usableWidth, yOffset + usableHeight);
                fullLayout.draw(canvas);
                canvas.restore();

                pdfDocument.finishPage(page);
                yOffset += usableHeight;
                pageNumber++;
            }

            // Determine output file name
            String docName = "Document";
            if (currentDocument != null && currentDocument.getFileName() != null && !currentDocument.getFileName().isEmpty()) {
                docName = currentDocument.getFileName().replaceAll("\\.[^.]+$", "");
            }

            // Save PDF to internal cache for sharing
            File pdfDir = new File(getFilesDir(), "scanned_pdfs");
            if (!pdfDir.exists()) {
                pdfDir.mkdirs();
            }
            File pdfFile = new File(pdfDir, docName + ".pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            fos.close();
            pdfDocument.close();

            // Share the PDF file via a chooser
            Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Export PDF"));

            Toast.makeText(this, "PDF exported successfully!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("DocumentEditor", "Failed to export PDF", e);
            Toast.makeText(this, "Failed to export PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Download/copy the current document file to the device's public Downloads folder
     * so the user can find it in their file manager.
     * Uses MediaStore for Android 10+ and direct file copy for older versions.
     */
    private void downloadToStorage() {
        // First, make sure local file is saved with latest content
        if (currentDocument == null || currentDocument.getLocalPath() == null || currentDocument.getLocalPath().isEmpty()) {
            Toast.makeText(this, "Document path is unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save current editor content to local file first
        if (!documentService.saveTextToLocalFile(currentDocument.getLocalPath(), editor.getText().toString())) {
            Toast.makeText(this, "Could not save local file.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = currentDocument.getFileName();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "Document.txt";
        }

        File sourceFile = new File(currentDocument.getLocalPath());
        if (!sourceFile.exists()) {
            Toast.makeText(this, "Source file not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ : use MediaStore to write to Downloads
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, getMimeTypeForFile(fileName));
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri downloadUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (downloadUri == null) {
                    Toast.makeText(this, "Failed to create file in Downloads.", Toast.LENGTH_SHORT).show();
                    return;
                }

                OutputStream outputStream = getContentResolver().openOutputStream(downloadUri);
                FileInputStream inputStream = new FileInputStream(sourceFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();

                Toast.makeText(this, "Downloaded to Downloads/" + fileName, Toast.LENGTH_LONG).show();
            } else {
                // Android 9 and below: copy directly to Downloads directory
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                File destFile = new File(downloadsDir, fileName);

                FileInputStream inputStream = new FileInputStream(sourceFile);
                FileOutputStream outputStream = new FileOutputStream(destFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();

                Toast.makeText(this, "Downloaded to Downloads/" + fileName, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("DocumentEditor", "Failed to download to storage", e);
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Returns a MIME type string based on the file extension.
     */
    private String getMimeTypeForFile(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
}

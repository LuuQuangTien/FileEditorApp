package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.TranslateService;

public class TranslateActivity extends AppCompatActivity {

    private Spinner spinnerSource, spinnerTarget;
    private EditText etSourceText;
    private TextView tvCharCount, tvTranslatedText;
    private MaterialButton btnTranslate, btnDownload;
    private MaterialCardView cardTranslationResult;
    private ProgressBar progressTranslate;
    private ImageView btnCopyTranslation, btnClearSource;
    private FrameLayout btnSwapLanguages;

    private TranslateService translateService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        translateService = new TranslateService();

        initViews();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        // Header
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Language selection
        spinnerSource = findViewById(R.id.spinner_source);
        spinnerTarget = findViewById(R.id.spinner_target);
        btnSwapLanguages = findViewById(R.id.btn_swap_languages);

        // Source text
        etSourceText = findViewById(R.id.et_source_text);
        tvCharCount = findViewById(R.id.tv_char_count);
        btnClearSource = findViewById(R.id.btn_clear_source);

        // Translate button & progress
        btnTranslate = findViewById(R.id.btn_translate);
        progressTranslate = findViewById(R.id.progress_translate);

        // Translation result
        cardTranslationResult = findViewById(R.id.card_translation_result);
        tvTranslatedText = findViewById(R.id.tv_translated_text);
        btnCopyTranslation = findViewById(R.id.btn_copy_translation);
        btnDownload = findViewById(R.id.btn_download_translation);
    }

    private void setupSpinners() {
        String[] languages = translateService.getLanguageNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (spinnerSource != null) {
            spinnerSource.setAdapter(adapter);
            spinnerSource.setSelection(0); // English
        }
        if (spinnerTarget != null) {
            spinnerTarget.setAdapter(adapter);
            spinnerTarget.setSelection(1); // Vietnamese
        }
    }

    private void setupListeners() {
        // Character counter + show/hide clear button
        if (etSourceText != null) {
            etSourceText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    int length = s.length();
                    if (tvCharCount != null) {
                        tvCharCount.setText(length + (length == 1 ? " character" : " characters"));
                    }
                    if (btnClearSource != null) {
                        btnClearSource.setVisibility(length > 0 ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Clear source
        if (btnClearSource != null) {
            btnClearSource.setOnClickListener(v -> {
                etSourceText.setText("");
                cardTranslationResult.setVisibility(View.GONE);
            });
        }

        // Swap languages
        if (btnSwapLanguages != null) {
            btnSwapLanguages.setOnClickListener(v -> {
                if (spinnerSource != null && spinnerTarget != null) {
                    int sourcePos = spinnerSource.getSelectedItemPosition();
                    int targetPos = spinnerTarget.getSelectedItemPosition();
                    spinnerSource.setSelection(targetPos);
                    spinnerTarget.setSelection(sourcePos);
                }
            });
        }

        // Translate
        if (btnTranslate != null) {
            btnTranslate.setOnClickListener(v -> performTranslation());
        }

        // Copy translation
        if (btnCopyTranslation != null) {
            btnCopyTranslation.setOnClickListener(v -> {
                if (tvTranslatedText != null) {
                    String text = tvTranslatedText.getText().toString();
                    if (!text.isEmpty()) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Translation", text);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Đã sao chép bản dịch!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Download translation as .txt
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> downloadTranslation());
        }
    }

    private void performTranslation() {
        String sourceText = etSourceText != null ? etSourceText.getText().toString().trim() : "";
        if (sourceText.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập văn bản cần dịch", Toast.LENGTH_SHORT).show();
            return;
        }

        String sourceLang = spinnerSource != null ? spinnerSource.getSelectedItem().toString() : "English";
        String targetLang = spinnerTarget != null ? spinnerTarget.getSelectedItem().toString() : "Vietnamese";

        // Disable button, show progress
        setTranslating(true);

        translateService.translate(sourceText, sourceLang, targetLang, new TranslateService.TranslateCallback() {
            @Override
            public void onSuccess(String translatedText) {
                runOnUiThread(() -> {
                    setTranslating(false);
                    if (tvTranslatedText != null) {
                        tvTranslatedText.setText(translatedText);
                    }
                    if (cardTranslationResult != null) {
                        cardTranslationResult.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    setTranslating(false);
                    Toast.makeText(TranslateActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onModelDownloading() {
                runOnUiThread(() ->
                        Toast.makeText(TranslateActivity.this,
                                "Đang tải model ngôn ngữ...", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setTranslating(boolean translating) {
        if (btnTranslate != null) {
            btnTranslate.setEnabled(!translating);
            btnTranslate.setText(translating ? "Translating..." : "Translate");
        }
        if (progressTranslate != null) {
            progressTranslate.setVisibility(translating ? View.VISIBLE : View.GONE);
        }
    }

    private void downloadTranslation() {
        if (tvTranslatedText == null) return;
        String text = tvTranslatedText.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "Không có bản dịch để tải xuống", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Save to app-specific Documents directory
            File dir = new File(getFilesDir(), "translations");
            if (!dir.exists()) dir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "translation_" + timestamp + ".txt";
            File file = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(text.getBytes(StandardCharsets.UTF_8));
            fos.close();

            Toast.makeText(this, "Đã lưu: " + fileName, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi lưu file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translateService != null) {
            translateService.closeTranslator();
        }
    }
}

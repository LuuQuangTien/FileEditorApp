package hcmute.edu.vn.documentfileeditor.Activity;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.ScanService;
import hcmute.edu.vn.documentfileeditor.Service.TranslateService;

public class OcrActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 101;

    // Input section
    private LinearLayout ocrInputSection;
    private MaterialCardView cardTakePhoto, cardUploadGallery;

    // Result section
    private LinearLayout ocrResultSection;
    private MaterialCardView cardProcessing, cardFileInfo;
    private TextView tvFileName, tvFileSize;
    private EditText etExtractedText;
    private MaterialButton btnCopy, btnTranslateOcr, btnScanAgain;

    // Translate section
    private Spinner spinnerSourceOcr, spinnerTargetOcr;
    private FrameLayout btnSwapLanguagesOcr;
    private ProgressBar progressTranslateOcr;

    // Services
    private ScanService scanService;
    private TranslateService translateService;

    // State
    private Uri currentPhotoUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        scanService = new ScanService();
        translateService = new TranslateService();

        initViews();
        setupLaunchers();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        // Back
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Input section
        ocrInputSection = findViewById(R.id.ocr_input_section);
        cardTakePhoto = findViewById(R.id.card_take_photo);
        cardUploadGallery = findViewById(R.id.card_upload_gallery);

        // Result section
        ocrResultSection = findViewById(R.id.ocr_result_section);
        cardProcessing = findViewById(R.id.card_processing);
        cardFileInfo = findViewById(R.id.card_file_info);
        tvFileName = findViewById(R.id.tv_file_name_ocr);
        tvFileSize = findViewById(R.id.tv_file_size_ocr);
        etExtractedText = findViewById(R.id.et_extracted_text);

        // Buttons
        btnCopy = findViewById(R.id.btn_copy);
        btnTranslateOcr = findViewById(R.id.btn_translate_ocr);
        btnScanAgain = findViewById(R.id.btn_scan_again);

        // Translate
        spinnerSourceOcr = findViewById(R.id.spinner_source_ocr);
        spinnerTargetOcr = findViewById(R.id.spinner_target_ocr);
        btnSwapLanguagesOcr = findViewById(R.id.btn_swap_languages_ocr);
        progressTranslateOcr = findViewById(R.id.progress_translate_ocr);
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && currentPhotoUri != null) {
                        processImage(currentPhotoUri);
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        currentPhotoUri = uri;
                        processImage(uri);
                    }
                }
        );
    }

    private void setupSpinners() {
        String[] languages = translateService.getLanguageNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (spinnerSourceOcr != null) {
            spinnerSourceOcr.setAdapter(adapter);
            spinnerSourceOcr.setSelection(0); // English
        }
        if (spinnerTargetOcr != null) {
            spinnerTargetOcr.setAdapter(adapter);
            spinnerTargetOcr.setSelection(1); // Vietnamese
        }
    }

    private void setupListeners() {
        // Take Photo
        if (cardTakePhoto != null) {
            cardTakePhoto.setOnClickListener(v -> checkCameraPermissionAndCapture());
        }

        // Upload from Gallery
        if (cardUploadGallery != null) {
            cardUploadGallery.setOnClickListener(v -> {
                galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            });
        }

        // Copy
        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                String text = etExtractedText != null ? etExtractedText.getText().toString() : "";
                if (!text.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Extracted Text", text);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Đã sao chép văn bản!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Translate
        if (btnTranslateOcr != null) {
            btnTranslateOcr.setOnClickListener(v -> performTranslation());
        }

        // Swap languages
        if (btnSwapLanguagesOcr != null) {
            btnSwapLanguagesOcr.setOnClickListener(v -> {
                if (spinnerSourceOcr != null && spinnerTargetOcr != null) {
                    int sourcePos = spinnerSourceOcr.getSelectedItemPosition();
                    int targetPos = spinnerTargetOcr.getSelectedItemPosition();
                    spinnerSourceOcr.setSelection(targetPos);
                    spinnerTargetOcr.setSelection(sourcePos);
                }
            });
        }

        // Scan again
        if (btnScanAgain != null) {
            btnScanAgain.setOnClickListener(v -> resetToInput());
        }
    }

    private void checkCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Cần quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = createImageFile();
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                cameraLauncher.launch(takePictureIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String imageFileName = "OCR_" + timeStamp;
            File storageDir = new File(getFilesDir(), "scanned_images");
            if (!storageDir.exists()) storageDir.mkdirs();
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi tạo file ảnh", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void processImage(Uri imageUri) {
        // Show result section with processing
        showResultSection();
        if (cardProcessing != null) cardProcessing.setVisibility(View.VISIBLE);
        if (tvFileName != null) tvFileName.setText(imageUri.getLastPathSegment());
        if (tvFileSize != null) tvFileSize.setText("Đang xử lý...");
        if (etExtractedText != null) etExtractedText.setText("");

        scanService.extractText(this, imageUri, ScanService.SCRIPT_LATIN, new ScanService.OcrCallback() {
            @Override
            public void onSuccess(String extractedText) {
                runOnUiThread(() -> {
                    if (cardProcessing != null) cardProcessing.setVisibility(View.GONE);
                    if (etExtractedText != null) etExtractedText.setText(extractedText);
                    if (tvFileSize != null) tvFileSize.setText("Hoàn thành");
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    if (cardProcessing != null) cardProcessing.setVisibility(View.GONE);
                    if (tvFileSize != null) tvFileSize.setText("Lỗi");
                    Toast.makeText(OcrActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void performTranslation() {
        String sourceText = etExtractedText != null ? etExtractedText.getText().toString().trim() : "";
        if (sourceText.isEmpty()) {
            Toast.makeText(this, "Vui lòng quét văn bản trước khi dịch!", Toast.LENGTH_SHORT).show();
            return;
        }

        String sourceLang = spinnerSourceOcr != null ? spinnerSourceOcr.getSelectedItem().toString() : "English";
        String targetLang = spinnerTargetOcr != null ? spinnerTargetOcr.getSelectedItem().toString() : "Vietnamese";

        setTranslating(true);

        translateService.translate(sourceText, sourceLang, targetLang, new TranslateService.TranslateCallback() {
            @Override
            public void onSuccess(String translatedText) {
                runOnUiThread(() -> {
                    setTranslating(false);
                    if (etExtractedText != null) etExtractedText.setText(translatedText);
                    Toast.makeText(OcrActivity.this, "Dịch thành công!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    setTranslating(false);
                    Toast.makeText(OcrActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onModelDownloading() {
                runOnUiThread(() ->
                        Toast.makeText(OcrActivity.this, "Đang tải model ngôn ngữ...", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setTranslating(boolean translating) {
        if (btnTranslateOcr != null) {
            btnTranslateOcr.setEnabled(!translating);
            btnTranslateOcr.setText(translating ? "Translating..." : "Translate");
        }
        if (progressTranslateOcr != null) {
            progressTranslateOcr.setVisibility(translating ? View.VISIBLE : View.GONE);
        }
    }

    private void showResultSection() {
        if (ocrInputSection != null) ocrInputSection.setVisibility(View.GONE);
        if (ocrResultSection != null) ocrResultSection.setVisibility(View.VISIBLE);
    }

    private void resetToInput() {
        if (ocrInputSection != null) ocrInputSection.setVisibility(View.VISIBLE);
        if (ocrResultSection != null) ocrResultSection.setVisibility(View.GONE);
        if (etExtractedText != null) etExtractedText.setText("");
        currentPhotoUri = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanService != null) scanService.close();
        if (translateService != null) translateService.closeTranslator();
    }
}

package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.AuthService;
import hcmute.edu.vn.documentfileeditor.Service.ScanService;

public class ScanActivity extends AppCompatActivity {

    // Scan options
    private LinearLayout scanOptionsSection;
    private MaterialCardView cardUploadGallery;
    private MaterialCardView cardUploadDocument;

    // Scan result
    private LinearLayout scanResultSection;
    private MaterialCardView cardProcessing;
    private ImageView ivScannedImage;
    private TextView tvScanStatus;
    private MaterialButton btnSavePdf, btnRetake;

    private ScanService scanService;
    private hcmute.edu.vn.documentfileeditor.Service.CloudConvertService cloudConvertService;
    private DocumentRepository documentRepository;
    private AuthService authService;
    private Bitmap currentBitmap;

    private ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;
    private ActivityResultLauncher<Intent> docLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        scanService = new ScanService();
        cloudConvertService = new hcmute.edu.vn.documentfileeditor.Service.CloudConvertService();
        documentRepository = DocumentRepository.getInstance(this);
        authService = new AuthService();

        initViews();
        setupLaunchers();
        setupListeners();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Scan options
        scanOptionsSection = findViewById(R.id.scan_options_section);
        cardUploadGallery = findViewById(R.id.card_upload_gallery);
        cardUploadDocument = findViewById(R.id.card_upload_document);

        // Scan result
        scanResultSection = findViewById(R.id.scan_result_section);
        cardProcessing = findViewById(R.id.card_processing);
        ivScannedImage = findViewById(R.id.iv_scanned_image);
        tvScanStatus = findViewById(R.id.tv_scan_status);
        btnSavePdf = findViewById(R.id.btn_save_pdf);
        btnRetake = findViewById(R.id.btn_retake);
    }

    private void setupLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        showScannedImage(uri);
                    }
                }
        );

        docLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri docUri = result.getData().getData();
                        if (docUri != null) {
                            startCloudConvert(docUri);
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        if (cardUploadGallery != null) {
            cardUploadGallery.setOnClickListener(v -> {
                galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            });
        }

        if (cardUploadDocument != null) {
            cardUploadDocument.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                String[] mimeTypes = {
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "text/plain",
                        "application/pdf"
                };
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                docLauncher.launch(intent);
            });
        }

        if (btnSavePdf != null) {
            btnSavePdf.setOnClickListener(v -> saveAsPdf());
        }

        if (btnRetake != null) {
            btnRetake.setOnClickListener(v -> resetToScanOptions());
        }
    }

    private void showScannedImage(Uri imageUri) {
        // Switch to result view
        if (scanOptionsSection != null) scanOptionsSection.setVisibility(View.GONE);
        if (scanResultSection != null) scanResultSection.setVisibility(View.VISIBLE);
        findViewById(R.id.card_preview).setVisibility(View.VISIBLE);

        // Load bitmap and show preview
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            currentBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (ivScannedImage != null && currentBitmap != null) {
                ivScannedImage.setImageBitmap(currentBitmap);
            }
            if (tvScanStatus != null) tvScanStatus.setText("Ready to save");
            if (btnSavePdf != null) {
                btnSavePdf.setVisibility(View.VISIBLE);
                btnSavePdf.setEnabled(true);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCloudConvert(Uri docUri) {
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để lưu file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (scanOptionsSection != null) scanOptionsSection.setVisibility(View.GONE);
        if (scanResultSection != null) scanResultSection.setVisibility(View.VISIBLE);
        
        findViewById(R.id.card_preview).setVisibility(View.GONE); // Hide image preview for docs
        if (cardProcessing != null) cardProcessing.setVisibility(View.VISIBLE);
        if (btnSavePdf != null) btnSavePdf.setVisibility(View.GONE); // No manual save needed
        if (tvScanStatus != null) tvScanStatus.setText("Đang chuẩn bị...");

        String originalName = new hcmute.edu.vn.documentfileeditor.Service.DocumentService().resolveFileName(this, docUri);
        String baseName = originalName.lastIndexOf(".") > 0 ? originalName.substring(0, originalName.lastIndexOf(".")) : originalName;

        cloudConvertService.convertDocumentToPdf(this, docUri, baseName, new hcmute.edu.vn.documentfileeditor.Service.CloudConvertService.CloudConvertCallback() {
            @Override
            public void onProgress(String message) {
                if (tvScanStatus != null) tvScanStatus.setText(message);
            }

            @Override
            public void onSuccess(String pdfLocalPath) {
                uploadToRepository(pdfLocalPath, baseName + ".pdf", userId);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ScanActivity.this, error, Toast.LENGTH_LONG).show();
                resetToScanOptions();
            }
        });
    }

    private void saveAsPdf() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Không có ảnh để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = authService.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để lưu file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cardProcessing != null) cardProcessing.setVisibility(View.VISIBLE);
        if (btnSavePdf != null) btnSavePdf.setEnabled(false);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Scan_" + timestamp;

        // 1. Tạo PDF qua ScanService
        scanService.saveAsPdf(this, currentBitmap, fileName, new ScanService.SaveCallback() {
            @Override
            public void onSuccess(String filePath) {
                // 2. Đưa PDF vào hệ sinh thái Database của app (Firebase/Room)
                uploadToRepository(filePath, fileName + ".pdf", userId);
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    if (cardProcessing != null) cardProcessing.setVisibility(View.GONE);
                    if (btnSavePdf != null) btnSavePdf.setEnabled(true);
                    Toast.makeText(ScanActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void uploadToRepository(String absolutePath, String fileName, String userId) {
        File pdfFile = new File(absolutePath);
        Uri fileUri = Uri.fromFile(pdfFile);

        DocumentFB document = new DocumentFB();
        document.setUserId(userId);
        document.setFileName(fileName);
        document.setFileType(FileType.PDF);

        documentRepository.uploadDocument(this, fileUri, document, new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB doc) {
                runOnUiThread(() -> {
                    if (cardProcessing != null) cardProcessing.setVisibility(View.GONE);
                    if (tvScanStatus != null) tvScanStatus.setText("Saved ✓");
                    Toast.makeText(ScanActivity.this, "Đã lưu vào danh sách tài liệu!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onProgress(int progressPercentage) {
                // Có thể cập nhật UI nếu cần
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    if (cardProcessing != null) cardProcessing.setVisibility(View.GONE);
                    if (btnSavePdf != null) btnSavePdf.setEnabled(true);
                    Toast.makeText(ScanActivity.this, "Lỗi đồng bộ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void resetToScanOptions() {
        if (scanOptionsSection != null) scanOptionsSection.setVisibility(View.VISIBLE);
        if (scanResultSection != null) scanResultSection.setVisibility(View.GONE);
        if (cardProcessing != null) cardProcessing.setVisibility(View.GONE);
        findViewById(R.id.card_preview).setVisibility(View.VISIBLE);
        if (ivScannedImage != null) ivScannedImage.setImageDrawable(null);
        currentBitmap = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanService != null) scanService.close();
    }
}

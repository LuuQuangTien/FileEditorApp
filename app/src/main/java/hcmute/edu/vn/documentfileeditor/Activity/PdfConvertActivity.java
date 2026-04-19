package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;

import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.AuthService;
import hcmute.edu.vn.documentfileeditor.Service.CloudConvertService;
import hcmute.edu.vn.documentfileeditor.Service.DocumentService;

public class PdfConvertActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_FORMAT = "extra_target_format"; // "docx" hoặc "xlsx"

    private String targetFormat = "docx";
    private CloudConvertService cloudConvertService;
    private DocumentRepository documentRepository;
    private AuthService authService;

    // Views
    private TextView tvTitle, tvSubtitle, tvScanStatus, tvTargetFormat;
    private MaterialCardView cardUploadPdf, cardProcessing, cardPreview;
    private MaterialButton btnConvert, btnRetake;
    private LinearLayout scanOptionsSection, scanResultSection;

    private Uri selectedPdfUri = null;
    private String selectedFileName = "";

    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_convert);

        if (getIntent().hasExtra(EXTRA_TARGET_FORMAT)) {
            targetFormat = getIntent().getStringExtra(EXTRA_TARGET_FORMAT);
        }

        cloudConvertService = new CloudConvertService();
        documentRepository = DocumentRepository.getInstance(this);
        authService = new AuthService();

        initViews();
        setupLaunchers();
        setupListeners();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        tvScanStatus = findViewById(R.id.tv_scan_status);
        tvTargetFormat = findViewById(R.id.tv_target_format);
        
        cardUploadPdf = findViewById(R.id.card_upload_pdf);
        cardProcessing = findViewById(R.id.card_processing);
        cardPreview = findViewById(R.id.card_preview);
        
        btnConvert = findViewById(R.id.btn_convert);
        btnRetake = findViewById(R.id.btn_retake);
        
        scanOptionsSection = findViewById(R.id.scan_options_section);
        scanResultSection = findViewById(R.id.scan_result_section);

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        if (targetFormat.equals("docx")) {
            tvTitle.setText("PDF to Word");
            tvSubtitle.setText("Convert PDF to editable DOCX");
            if (tvTargetFormat != null) tvTargetFormat.setText("to Word");
        } else {
            tvTitle.setText("PDF to Excel");
            tvSubtitle.setText("Convert PDF to editable XLSX");
            if (tvTargetFormat != null) tvTargetFormat.setText("to Excel");
        }
    }

    private void setupLaunchers() {
        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedPdfUri = result.getData().getData();
                        if (selectedPdfUri != null) {
                            showPdfPreview();
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        cardUploadPdf.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            pdfPickerLauncher.launch(intent);
        });

        btnConvert.setOnClickListener(v -> startConversion());
        btnRetake.setOnClickListener(v -> resetToInitialState());
    }

    private void showPdfPreview() {
        selectedFileName = new DocumentService().resolveFileName(this, selectedPdfUri);
        
        scanOptionsSection.setVisibility(View.GONE);
        scanResultSection.setVisibility(View.VISIBLE);
        cardPreview.setVisibility(View.VISIBLE);
        cardProcessing.setVisibility(View.GONE);
        
        TextView tvSelectedFileName = findViewById(R.id.tv_selected_file_name);
        tvSelectedFileName.setText(selectedFileName);
        
        tvScanStatus.setText("Ready");
        btnConvert.setEnabled(true);
        btnConvert.setVisibility(View.VISIBLE);
    }

    private void startConversion() {
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để lưu file", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConvert.setVisibility(View.GONE);
        cardProcessing.setVisibility(View.VISIBLE);
        tvScanStatus.setText("Đang chuẩn bị...");

        String baseName = selectedFileName.lastIndexOf(".") > 0 ? 
                selectedFileName.substring(0, selectedFileName.lastIndexOf(".")) : selectedFileName;

        cloudConvertService.convertDocument(this, selectedPdfUri, baseName, targetFormat, 
                new CloudConvertService.CloudConvertCallback() {
            @Override
            public void onProgress(String message) {
                if (tvScanStatus != null) tvScanStatus.setText(message);
            }

            @Override
            public void onSuccess(String localPath) {
                uploadToRepository(localPath, baseName + "." + targetFormat, userId);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(PdfConvertActivity.this, error, Toast.LENGTH_LONG).show();
                resetToInitialState();
            }
        });
    }

    private void uploadToRepository(String absolutePath, String fileName, String userId) {
        File docFile = new File(absolutePath);
        Uri fileUri = Uri.fromFile(docFile);

        DocumentFB document = new DocumentFB();
        document.setUserId(userId);
        document.setFileName(fileName);
        document.setFileType(targetFormat.equals("docx") ? FileType.WORD : FileType.EXCEL);

        documentRepository.uploadDocument(this, fileUri, document, new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB doc) {
                runOnUiThread(() -> {
                    if (cardProcessing != null) cardProcessing.setVisibility(View.GONE);
                    if (tvScanStatus != null) tvScanStatus.setText("Saved ✓");
                    Toast.makeText(PdfConvertActivity.this, "Đã lưu vào danh sách tài liệu!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onProgress(int progressPercentage) {}

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    if (cardProcessing != null) cardProcessing.setVisibility(View.GONE);
                    btnConvert.setVisibility(View.VISIBLE);
                    btnConvert.setEnabled(true);
                    Toast.makeText(PdfConvertActivity.this, "Lỗi đồng bộ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void resetToInitialState() {
        scanOptionsSection.setVisibility(View.VISIBLE);
        scanResultSection.setVisibility(View.GONE);
        selectedPdfUri = null;
        selectedFileName = "";
    }
}

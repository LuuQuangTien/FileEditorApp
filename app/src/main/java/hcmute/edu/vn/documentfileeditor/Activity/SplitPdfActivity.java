package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.EditText;
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
import hcmute.edu.vn.documentfileeditor.Service.DocumentService;
import hcmute.edu.vn.documentfileeditor.Service.PdfService;

public class SplitPdfActivity extends AppCompatActivity {

    private Uri selectedPdfUri;
    private String selectedFileName;
    private int totalPages = 0;

    private PdfService pdfService;
    private DocumentRepository documentRepository;
    private AuthService authService;

    // UI
    private LinearLayout scanOptionsSection, scanResultSection;
    private MaterialCardView cardUploadPdf, cardProcessing, cardPreview;
    private TextView tvSelectedFileName, tvPageInfo;
    private EditText etRange, etOutputName;
    private MaterialButton btnSplit, btnRetake;

    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_pdf);

        pdfService = new PdfService();
        documentRepository = DocumentRepository.getInstance(this);
        authService = new AuthService();

        initViews();
        setupLaunchers();
        setupListeners();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        scanOptionsSection = findViewById(R.id.scan_options_section);
        scanResultSection = findViewById(R.id.scan_result_section);
        
        cardUploadPdf = findViewById(R.id.card_upload_pdf);
        cardProcessing = findViewById(R.id.card_processing);
        cardPreview = findViewById(R.id.card_preview);
        
        tvSelectedFileName = findViewById(R.id.tv_selected_file_name);
        tvPageInfo = findViewById(R.id.tv_page_info);
        
        etRange = findViewById(R.id.et_range);
        etOutputName = findViewById(R.id.et_output_name);
        
        btnSplit = findViewById(R.id.btn_split);
        btnRetake = findViewById(R.id.btn_retake);
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
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            pdfPickerLauncher.launch(intent);
        });

        btnSplit.setOnClickListener(v -> startSplit());
        btnRetake.setOnClickListener(v -> resetToInitialState());
    }

    private void showPdfPreview() {
        selectedFileName = new DocumentService().resolveFileName(this, selectedPdfUri);
        
        // Calculate total pages
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(selectedPdfUri, "r");
            if (pfd != null) {
                PdfRenderer renderer = new PdfRenderer(pfd);
                totalPages = renderer.getPageCount();
                renderer.close();
                pfd.close();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không thể đọc file PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        scanOptionsSection.setVisibility(View.GONE);
        scanResultSection.setVisibility(View.VISIBLE);
        cardPreview.setVisibility(View.VISIBLE);
        cardProcessing.setVisibility(View.GONE);
        
        tvSelectedFileName.setText(selectedFileName);
        tvPageInfo.setText("Total Pages: " + totalPages);
        
        btnSplit.setEnabled(true);
        btnSplit.setVisibility(View.VISIBLE);
    }

    private void startSplit() {
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để lưu file", Toast.LENGTH_SHORT).show();
            return;
        }

        String range = etRange.getText().toString().trim();
        if (range.isEmpty()) {
            range = "all"; // Mặc định tách hết nến ko ghi gì
        }

        String outputName = etOutputName.getText().toString().trim();
        if (outputName.isEmpty()) {
            outputName = "Split_" + selectedFileName.replaceAll(".pdf", "");
        }

        btnSplit.setVisibility(View.GONE);
        cardProcessing.setVisibility(View.VISIBLE);

        String finalName = outputName;
        pdfService.splitPdf(this, selectedPdfUri, range, finalName, new PdfService.PdfCallback() {
            @Override
            public void onSuccess(String localPath) {
                uploadToRepository(localPath, finalName + ".pdf", userId);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(SplitPdfActivity.this, error, Toast.LENGTH_LONG).show();
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
        document.setFileType(FileType.PDF);

        documentRepository.uploadDocument(this, fileUri, document, new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB doc) {
                runOnUiThread(() -> {
                    Toast.makeText(SplitPdfActivity.this, "Đã cắt và lưu thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onProgress(int progressPercentage) {}

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    cardProcessing.setVisibility(View.GONE);
                    btnSplit.setVisibility(View.VISIBLE);
                    Toast.makeText(SplitPdfActivity.this, "Lỗi lưu file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void resetToInitialState() {
        scanOptionsSection.setVisibility(View.VISIBLE);
        scanResultSection.setVisibility(View.GONE);
        selectedPdfUri = null;
        selectedFileName = "";
        totalPages = 0;
        etRange.setText("");
        etOutputName.setText("");
    }
}

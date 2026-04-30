package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import hcmute.edu.vn.documentfileeditor.View.PdfAnnotationView;

public class AnnotatePdfActivity extends AppCompatActivity {

    private Uri selectedPdfUri;
    private String selectedFileName;
    
    private PdfService pdfService;
    private DocumentRepository documentRepository;
    private AuthService authService;

    // UI
    private LinearLayout scanOptionsSection, scanResultSection;
    private MaterialCardView cardUploadPdf, cardProcessing;
    
    private PdfAnnotationView pdfAnnotationView;
    private TextView tvPageInfo;
    private MaterialButton btnHighlight, btnText, btnSave;
    private ImageView btnPrev, btnNext;
    private EditText etOutputName;

    // PDF State
    private ParcelFileDescriptor pfd;
    private PdfRenderer renderer;
    private int currentPage = 0;
    private int totalPages = 0;
    private Bitmap currentPageBitmap;

    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annotate_pdf);

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
        
        pdfAnnotationView = findViewById(R.id.pdf_annotation_view);
        tvPageInfo = findViewById(R.id.tv_page_info);
        
        btnHighlight = findViewById(R.id.btn_highlight);
        btnText = findViewById(R.id.btn_text);
        btnSave = findViewById(R.id.btn_save);
        
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        
        etOutputName = findViewById(R.id.et_output_name);
    }

    private void setupLaunchers() {
        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedPdfUri = result.getData().getData();
                        if (selectedPdfUri != null) {
                            loadPdf();
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

        btnPrev.setOnClickListener(v -> showPage(currentPage - 1));
        btnNext.setOnClickListener(v -> showPage(currentPage + 1));
        
        btnHighlight.setOnClickListener(v -> {
            pdfAnnotationView.setMode(PdfAnnotationView.Mode.HIGHLIGHT);
            updateToolbarSelection(btnHighlight);
        });
        
        btnText.setOnClickListener(v -> {
            pdfAnnotationView.setMode(PdfAnnotationView.Mode.TEXT);
            updateToolbarSelection(btnText);
        });
        
        btnSave.setOnClickListener(v -> saveDocument());
    }

    private void updateToolbarSelection(MaterialButton selected) {
        btnHighlight.setBackgroundTintList(getResources().getColorStateList(R.color.white, null));
        btnText.setBackgroundTintList(getResources().getColorStateList(R.color.white, null));
        selected.setBackgroundTintList(getResources().getColorStateList(R.color.blue_100, null));
    }

    private void loadPdf() {
        selectedFileName = new DocumentService().resolveFileName(this, selectedPdfUri);
        try {
            pfd = getContentResolver().openFileDescriptor(selectedPdfUri, "r");
            if (pfd != null) {
                renderer = new PdfRenderer(pfd);
                totalPages = renderer.getPageCount();
                currentPage = 0;
                
                scanOptionsSection.setVisibility(View.GONE);
                scanResultSection.setVisibility(View.VISIBLE);
                
                showPage(0);
                
                // Default mode
                pdfAnnotationView.setMode(PdfAnnotationView.Mode.HIGHLIGHT);
                updateToolbarSelection(btnHighlight);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không thể đọc file PDF", Toast.LENGTH_SHORT).show();
            resetToInitialState();
        }
    }

    private void showPage(int index) {
        if (renderer == null || index < 0 || index >= totalPages) return;
        
        currentPage = index;
        tvPageInfo.setText("Page " + (currentPage + 1) + " of " + totalPages);
        
        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < totalPages - 1);
        
        if (currentPageBitmap != null && !currentPageBitmap.isRecycled()) {
            currentPageBitmap.recycle();
        }

        PdfRenderer.Page page = renderer.openPage(currentPage);
        
        // Scale for better quality on screen, using a fixed width 
        // that's large enough (e.g., 2048px width)
        int width = 1440;
        float ratio = (float) page.getHeight() / page.getWidth();
        int height = (int) (width * ratio);
        
        currentPageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        currentPageBitmap.eraseColor(Color.WHITE);
        page.render(currentPageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        
        pdfAnnotationView.setCurrentPage(currentPage, currentPageBitmap);
        page.close();
    }

    private void saveDocument() {
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để lưu file", Toast.LENGTH_SHORT).show();
            return;
        }

        String outputName = etOutputName.getText().toString().trim();
        if (outputName.isEmpty()) {
            outputName = "Annotated_" + selectedFileName.replaceAll(".pdf", "");
        }

        scanResultSection.setVisibility(View.GONE);
        cardProcessing.setVisibility(View.VISIBLE);

        String finalName = outputName;
        pdfService.saveAnnotatedPdf(this, selectedPdfUri, pdfAnnotationView.getAllAnnotations(), finalName, new PdfService.PdfCallback() {
            @Override
            public void onSuccess(String localPath) {
                uploadToRepository(localPath, finalName + ".pdf", userId);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AnnotatePdfActivity.this, error, Toast.LENGTH_LONG).show();
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
                    Toast.makeText(AnnotatePdfActivity.this, "Đã lưu tài liệu thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onProgress(int progressPercentage) {}

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    cardProcessing.setVisibility(View.GONE);
                    scanResultSection.setVisibility(View.VISIBLE);
                    Toast.makeText(AnnotatePdfActivity.this, "Lỗi lưu file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void resetToInitialState() {
        scanOptionsSection.setVisibility(View.VISIBLE);
        scanResultSection.setVisibility(View.GONE);
        cardProcessing.setVisibility(View.GONE);
        
        if (renderer != null) {
            try {
                renderer.close();
                pfd.close();
            } catch (Exception ignored) {}
        }
        
        selectedPdfUri = null;
        selectedFileName = "";
        totalPages = 0;
        currentPage = 0;
        etOutputName.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (renderer != null) {
            try {
                renderer.close();
                pfd.close();
            } catch (Exception ignored) {}
        }
        if (currentPageBitmap != null && !currentPageBitmap.isRecycled()) {
            currentPageBitmap.recycle();
        }
    }
}

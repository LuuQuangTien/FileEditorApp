package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Adapter.PdfFileAdapter;
import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.AuthService;
import hcmute.edu.vn.documentfileeditor.Service.PdfService;

public class MergePdfActivity extends AppCompatActivity {

    private final List<Uri> selectedPdfs = new ArrayList<>();
    private PdfFileAdapter adapter;
    private PdfService pdfService;
    private DocumentRepository documentRepository;
    private AuthService authService;

    // Views
    private RecyclerView rvPdfs;
    private MaterialButton btnAddPdfs, btnMerge;
    private EditText etOutputName;
    private MaterialCardView cardProcessing;
    private LinearLayout contentSection;
    private TextView tvEmptyState;

    private ActivityResultLauncher<Intent> multiPdfPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_pdf);

        pdfService = new PdfService();
        documentRepository = DocumentRepository.getInstance(this);
        authService = new AuthService();

        initViews();
        setupRecyclerView();
        setupLaunchers();
        setupListeners();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        rvPdfs = findViewById(R.id.rv_pdfs);
        btnAddPdfs = findViewById(R.id.btn_add_pdfs);
        btnMerge = findViewById(R.id.btn_merge);
        etOutputName = findViewById(R.id.et_output_name);
        cardProcessing = findViewById(R.id.card_processing);
        contentSection = findViewById(R.id.content_section);
        tvEmptyState = findViewById(R.id.tv_empty_state);
    }

    private void setupRecyclerView() {
        adapter = new PdfFileAdapter(this, selectedPdfs, position -> {
            selectedPdfs.remove(position);
            adapter.notifyItemRemoved(position);
            updateUIState();
        });
        rvPdfs.setLayoutManager(new LinearLayoutManager(this));
        rvPdfs.setAdapter(adapter);

        // Add drag & drop support
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(selectedPdfs, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not supported
            }
        });
        itemTouchHelper.attachToRecyclerView(rvPdfs);
    }

    private void setupLaunchers() {
        multiPdfPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = data.getClipData().getItemAt(i).getUri();
                                selectedPdfs.add(uri);
                            }
                        } else if (data.getData() != null) {
                            selectedPdfs.add(data.getData());
                        }
                        adapter.notifyDataSetChanged();
                        updateUIState();
                    }
                }
        );
    }

    private void setupListeners() {
        btnAddPdfs.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            multiPdfPicker.launch(intent);
        });

        btnMerge.setOnClickListener(v -> startMerge());
    }

    private void updateUIState() {
        if (selectedPdfs.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvPdfs.setVisibility(View.GONE);
            btnMerge.setEnabled(false);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvPdfs.setVisibility(View.VISIBLE);
            btnMerge.setEnabled(selectedPdfs.size() > 1);
        }
    }

    private void startMerge() {
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để lưu file", Toast.LENGTH_SHORT).show();
            return;
        }

        String outputName = etOutputName.getText().toString().trim();
        if (outputName.isEmpty()) {
            outputName = "Merged_Document";
        }

        contentSection.setVisibility(View.GONE);
        cardProcessing.setVisibility(View.VISIBLE);

        String finalName = outputName;
        pdfService.mergePdfs(this, selectedPdfs, finalName, new PdfService.PdfCallback() {
            @Override
            public void onSuccess(String localPath) {
                uploadToRepository(localPath, finalName + ".pdf", userId);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(MergePdfActivity.this, error, Toast.LENGTH_LONG).show();
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
                    Toast.makeText(MergePdfActivity.this, "Đã merged và lưu thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Done, return to tools
                });
            }

            @Override
            public void onProgress(int progressPercentage) {}

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MergePdfActivity.this, "Lỗi lưu file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetToInitialState();
                });
            }
        });
    }

    private void resetToInitialState() {
        contentSection.setVisibility(View.VISIBLE);
        cardProcessing.setVisibility(View.GONE);
    }
}

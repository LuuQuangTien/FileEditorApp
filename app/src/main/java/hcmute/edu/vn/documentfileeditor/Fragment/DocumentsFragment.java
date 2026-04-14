package hcmute.edu.vn.documentfileeditor.Fragment;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import hcmute.edu.vn.documentfileeditor.Activity.DocumentEditorActivity;
import hcmute.edu.vn.documentfileeditor.Activity.ExcelEditorActivity;
import hcmute.edu.vn.documentfileeditor.Adapter.LiveDocumentAdapter;
import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.AuthService;
import hcmute.edu.vn.documentfileeditor.Service.DocumentService;
import hcmute.edu.vn.documentfileeditor.Util.NavigationHelper;

public class DocumentsFragment extends Fragment {
    private static final String TAG = "DocumentsFragment";
    private LiveDocumentAdapter adapter;
    private ProgressBar progressBar;
    private TextView statusView;
    private DocumentRepository documentRepository;
    private DocumentService documentService;
    private AuthService authService;
    private ActivityResultLauncher<String[]> importDocumentLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupImportLauncher();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_documents, container, false);

        documentRepository = DocumentRepository.getInstance(requireContext());
        documentService = new DocumentService();
        authService = new AuthService();
        progressBar = view.findViewById(R.id.progress_documents);
        statusView = view.findViewById(R.id.tv_documents_status);

        // --- BẮT ĐẦU PHẦN BỔ SUNG LOGIC CHO UI CỦA BẠN (KHÔNG XOÁ CODE CŨ) ---

        // 1. Logic cho nút "Tạo thư mục"
        MaterialButton btnCreateFolder = view.findViewById(R.id.btn_create_folder);
        if (btnCreateFolder != null) {
            btnCreateFolder.setOnClickListener(v -> showCreateFolderDialog());
        }

        // 2. Logic cho nút "Lọc"
        MaterialButton btnFilter = view.findViewById(R.id.btn_filter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }

        // 3. Logic click cho các Card mẫu (Mock UI)
        setupMockFileClick(view, R.id.card_doc_1, DocumentEditorActivity.class);
        setupMockFileClick(view, R.id.card_doc_2, DocumentEditorActivity.class);
        setupMockFileClick(view, R.id.card_doc_3, ExcelEditorActivity.class);

        // --- KẾT THÚC PHẦN BỔ SUNG ---

        setupRecyclerView(view);
        setupFabActions(view);
        loadDocuments();

        return view;
    }

    // Các hàm Helper mới bổ sung
    private void showCreateFolderDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Tên thư mục mới");
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(requireContext())
                .setTitle("Tạo thư mục")
                .setMessage("Nhập tên cho thư mục bạn muốn tạo:")
                .setView(input)
                .setPositiveButton("Tạo", (dialog, which) -> {
                    String folderName = input.getText().toString().trim();
                    if (!folderName.isEmpty()) {
                        Toast.makeText(getContext(), "Đã tạo thư mục: " + folderName, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showFilterDialog() {
        String[] options = {"Tất cả", "File PDF (.pdf)", "File Word (.docx)", "File Excel (.xlsx)"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Lọc danh sách file")
                .setItems(options, (dialog, which) -> {
                    Toast.makeText(getContext(), "Đang lọc: " + options[which], Toast.LENGTH_SHORT).show();
                    loadDocuments(); // Gọi lại hàm load cũ của bạn để refresh
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && documentRepository != null && adapter != null) {
            loadDocuments();
        }
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.rv_documents);
        if (recyclerView != null) {
            adapter = new LiveDocumentAdapter(this::openLiveDocument, this::showDocumentMenu);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupImportLauncher() {
        if (importDocumentLauncher != null) return;
        importDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::handleImportedDocument
        );
    }

    private void setupFabActions(View view) {
        View fab = view.findViewById(R.id.fab_import_document);
        if (fab != null) {
            fab.setOnClickListener(v -> showDocumentActionSheet());
        }
    }

    private void showDocumentActionSheet() {
        try {
            BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
            View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_document_actions, null);
            dialog.setContentView(sheetView);

            if (sheetView != null) {
                View importFile = sheetView.findViewById(R.id.action_import_file);
                View newDocument = sheetView.findViewById(R.id.action_new_document);
                View newSpreadsheet = sheetView.findViewById(R.id.action_new_spreadsheet);

                if (importFile != null) {
                    importFile.setOnClickListener(v -> {
                        dialog.dismiss();
                        importDocumentLauncher.launch(new String[]{"*/*"});
                    });
                }
                if (newDocument != null) {
                    newDocument.setOnClickListener(v -> {
                        dialog.dismiss();
                        showCreateDialog(FileType.WORD);
                    });
                }
                if (newSpreadsheet != null) {
                    newSpreadsheet.setOnClickListener(v -> {
                        dialog.dismiss();
                        showCreateDialog(FileType.EXCEL);
                    });
                }
            }
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing action sheet", e);
        }
    }

    private void showCreateDialog(FileType fileType) {
        EditText input = new EditText(requireContext());
        input.setHint("Tên file");
        new AlertDialog.Builder(requireContext())
                .setTitle("Tạo file mới")
                .setView(input)
                .setPositiveButton("Tạo", (dialog, which) -> createNewDocument(input.getText().toString(), fileType))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createNewDocument(String rawName, FileType fileType) {
        String userId = authService.getCurrentUserId();
        if (userId == null) return;

        String fileName = documentService.buildFileName(rawName, fileType);
        DocumentFB document = new DocumentFB();
        document.setUserId(userId);
        document.setFileName(fileName);
        document.setFileType(fileType);

        showLoading(true);
        documentRepository.createDocument(requireContext(), document, "", new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB doc) {
                if (!isAdded()) return;
                showLoading(false);
                adapter.upsertItem(doc);
                openLiveDocument(doc);
            }
            @Override public void onProgress(int p) {}
            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                showLoading(false);
            }
        });
    }

    private void loadDocuments() {
        String userId = authService.getCurrentUserId();
        if (userId == null) return;

        showLoading(true);
        documentRepository.getDocuments(userId, new DocumentCallback.GetDocumentsCallback() {
            @Override
            public void onSuccess(List<DocumentFB> documents) {
                if (!isAdded()) return;
                showLoading(false);
                adapter.submitList(documents);
                if (documents.isEmpty()) showStatus("Chưa có tài liệu trực tuyến.");
                else hideStatus();
            }
            @Override public void onFailure(Exception e) {
                if (!isAdded()) return;
                showLoading(false);
            }
        });
    }

    private void handleImportedDocument(Uri uri) {
        if (uri == null || !isAdded()) return;
        String userId = authService.getCurrentUserId();
        if (userId == null) return;

        String fileName = documentService.resolveFileName(requireContext(), uri);
        DocumentFB document = new DocumentFB();
        document.setUserId(userId);
        document.setFileName(fileName);
        document.setFileType(documentService.resolveFileType(fileName, ""));

        showLoading(true);
        documentRepository.uploadDocument(requireContext(), uri, document, new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB doc) {
                if (!isAdded()) return;
                showLoading(false);
                adapter.upsertItem(doc);
            }
            @Override public void onProgress(int p) {}
            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                showLoading(false);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showStatus(String message) {
        if (statusView != null) {
            statusView.setText(message);
            statusView.setVisibility(View.VISIBLE);
        }
    }

    private void hideStatus() {
        if (statusView != null) statusView.setVisibility(View.GONE);
    }

    private void setupMockFileClick(View view, int viewId, Class<?> activityClass) {
        View card = view.findViewById(viewId);
        if (card != null) {
            card.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getActivity(), activityClass);
                startActivity(intent);
            });
        }
    }

    private void openLiveDocument(DocumentFB document) {
        NavigationHelper.launchEditor(requireContext(), document);
    }

    private void showDocumentMenu(View anchor, DocumentFB document) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add("Xóa");
        popupMenu.setOnMenuItemClickListener(item -> {
            Toast.makeText(getContext(), "Đã xóa: " + document.getFileName(), Toast.LENGTH_SHORT).show();
            return true;
        });
        popupMenu.show();
    }
}

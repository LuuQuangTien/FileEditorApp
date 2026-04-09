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

        setupRecyclerView(view);
        setupFabActions(view);
        setupRefreshAction(view);
        setupMockFileClick(view, R.id.card_doc_1, DocumentEditorActivity.class);
        setupMockFileClick(view, R.id.card_doc_2, DocumentEditorActivity.class);
        setupMockFileClick(view, R.id.card_doc_3, ExcelEditorActivity.class);
        loadDocuments();

        return view;
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
        adapter = new LiveDocumentAdapter(this::openLiveDocument, this::showDocumentMenu);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupImportLauncher() {
        if (importDocumentLauncher != null) {
            return;
        }
        importDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::handleImportedDocument
        );
    }

    private void setupRefreshAction(View view) {
        View filterButton = view.findViewById(R.id.btn_filter);
        filterButton.setOnClickListener(v -> loadDocuments());
    }

    private void setupFabActions(View view) {
        View fab = view.findViewById(R.id.fab_import_document);
        fab.setOnClickListener(v -> showDocumentActionSheet());
    }

    private void showDocumentActionSheet() {
        try {
            BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
            View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_document_actions, null);
            dialog.setContentView(sheetView);

            if (sheetView == null) {
                reportUiError("Could not open action sheet.", null);
                return;
            }

            View importFile = sheetView.findViewById(R.id.action_import_file);
            View newDocument = sheetView.findViewById(R.id.action_new_document);
            View newSpreadsheet = sheetView.findViewById(R.id.action_new_spreadsheet);

            importFile.setOnClickListener(v -> {
                dialog.dismiss();
                importDocumentLauncher.launch(new String[]{
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "image/*",
                        "text/plain"
                });
            });

            newDocument.setOnClickListener(v -> {
                dialog.dismiss();
                showCreateDialog(FileType.WORD);
            });

            newSpreadsheet.setOnClickListener(v -> {
                dialog.dismiss();
                showCreateDialog(FileType.EXCEL);
            });

            dialog.show();
        } catch (Exception e) {
            reportUiError("Failed to open add-document actions.", e);
        }
    }

    private void showCreateDialog(FileType fileType) {
        try {
            EditText input = new EditText(requireContext());
            input.setHint(fileType == FileType.EXCEL ? "Budget Plan" : "Project Notes");
            input.setPadding(48, 32, 48, 32);

            String title = fileType == FileType.EXCEL ? "Create spreadsheet" : "Create document";

            new AlertDialog.Builder(requireContext())
                    .setTitle(title)
                    .setMessage("Enter a file name")
                    .setView(input)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Create", (dialog, which) -> {
                        String rawName = input.getText().toString().trim();
                        createNewDocument(rawName, fileType);
                    })
                    .show();
        } catch (Exception e) {
            reportUiError("Failed to open create dialog.", e);
        }
    }

    private void createNewDocument(String rawName, FileType fileType) {
        try {
            String userId = authService.getCurrentUserId();
            if (userId == null) {
                showStatus("Please sign in before creating a document.");
                return;
            }

            String fileName = documentService.buildFileName(rawName, fileType);
            String initialContent = "";

            DocumentFB document = new DocumentFB();
            document.setUserId(userId);
            document.setFileName(fileName);
            document.setFileType(fileType);

            showLoading(true);
            showStatus("Creating " + fileName + "...");

            documentRepository.createDocument(requireContext(), document, initialContent, new DocumentCallback.UploadCallback() {
                @Override
                public void onSuccess(DocumentFB documentFB) {
                    if (!isAdded()) {
                        return;
                    }
                    showLoading(false);
                    showStatus("Created locally. Cloud sync will continue in background.");
                    adapter.upsertItem(documentFB);
                    Toast.makeText(requireContext(), "Created " + documentFB.getFileName(), Toast.LENGTH_SHORT).show();
                    openLiveDocument(documentFB);
                }

                @Override
                public void onProgress(int progressPercentage) {
                    if (!isAdded()) {
                        return;
                    }
                    showStatus("Creating " + fileName + "... " + progressPercentage + "%");
                }

                @Override
                public void onFailure(Exception e) {
                    if (!isAdded()) {
                        return;
                    }
                    showLoading(false);
                    reportUiError("Could not create " + fileName, e);
                }
            });
        } catch (Exception e) {
            showLoading(false);
            reportUiError("Create document crashed before upload started.", e);
        }
    }

    private void loadDocuments() {
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            showStatus("Please sign in to load your documents.");
            return;
        }

        showLoading(true);
        documentRepository.getDocuments(userId, new DocumentCallback.GetDocumentsCallback() {
            @Override
            public void onSuccess(List<DocumentFB> documents) {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                adapter.submitList(documents);
                if (documents.isEmpty()) {
                    showStatus("No live documents yet. Mock cards below are still available for UI testing.");
                } else {
                    hideStatus();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                if (adapter.getItemCount() == 0) {
                    showStatus("Could not load live documents. You can still test the static mock cards below.");
                }
            }
        });
    }

    private void handleImportedDocument(Uri uri) {
        try {
            if (uri == null || !isAdded()) {
                return;
            }

            String userId = authService.getCurrentUserId();
            if (userId == null) {
                showStatus("Please sign in before importing a document.");
                return;
            }

            try {
                requireContext().getContentResolver().takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (SecurityException ignored) {
            }

            String fileName = documentService.resolveFileName(requireContext(), uri);
            DocumentFB document = new DocumentFB();
            document.setUserId(userId);
            document.setFileName(fileName);
            document.setFileType(documentService.resolveFileType(fileName, requireContext().getContentResolver().getType(uri)));

            showLoading(true);
            showStatus("Importing " + fileName + "...");
            documentRepository.uploadDocument(requireContext(), uri, document, new DocumentCallback.UploadCallback() {
                @Override
                public void onSuccess(DocumentFB documentFB) {
                    if (!isAdded()) {
                        return;
                    }
                    showLoading(false);
                    adapter.upsertItem(documentFB);
                    Toast.makeText(requireContext(), "Imported " + documentFB.getFileName(), Toast.LENGTH_SHORT).show();
                    showStatus("Imported locally. Cloud sync will continue in background.");
                }

                @Override
                public void onProgress(int progressPercentage) {
                    if (!isAdded()) {
                        return;
                    }
                    showStatus("Importing " + fileName + "... " + progressPercentage + "%");
                }

                @Override
                public void onFailure(Exception e) {
                    if (!isAdded()) {
                        return;
                    }
                    showLoading(false);
                    reportUiError("Could not import " + fileName, e);
                }
            });
        } catch (Exception e) {
            showLoading(false);
            reportUiError("Import flow crashed before upload started.", e);
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showStatus(String message) {
        statusView.setText(message);
        statusView.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusView.setVisibility(View.GONE);
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
        try {
            if ((document.getLocalPath() == null || document.getLocalPath().isEmpty())
                    && document.getCloudStorageUrl() != null
                    && !document.getCloudStorageUrl().isEmpty()) {
                showLoading(true);
                showStatus("Downloading " + document.getFileName() + "...");
                documentRepository.downloadIfNeeded(requireContext(), document, new DocumentCallback.DownloadCallback() {
                    @Override
                    public void onSuccess(String localPath) {
                        if (!isAdded()) {
                            return;
                        }
                        showLoading(false);
                        hideStatus();
                        document.setLocalPath(localPath);
                        NavigationHelper.launchEditor(requireContext(), document);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) {
                            return;
                        }
                        showLoading(false);
                        reportUiError("Could not download " + document.getFileName(), e);
                    }
                });
                return;
            }

            NavigationHelper.launchEditor(requireContext(), document);
        } catch (Exception e) {
            showLoading(false);
            reportUiError("Open document crashed.", e);
        }
    }

    private void showDocumentMenu(View anchor, DocumentFB document) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add("Delete");
        popupMenu.getMenu().add("Refresh");
        if (document.getCloudStorageUrl() == null || document.getCloudStorageUrl().isEmpty()) {
            popupMenu.getMenu().add("Retry sync");
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Delete".equals(title)) {
                confirmDeleteDocument(document);
                return true;
            }
            if ("Refresh".equals(title)) {
                loadDocuments();
                return true;
            }
            if ("Retry sync".equals(title)) {
                documentRepository.retrySync(document);
                showStatus("Retrying cloud sync for " + document.getFileName() + "...");
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void confirmDeleteDocument(DocumentFB document) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete document")
                .setMessage("Delete " + document.getFileName() + "?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteDocument(document))
                .show();
    }

    private void deleteDocument(DocumentFB document) {
        showLoading(true);
        showStatus("Deleting " + document.getFileName() + "...");
        documentRepository.deleteDocument(document, new DocumentCallback.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                Toast.makeText(requireContext(), "Deleted " + document.getFileName(), Toast.LENGTH_SHORT).show();
                loadDocuments();
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                reportUiError("Could not delete " + document.getFileName(), e);
            }
        });
    }

    private void reportUiError(String message, Exception e) {
        if (e != null) {
            Log.e(TAG, message, e);
        } else {
            Log.e(TAG, message);
        }

        String detail = e != null && e.getMessage() != null && !e.getMessage().isEmpty()
                ? e.getClass().getSimpleName() + ": " + e.getMessage()
                : "No exception message available";

        String fullMessage = message + "\n" + detail;
        showStatus(fullMessage);

        if (isAdded()) {
            Toast.makeText(requireContext(), fullMessage, Toast.LENGTH_LONG).show();
        }
    }
}

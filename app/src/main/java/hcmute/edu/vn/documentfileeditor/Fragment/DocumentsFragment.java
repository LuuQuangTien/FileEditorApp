package hcmute.edu.vn.documentfileeditor.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import hcmute.edu.vn.documentfileeditor.Activity.DocumentEditorActivity;
import hcmute.edu.vn.documentfileeditor.Activity.ExcelEditorActivity;
import hcmute.edu.vn.documentfileeditor.Adapter.FolderAdapter;
import hcmute.edu.vn.documentfileeditor.Adapter.LiveDocumentAdapter;
import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Entity.FolderItem;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.AuthService;
import hcmute.edu.vn.documentfileeditor.Service.DocumentService;
import hcmute.edu.vn.documentfileeditor.Util.NavigationHelper;

public class DocumentsFragment extends Fragment {
    private static final String TAG = "DocumentsFragment";
    private static final String FOLDER_PREFS = "documents_fragment_prefs";
    private static final String FOLDER_KEY = "custom_folders";
    private static final String ASSIGN_PREFS = "documents_folder_assignments";
    private static final String ASSIGN_KEY = "document_folder_links";

    private enum FilterOption {
        ALL("Tất cả"),
        FOLDERS_ONLY("Chỉ thư mục"),
        FILES_ONLY("Chỉ file"),
        PDF("File PDF"),
        WORD("File Word"),
        EXCEL("File Excel"),
        IMAGE("File ảnh"),
        OTHER("File khác");

        private final String label;

        FilterOption(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private LiveDocumentAdapter adapter;
    private FolderAdapter folderAdapter;
    private ProgressBar progressBar;
    private TextView statusView;
    private TextView foldersLabelView;
    private View backFolderButton;
    private RecyclerView foldersRecyclerView;
    private EditText searchEditText;
    private DocumentRepository documentRepository;
    private DocumentService documentService;
    private AuthService authService;
    private ActivityResultLauncher<String[]> importDocumentLauncher;
    private final List<DocumentFB> allDocuments = new ArrayList<>();
    private final List<FolderItem> allFolders = new ArrayList<>();
    private String currentSearchQuery = "";
    private FilterOption currentFilterOption = FilterOption.ALL;
    private String selectedFolderName;
    private int documentLoadVersion;

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
        foldersLabelView = view.findViewById(R.id.tv_folders_label);
        backFolderButton = view.findViewById(R.id.btn_back_folder);
        foldersRecyclerView = view.findViewById(R.id.rv_folders);
        searchEditText = view.findViewById(R.id.et_search_documents);

        MaterialButton btnCreateFolder = view.findViewById(R.id.btn_create_folder);
        MaterialButton btnFilter = view.findViewById(R.id.btn_filter);
        if (btnCreateFolder != null) {
            btnCreateFolder.setOnClickListener(v -> showCreateFolderDialog());
        }
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }

        setupMockFileClick(view, R.id.card_doc_1, DocumentEditorActivity.class);
        setupMockFileClick(view, R.id.card_doc_2, DocumentEditorActivity.class);
        setupMockFileClick(view, R.id.card_doc_3, ExcelEditorActivity.class);

        setupFolderRecyclerView();
        setupRecyclerView(view);
        setupSearch();
        setupFabActions(view);
        loadFolders();
        loadDocuments();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        resetFileScreenState();
        if (isAdded() && documentRepository != null && adapter != null) {
            loadFolders();
            loadDocuments();
        }
    }

    private void setupFolderRecyclerView() {
        if (foldersRecyclerView != null) {
            folderAdapter = new FolderAdapter(this::openFolder, this::showFolderMenu);
            foldersRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            foldersRecyclerView.setAdapter(folderAdapter);
        }
        if (foldersLabelView != null) {
            foldersLabelView.setOnClickListener(v -> {
                if (selectedFolderName != null) {
                    selectedFolderName = null;
                    applyFilters();
                }
            });
        }
        if (backFolderButton != null) {
            backFolderButton.setOnClickListener(v -> {
                selectedFolderName = null;
                applyFilters();
            });
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

    private void setupSearch() {
        if (searchEditText == null) {
            return;
        }
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s == null ? "" : s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupImportLauncher() {
        if (importDocumentLauncher != null) {
            return;
        }
        importDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportedDocument);
    }

    private void setupFabActions(View view) {
        View fab = view.findViewById(R.id.fab_import_document);
        if (fab != null) {
            fab.setOnClickListener(v -> showDocumentActionSheet());
        }
    }

    private void showCreateFolderDialog() {
        EditText input = buildInput("Tên thư mục mới");
        new AlertDialog.Builder(requireContext())
                .setTitle("Tạo thư mục")
                .setMessage("Nhập tên cho thư mục bạn muốn tạo:")
                .setView(input)
                .setPositiveButton("Tạo", (dialog, which) -> {
                    String folderName = input.getText().toString().trim();
                    if (folderName.isEmpty()) {
                        Toast.makeText(getContext(), "Tên thư mục không được để trống", Toast.LENGTH_SHORT).show();
                    } else if (!saveFolder(folderName)) {
                        Toast.makeText(getContext(), "Thư mục đã tồn tại", Toast.LENGTH_SHORT).show();
                    } else {
                        loadFolders();
                        Toast.makeText(getContext(), "Đã tạo thư mục: " + folderName, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showFilterDialog() {
        FilterOption[] options = FilterOption.values();
        String[] labels = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].getLabel();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Lọc file/thư mục")
                .setSingleChoiceItems(labels, currentFilterOption.ordinal(), (dialog, which) -> {
                    currentFilterOption = options[which];
                    selectedFolderName = null;
                    applyFilters();
                    Toast.makeText(getContext(), "Đang lọc: " + labels[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
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
        EditText input = buildInput("Tên file");
        new AlertDialog.Builder(requireContext())
                .setTitle("Tạo file mới")
                .setView(input)
                .setPositiveButton("Tạo", (dialog, which) -> createNewDocument(input.getText().toString(), fileType))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createNewDocument(String rawName, FileType fileType) {
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            return;
        }
        String fileName = documentService.buildFileName(rawName, fileType);
        DocumentFB document = new DocumentFB();
        document.setUserId(userId);
        document.setFileName(fileName);
        document.setFileType(fileType);
        showLoading(true);
        documentRepository.createDocument(requireContext(), document, "", new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB doc) {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                allDocuments.add(0, doc);
                if (selectedFolderName != null) {
                    assignDocumentToFolder(doc.getId(), selectedFolderName);
                }
                applyFilters();
                Toast.makeText(requireContext(), buildCloudSyncStatusMessage("Đã tạo file cục bộ."), Toast.LENGTH_LONG).show();
                openLiveDocument(doc);
            }

            @Override
            public void onProgress(int p) {
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                Toast.makeText(requireContext(), resolveErrorMessage(e, "Tạo file thất bại"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDocuments() {
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            return;
        }
        final int loadVersion = ++documentLoadVersion;
        final int[] maxDocumentCountSeen = {-1};
        showLoading(true);
        documentRepository.getDocuments(userId, new DocumentCallback.GetDocumentsCallback() {
            @Override
            public void onSuccess(List<DocumentFB> documents) {
                if (!isAdded()) {
                    return;
                }
                if (loadVersion != documentLoadVersion) {
                    return;
                }
                int incomingSize = documents == null ? 0 : documents.size();
                if (incomingSize < maxDocumentCountSeen[0]) {
                    Log.d(TAG, "Ignoring smaller document batch. currentMax=" + maxDocumentCountSeen[0] + ", incoming=" + incomingSize);
                    return;
                }
                maxDocumentCountSeen[0] = incomingSize;
                Log.d(TAG, "Applying document batch size=" + incomingSize + ", loadVersion=" + loadVersion);
                showLoading(false);
                allDocuments.clear();
                if (documents != null) {
                    allDocuments.addAll(documents);
                }
                applyFilters();
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }
                if (loadVersion != documentLoadVersion) {
                    return;
                }
                showLoading(false);
            }
        });
    }

    private void loadFolders() {
        allFolders.clear();
        allFolders.addAll(readFolders());
        applyFilters();
    }

    private void handleImportedDocument(Uri uri) {
        if (uri == null || !isAdded()) {
            return;
        }
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            return;
        }

        String fileName = documentService.resolveFileName(requireContext(), uri);
        DocumentFB document = new DocumentFB();
        document.setUserId(userId);
        document.setFileName(fileName);
        document.setFileType(documentService.resolveFileType(fileName, ""));
        showLoading(true);
        documentRepository.uploadDocument(requireContext(), uri, document, new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB doc) {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                allDocuments.add(0, doc);
                if (selectedFolderName != null) {
                    assignDocumentToFolder(doc.getId(), selectedFolderName);
                }
                applyFilters();
                Toast.makeText(requireContext(), buildCloudSyncStatusMessage("Đã nhập file vào máy."), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProgress(int p) {
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                Toast.makeText(requireContext(), resolveErrorMessage(e, "Nhập file thất bại"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void showStatus(String message) {
        if (statusView != null) {
            statusView.setText(message);
            statusView.setVisibility(View.VISIBLE);
        }
    }

    private void hideStatus() {
        if (statusView != null) {
            statusView.setVisibility(View.GONE);
        }
    }

    private void setupMockFileClick(View view, int viewId, Class<?> activityClass) {
        View card = view.findViewById(viewId);
        if (card != null) {
            card.setOnClickListener(v -> startActivity(new Intent(getActivity(), activityClass)));
        }
    }

    private void openLiveDocument(DocumentFB document) {
        if ((document.getLocalPath() == null || document.getLocalPath().isEmpty())
                && document.getCloudStorageUrl() != null
                && !document.getCloudStorageUrl().isEmpty()) {
            showLoading(true);
            documentRepository.downloadIfNeeded(requireContext(), document, new DocumentCallback.DownloadCallback() {
                @Override
                public void onSuccess(String localPath) {
                    if (!isAdded()) {
                        return;
                    }
                    showLoading(false);
                    document.setLocalPath(localPath);
                    NavigationHelper.launchEditor(requireContext(), document);
                }

                @Override
                public void onFailure(Exception e) {
                    if (!isAdded()) {
                        return;
                    }
                    showLoading(false);
                    Toast.makeText(requireContext(), resolveErrorMessage(e, "Không thể mở file này lúc này"), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Could not open remote document", e);
                }
            });
            return;
        }
        NavigationHelper.launchEditor(requireContext(), document);
    }

    private void openFolder(FolderItem folderItem) {
        selectedFolderName = folderItem.getName();
        applyFilters();
        Toast.makeText(getContext(), "Đang xem thư mục: " + folderItem.getName(), Toast.LENGTH_SHORT).show();
    }

    private void showFolderMenu(View anchor, FolderItem folderItem) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add("Mở");
        popupMenu.getMenu().add("Đổi tên");
        popupMenu.getMenu().add("Xóa");
        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Mở".equals(title)) {
                openFolder(folderItem);
            } else if ("Đổi tên".equals(title)) {
                showRenameFolderDialog(folderItem);
            } else if ("Xóa".equals(title)) {
                showDeleteFolderDialog(folderItem);
            }
            return true;
        });
        popupMenu.show();
    }

    private void showDocumentMenu(View anchor, DocumentFB document) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add("Đổi tên");
        popupMenu.getMenu().add("Gán vào thư mục");
        if (getAssignedFolderName(document.getId()) != null) {
            popupMenu.getMenu().add("Bỏ khỏi thư mục");
        }
        popupMenu.getMenu().add("Xóa");
        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Xóa".equals(title)) {
                showDeleteDocumentDialog(document);
            } else if ("Đổi tên".equals(title)) {
                showRenameDialog(document);
            } else if ("Gán vào thư mục".equals(title)) {
                showAssignFolderDialog(document);
            } else if ("Bỏ khỏi thư mục".equals(title)) {
                removeDocumentFolderAssignment(document.getId());
                applyFilters();
                Toast.makeText(getContext(), "Đã bỏ file khỏi thư mục", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        popupMenu.show();
    }

    private void showDeleteDocumentDialog(DocumentFB document) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa file")
                .setMessage("Thao tác này sẽ xóa file khỏi danh sách Firestore và bộ nhớ cục bộ. Asset trên Cloudinary hiện chưa bị xóa tự động.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteDocument(document))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteDocument(DocumentFB document) {
        showLoading(true);
        documentRepository.deleteDocument(document, new DocumentCallback.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                removeDocumentFolderAssignment(document.getId());
                removeDocumentFromCache(document.getId());
                applyFilters();
                Toast.makeText(getContext(), "Đã xóa khỏi danh sách và bộ nhớ máy: " + document.getFileName(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                Toast.makeText(getContext(), resolveErrorMessage(e, "Xóa thất bại"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAssignFolderDialog(DocumentFB document) {
        if (allFolders.isEmpty()) {
            Toast.makeText(getContext(), "Chưa có thư mục để gán", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] folderNames = new String[allFolders.size()];
        for (int i = 0; i < allFolders.size(); i++) {
            folderNames[i] = allFolders.get(i).getName();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Gán file vào thư mục")
                .setItems(folderNames, (dialog, which) -> {
                    assignDocumentToFolder(document.getId(), folderNames[which]);
                    applyFilters();
                    Toast.makeText(getContext(), "Đã gán vào: " + folderNames[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showRenameFolderDialog(FolderItem folderItem) {
        EditText input = buildInput(null);
        input.setText(folderItem.getName());
        input.setSelection(folderItem.getName().length());
        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi tên thư mục")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty() || newName.equals(folderItem.getName())) {
                        return;
                    }
                    if (!renameFolder(folderItem.getName(), newName)) {
                        Toast.makeText(getContext(), "Tên thư mục đã tồn tại", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (folderItem.getName().equals(selectedFolderName)) {
                        selectedFolderName = newName;
                    }
                    loadFolders();
                    Toast.makeText(getContext(), "Đã đổi tên thư mục", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteFolderDialog(FolderItem folderItem) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa thư mục")
                .setMessage("Thư mục sẽ bị xóa khỏi danh sách cục bộ. Các file bên trong sẽ được bỏ gán khỏi thư mục này.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteFolder(folderItem.getName());
                    if (folderItem.getName().equals(selectedFolderName)) {
                        selectedFolderName = null;
                    }
                    loadFolders();
                    Toast.makeText(getContext(), "Đã xóa thư mục: " + folderItem.getName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showRenameDialog(DocumentFB document) {
        String currentName = document.getFileName();
        String extension = "";
        String baseName = currentName;
        int lastDotIndex = currentName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            baseName = currentName.substring(0, lastDotIndex);
            extension = currentName.substring(lastDotIndex);
        }

        EditText input = buildInput(null);
        input.setText(baseName);
        input.setSelection(baseName.length());
        String finalExtension = extension;
        String finalBaseName = baseName;
        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi tên file")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newBaseName = input.getText().toString().trim();
                    if (newBaseName.isEmpty() || newBaseName.equals(finalBaseName)) {
                        return;
                    }
                    String newFileName = newBaseName + finalExtension;
                    document.setFileName(newFileName);
                    showLoading(true);
                    documentRepository.saveDocument(document, new DocumentCallback.UploadCallback() {
                        @Override
                        public void onSuccess(DocumentFB documentFB) {
                            if (!isAdded()) {
                                return;
                            }
                            showLoading(false);
                            updateDocumentInCache(documentFB);
                            applyFilters();
                            Toast.makeText(getContext(), "Đã đổi tên thành: " + newFileName, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProgress(int progressPercentage) {
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (!isAdded()) {
                                return;
                            }
                            showLoading(false);
                            Toast.makeText(getContext(), resolveErrorMessage(e, "Đổi tên thất bại"), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyFilters() {
        if (adapter == null || folderAdapter == null) {
            return;
        }
        List<FolderItem> filteredFolders = new ArrayList<>();
        for (FolderItem folder : allFolders) {
            if (matchesFolderFilter(folder)) {
                filteredFolders.add(folder);
            }
        }
        List<DocumentFB> filteredDocuments = new ArrayList<>();
        for (DocumentFB document : allDocuments) {
            if (matchesDocumentFilter(document)) {
                filteredDocuments.add(document);
            }
        }
        folderAdapter.submitList(filteredFolders);
        adapter.submitList(filteredDocuments);
        updateFolderSectionVisibility(filteredFolders);
        updateStatus(filteredFolders, filteredDocuments);
    }

    private boolean matchesFolderFilter(FolderItem folderItem) {
        if (selectedFolderName != null) {
            return false;
        }
        if (currentFilterOption != FilterOption.ALL && currentFilterOption != FilterOption.FOLDERS_ONLY) {
            return false;
        }
        return containsQuery(folderItem.getName());
    }

    private boolean matchesDocumentFilter(DocumentFB document) {
        if (!containsQuery(document.getFileName())) {
            return false;
        }
        if (selectedFolderName != null) {
            return selectedFolderName.equals(getAssignedFolderName(document.getId()));
        }
        switch (currentFilterOption) {
            case ALL:
            case FILES_ONLY:
                return true;
            case PDF:
                return document.getFileType() == FileType.PDF;
            case WORD:
                return document.getFileType() == FileType.WORD;
            case EXCEL:
                return document.getFileType() == FileType.EXCEL;
            case IMAGE:
                return document.getFileType() == FileType.IMAGE;
            case OTHER:
                return document.getFileType() == FileType.OTHER;
            default:
                return false;
        }
    }

    private boolean containsQuery(String source) {
        if (currentSearchQuery.isEmpty()) {
            return true;
        }
        String normalizedSource = source == null ? "" : source.toLowerCase(Locale.getDefault());
        String normalizedQuery = currentSearchQuery.toLowerCase(Locale.getDefault());
        return normalizedSource.contains(normalizedQuery);
    }

    private void updateFolderSectionVisibility(List<FolderItem> filteredFolders) {
        if (foldersLabelView != null) {
            if (selectedFolderName != null) {
                foldersLabelView.setVisibility(View.VISIBLE);
                foldersLabelView.setText("Đang xem: " + selectedFolderName);
            } else {
                foldersLabelView.setText("Thư mục");
                foldersLabelView.setVisibility(filteredFolders.isEmpty() ? View.GONE : View.VISIBLE);
            }
        }
        if (backFolderButton != null) {
            backFolderButton.setVisibility(selectedFolderName != null ? View.VISIBLE : View.GONE);
        }
        if (foldersRecyclerView != null) {
            foldersRecyclerView.setVisibility(selectedFolderName != null || filteredFolders.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void updateStatus(List<FolderItem> filteredFolders, List<DocumentFB> filteredDocuments) {
        if (selectedFolderName != null) {
            showStatus(filteredDocuments.isEmpty()
                    ? "Thư mục này chưa có file phù hợp."
                    : "Đang xem các file trong thư mục: " + selectedFolderName);
            return;
        }
        if (allDocuments.isEmpty() && allFolders.isEmpty()) {
            showStatus("Chưa có file hoặc thư mục nào.");
            return;
        }
        if (filteredFolders.isEmpty() && filteredDocuments.isEmpty()) {
            showStatus("Không tìm thấy kết quả phù hợp với bộ lọc hiện tại.");
            return;
        }
        if (filteredDocuments.isEmpty() && currentFilterOption != FilterOption.FOLDERS_ONLY && !filteredFolders.isEmpty()) {
            showStatus("Không có file phù hợp, nhưng vẫn còn thư mục hiển thị.");
            return;
        }
        hideStatus();
    }

    private List<FolderItem> readFolders() {
        SharedPreferences preferences = requireContext().getSharedPreferences(FOLDER_PREFS, Context.MODE_PRIVATE);
        Set<String> folderSet = preferences.getStringSet(FOLDER_KEY, null);
        List<FolderItem> folders = new ArrayList<>();
        if (folderSet == null || folderSet.isEmpty()) {
            long now = System.currentTimeMillis();
            folders.add(new FolderItem("Công việc", now));
            folders.add(new FolderItem("Cá nhân", now - 1000));
            folders.add(new FolderItem("Dự án", now - 2000));
            folders.add(new FolderItem("Lưu trữ", now - 3000));
            return folders;
        }
        for (String entry : folderSet) {
            String[] parts = entry.split("\\|", 2);
            String name = parts.length > 0 ? parts[0].trim() : "";
            long createdAt = System.currentTimeMillis();
            if (parts.length == 2) {
                try {
                    createdAt = Long.parseLong(parts[1]);
                } catch (NumberFormatException ignored) {
                }
            }
            if (!name.isEmpty()) {
                folders.add(new FolderItem(name, createdAt));
            }
        }
        Collections.sort(folders, (first, second) -> Long.compare(second.getCreatedAt(), first.getCreatedAt()));
        return folders;
    }

    private boolean saveFolder(String folderName) {
        SharedPreferences preferences = requireContext().getSharedPreferences(FOLDER_PREFS, Context.MODE_PRIVATE);
        Set<String> existingEntries = preferences.getStringSet(FOLDER_KEY, null);
        Set<String> writableEntries = new HashSet<>();
        if (existingEntries != null) {
            writableEntries.addAll(existingEntries);
        } else {
            for (FolderItem folder : allFolders) {
                writableEntries.add(folder.getName() + "|" + folder.getCreatedAt());
            }
        }
        for (String entry : writableEntries) {
            String[] parts = entry.split("\\|", 2);
            if (parts.length > 0 && parts[0].equalsIgnoreCase(folderName)) {
                return false;
            }
        }
        writableEntries.add(folderName + "|" + System.currentTimeMillis());
        preferences.edit().putStringSet(FOLDER_KEY, writableEntries).apply();
        return true;
    }

    private boolean renameFolder(String oldName, String newName) {
        SharedPreferences preferences = requireContext().getSharedPreferences(FOLDER_PREFS, Context.MODE_PRIVATE);
        Set<String> existingEntries = preferences.getStringSet(FOLDER_KEY, null);
        if (existingEntries == null) {
            return false;
        }
        Set<String> writableEntries = new HashSet<>();
        boolean found = false;
        for (String entry : existingEntries) {
            String[] parts = entry.split("\\|", 2);
            String name = parts.length > 0 ? parts[0] : "";
            String createdAt = parts.length > 1 ? parts[1] : String.valueOf(System.currentTimeMillis());
            if (name.equalsIgnoreCase(newName) && !name.equalsIgnoreCase(oldName)) {
                return false;
            }
            if (name.equalsIgnoreCase(oldName)) {
                writableEntries.add(newName + "|" + createdAt);
                found = true;
            } else {
                writableEntries.add(entry);
            }
        }
        if (!found) {
            return false;
        }
        preferences.edit().putStringSet(FOLDER_KEY, writableEntries).apply();
        renameFolderAssignments(oldName, newName);
        return true;
    }

    private void deleteFolder(String folderName) {
        SharedPreferences preferences = requireContext().getSharedPreferences(FOLDER_PREFS, Context.MODE_PRIVATE);
        Set<String> existingEntries = preferences.getStringSet(FOLDER_KEY, null);
        Set<String> writableEntries = new HashSet<>();
        if (existingEntries != null) {
            for (String entry : existingEntries) {
                String[] parts = entry.split("\\|", 2);
                String name = parts.length > 0 ? parts[0] : "";
                if (!name.equalsIgnoreCase(folderName)) {
                    writableEntries.add(entry);
                }
            }
        }
        preferences.edit().putStringSet(FOLDER_KEY, writableEntries).apply();
        removeAssignmentsForFolder(folderName);
    }

    private void assignDocumentToFolder(String documentId, String folderName) {
        if (documentId == null || folderName == null) {
            return;
        }
        SharedPreferences preferences = requireContext().getSharedPreferences(ASSIGN_PREFS, Context.MODE_PRIVATE);
        Set<String> existingEntries = preferences.getStringSet(ASSIGN_KEY, null);
        Set<String> writableEntries = new HashSet<>();
        if (existingEntries != null) {
            for (String entry : existingEntries) {
                String[] parts = entry.split("\\|", 2);
                if (parts.length > 0 && !documentId.equals(parts[0])) {
                    writableEntries.add(entry);
                }
            }
        }
        writableEntries.add(documentId + "|" + folderName);
        preferences.edit().putStringSet(ASSIGN_KEY, writableEntries).apply();
    }

    private void removeDocumentFolderAssignment(String documentId) {
        if (documentId == null) {
            return;
        }
        SharedPreferences preferences = requireContext().getSharedPreferences(ASSIGN_PREFS, Context.MODE_PRIVATE);
        Set<String> existingEntries = preferences.getStringSet(ASSIGN_KEY, null);
        Set<String> writableEntries = new HashSet<>();
        if (existingEntries != null) {
            for (String entry : existingEntries) {
                String[] parts = entry.split("\\|", 2);
                if (parts.length > 0 && !documentId.equals(parts[0])) {
                    writableEntries.add(entry);
                }
            }
        }
        preferences.edit().putStringSet(ASSIGN_KEY, writableEntries).apply();
    }

    private String getAssignedFolderName(String documentId) {
        if (documentId == null) {
            return null;
        }
        SharedPreferences preferences = requireContext().getSharedPreferences(ASSIGN_PREFS, Context.MODE_PRIVATE);
        Set<String> existingEntries = preferences.getStringSet(ASSIGN_KEY, null);
        if (existingEntries == null) {
            return null;
        }
        for (String entry : existingEntries) {
            String[] parts = entry.split("\\|", 2);
            if (parts.length == 2 && documentId.equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }

    private void renameFolderAssignments(String oldName, String newName) {
        SharedPreferences preferences = requireContext().getSharedPreferences(ASSIGN_PREFS, Context.MODE_PRIVATE);
        Set<String> existingEntries = preferences.getStringSet(ASSIGN_KEY, null);
        Set<String> writableEntries = new HashSet<>();
        if (existingEntries != null) {
            for (String entry : existingEntries) {
                String[] parts = entry.split("\\|", 2);
                if (parts.length == 2 && oldName.equalsIgnoreCase(parts[1])) {
                    writableEntries.add(parts[0] + "|" + newName);
                } else {
                    writableEntries.add(entry);
                }
            }
        }
        preferences.edit().putStringSet(ASSIGN_KEY, writableEntries).apply();
    }

    private void removeAssignmentsForFolder(String folderName) {
        SharedPreferences preferences = requireContext().getSharedPreferences(ASSIGN_PREFS, Context.MODE_PRIVATE);
        Set<String> existingEntries = preferences.getStringSet(ASSIGN_KEY, null);
        Set<String> writableEntries = new HashSet<>();
        if (existingEntries != null) {
            for (String entry : existingEntries) {
                String[] parts = entry.split("\\|", 2);
                if (parts.length == 2 && !folderName.equalsIgnoreCase(parts[1])) {
                    writableEntries.add(entry);
                }
            }
        }
        preferences.edit().putStringSet(ASSIGN_KEY, writableEntries).apply();
    }

    private void resetFileScreenState() {
        selectedFolderName = null;
        currentFilterOption = FilterOption.ALL;
        currentSearchQuery = "";
        if (searchEditText != null && searchEditText.getText() != null && searchEditText.getText().length() > 0) {
            searchEditText.setText("");
        }
    }

    private void updateDocumentInCache(DocumentFB updatedDocument) {
        for (int i = 0; i < allDocuments.size(); i++) {
            DocumentFB existing = allDocuments.get(i);
            if (existing.getId() != null && existing.getId().equals(updatedDocument.getId())) {
                allDocuments.set(i, updatedDocument);
                return;
            }
        }
        allDocuments.add(0, updatedDocument);
    }

    private void removeDocumentFromCache(String documentId) {
        for (int i = 0; i < allDocuments.size(); i++) {
            DocumentFB existing = allDocuments.get(i);
            if (existing.getId() != null && existing.getId().equals(documentId)) {
                allDocuments.remove(i);
                return;
            }
        }
    }

    private String buildCloudSyncStatusMessage(String localSuccessPrefix) {
        return documentRepository.isCloudSyncConfigured()
                ? localSuccessPrefix + " Cloudinary sẽ đồng bộ nền."
                : localSuccessPrefix + " Cloud sync đang tắt vì chưa cấu hình Cloudinary.";
    }

    private String resolveErrorMessage(Exception e, String fallback) {
        return e != null && e.getMessage() != null && !e.getMessage().trim().isEmpty()
                ? e.getMessage()
                : fallback;
    }

    private EditText buildInput(String hint) {
        EditText input = new EditText(requireContext());
        if (hint != null) {
            input.setHint(hint);
        }
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        return input;
    }
}

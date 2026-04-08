package hcmute.edu.vn.documentfileeditor.Fragment;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Activity.DocumentEditorActivity;
import hcmute.edu.vn.documentfileeditor.Activity.ExcelEditorActivity;
import hcmute.edu.vn.documentfileeditor.Activity.OcrActivity;
import hcmute.edu.vn.documentfileeditor.Activity.ScanActivity;
import hcmute.edu.vn.documentfileeditor.Activity.TranslateActivity;
import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Dao.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private DocumentRepository documentRepository;
    private View rootView;
    private RecentCardBinding[] recentBindings;
    private boolean hasShownLiveDocuments;
    private TextView recentStatusView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable android.os.Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        documentRepository = DocumentRepository.getInstance(requireContext());

        setupQuickActions(rootView);
        setupRecentFiles(rootView);
        loadRecentFiles();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && documentRepository != null && rootView != null) {
            loadRecentFiles();
        }
    }

    private void setupQuickActions(View view) {
        View actionScan = view.findViewById(R.id.card_action_scan);
        View actionOcr = view.findViewById(R.id.card_action_ocr);
        View actionTranslate = view.findViewById(R.id.card_action_translate);

        if (actionScan != null) {
            actionScan.setOnClickListener(v -> startActivity(new Intent(getActivity(), ScanActivity.class)));
        }

        if (actionOcr != null) {
            actionOcr.setOnClickListener(v -> startActivity(new Intent(getActivity(), OcrActivity.class)));
        }

        if (actionTranslate != null) {
            actionTranslate.setOnClickListener(v -> startActivity(new Intent(getActivity(), TranslateActivity.class)));
        }
    }

    private void setupRecentFiles(View view) {
        recentBindings = new RecentCardBinding[]{
                new RecentCardBinding(
                        view.findViewById(R.id.card_recent_1),
                        view.findViewById(R.id.recent_icon_container_1),
                        view.findViewById(R.id.recent_icon_1),
                        view.findViewById(R.id.tv_recent_name_1),
                        view.findViewById(R.id.tv_recent_meta_1)
                ),
                new RecentCardBinding(
                        view.findViewById(R.id.card_recent_2),
                        view.findViewById(R.id.recent_icon_container_2),
                        view.findViewById(R.id.recent_icon_2),
                        view.findViewById(R.id.tv_recent_name_2),
                        view.findViewById(R.id.tv_recent_meta_2)
                ),
                new RecentCardBinding(
                        view.findViewById(R.id.card_recent_3),
                        view.findViewById(R.id.recent_icon_container_3),
                        view.findViewById(R.id.recent_icon_3),
                        view.findViewById(R.id.tv_recent_name_3),
                        view.findViewById(R.id.tv_recent_meta_3)
                )
        };
        recentStatusView = view.findViewById(R.id.tv_recent_status);
        setRecentCardsVisible(false);
        showRecentStatus(false);

        View viewAllButton = view.findViewById(R.id.btn_view_all_recent);
        if (viewAllButton != null) {
            viewAllButton.setOnClickListener(v -> openDocumentsTab());
        }
    }

    private void loadRecentFiles() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            setRecentCardsVisible(false);
            showRecentStatus(true);
            return;
        }

        List<DocumentFB> cachedDocuments = documentRepository.getCachedDocuments(user.getUid());
        if (!cachedDocuments.isEmpty()) {
            bindRecentDocuments(cachedDocuments);
            hasShownLiveDocuments = true;
        } else if (!hasShownLiveDocuments) {
            setRecentCardsVisible(false);
            showRecentStatus(true);
        }

        documentRepository.getDocuments(user.getUid(), new DocumentCallback.GetDocumentsCallback() {
            @Override
            public void onSuccess(List<DocumentFB> documents) {
                if (!isAdded()) {
                    return;
                }
                if (documents.isEmpty() && !hasShownLiveDocuments) {
                    setRecentCardsVisible(false);
                    showRecentStatus(true);
                    return;
                }
                if (!documents.isEmpty()) {
                    hasShownLiveDocuments = true;
                }
                bindRecentDocuments(documents);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Could not load recent files", e);
                if (!isAdded()) {
                    return;
                }
                showRecentStatus(!hasShownLiveDocuments);
            }
        });
    }

    private void bindRecentDocuments(List<DocumentFB> documents) {
        List<DocumentFB> sortedDocuments = new ArrayList<>(documents);
        sortedDocuments.sort((left, right) -> Long.compare(getSortTime(right), getSortTime(left)));

        if (!sortedDocuments.isEmpty()) {
            hasShownLiveDocuments = true;
        }
        showRecentStatus(sortedDocuments.isEmpty());

        for (int i = 0; i < recentBindings.length; i++) {
            if (i < sortedDocuments.size()) {
                recentBindings[i].cardView.setVisibility(View.VISIBLE);
                bindDocumentCard(recentBindings[i], sortedDocuments.get(i));
            } else {
                recentBindings[i].cardView.setVisibility(View.GONE);
            }
        }
    }

    private long getSortTime(DocumentFB document) {
        if (document.getLastModified() > 0L) {
            return document.getLastModified();
        }
        return document.getCreatedDate();
    }

    private void bindDocumentCard(RecentCardBinding binding, DocumentFB document) {
        binding.nameView.setText(document.getFileName());
        binding.metaView.setText(buildMeta(document));
        bindFileType(binding, document.getFileType());
        binding.cardView.setOnClickListener(v -> openDocument(document));
    }

    private void setRecentCardsVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        for (RecentCardBinding binding : recentBindings) {
            binding.cardView.setVisibility(visibility);
        }
    }

    private void showRecentStatus(boolean show) {
        if (recentStatusView != null) {
            recentStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private String buildMeta(DocumentFB document) {
        String typeText = buildTypeLabel(document.getFileType());
        String dateText = document.getLastModified() > 0L
                ? DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(document.getLastModified()))
                : "Unknown date";
        String syncText = (document.getCloudStorageUrl() == null || document.getCloudStorageUrl().isEmpty())
                ? "Pending sync"
                : "Synced";
        return typeText + " | " + dateText + " | " + syncText;
    }

    private String buildTypeLabel(FileType fileType) {
        if (fileType == FileType.EXCEL) {
            return "Excel";
        }
        if (fileType == FileType.PDF) {
            return "PDF";
        }
        if (fileType == FileType.IMAGE) {
            return "Image";
        }
        if (fileType == FileType.OTHER) {
            return "File";
        }
        return "Word";
    }

    private void bindFileType(RecentCardBinding binding, FileType fileType) {
        if (fileType == FileType.EXCEL) {
            binding.iconContainer.setCardBackgroundColor(requireContext().getColor(R.color.green_100));
            binding.iconView.setImageResource(R.drawable.ic_sheet);
            binding.iconView.setColorFilter(requireContext().getColor(R.color.green_600));
            return;
        }

        if (fileType == FileType.PDF) {
            binding.iconContainer.setCardBackgroundColor(requireContext().getColor(R.color.red_100));
            binding.iconView.setImageResource(R.drawable.ic_file);
            binding.iconView.setColorFilter(requireContext().getColor(R.color.red_600));
            return;
        }

        binding.iconContainer.setCardBackgroundColor(requireContext().getColor(R.color.blue_100));
        binding.iconView.setImageResource(R.drawable.ic_file_text);
        binding.iconView.setColorFilter(requireContext().getColor(R.color.blue_600));
    }

    private void openDocument(DocumentFB document) {
        if ((document.getLocalPath() == null || document.getLocalPath().isEmpty())
                && document.getCloudStorageUrl() != null
                && !document.getCloudStorageUrl().isEmpty()) {
            documentRepository.downloadIfNeeded(requireContext(), document, new DocumentCallback.DownloadCallback() {
                @Override
                public void onSuccess(String localPath) {
                    if (!isAdded()) {
                        return;
                    }
                    document.setLocalPath(localPath);
                    launchEditor(document);
                }

                @Override
                public void onFailure(Exception e) {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(), "Could not download " + document.getFileName(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Could not download recent document", e);
                }
            });
            return;
        }

        launchEditor(document);
    }

    private void launchEditor(DocumentFB document) {
        Class<?> targetActivity = document.getFileType() == FileType.EXCEL
                ? ExcelEditorActivity.class
                : DocumentEditorActivity.class;

        Intent intent = new Intent(requireContext(), targetActivity);
        intent.putExtra(DocumentEditorActivity.EXTRA_DOCUMENT_ID, document.getId());
        intent.putExtra(DocumentEditorActivity.EXTRA_DOCUMENT_NAME, document.getFileName());
        intent.putExtra(DocumentEditorActivity.EXTRA_LOCAL_PATH, document.getLocalPath());
        intent.putExtra(DocumentEditorActivity.EXTRA_CLOUD_URL, document.getCloudStorageUrl());
        intent.putExtra(
                DocumentEditorActivity.EXTRA_FILE_TYPE,
                document.getFileType() != null ? document.getFileType().name() : FileType.WORD.name()
        );
        startActivity(intent);
    }

    private void openDocumentsTab() {
        if (getActivity() == null) {
            return;
        }

        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_documents);
        }
    }

    private static class RecentCardBinding {
        final View cardView;
        final MaterialCardView iconContainer;
        final ImageView iconView;
        final TextView nameView;
        final TextView metaView;

        RecentCardBinding(View cardView,
                          MaterialCardView iconContainer,
                          ImageView iconView,
                          TextView nameView,
                          TextView metaView) {
            this.cardView = cardView;
            this.iconContainer = iconContainer;
            this.iconView = iconView;
            this.nameView = nameView;
            this.metaView = metaView;
        }
    }
}

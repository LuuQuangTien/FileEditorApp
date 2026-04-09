package hcmute.edu.vn.documentfileeditor.Fragment;

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

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Activity.OcrActivity;
import hcmute.edu.vn.documentfileeditor.Activity.ScanActivity;
import hcmute.edu.vn.documentfileeditor.Activity.TranslateActivity;
import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.AuthService;
import hcmute.edu.vn.documentfileeditor.Service.DocumentService;
import hcmute.edu.vn.documentfileeditor.Util.FileTypeHelper;
import hcmute.edu.vn.documentfileeditor.Util.NavigationHelper;
import hcmute.edu.vn.documentfileeditor.Util.ThemeManager;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private DocumentRepository documentRepository;
    private DocumentService documentService;
    private AuthService authService;
    private View rootView;
    private RecentCardBinding[] recentBindings;
    private boolean hasShownLiveDocuments;
    private TextView recentStatusView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable android.os.Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        documentRepository = DocumentRepository.getInstance(requireContext());
        documentService = new DocumentService();
        authService = new AuthService();

        setupThemeButton(rootView);
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
            actionScan.setOnClickListener(v -> startActivity(new android.content.Intent(getActivity(), ScanActivity.class)));
        }

        if (actionOcr != null) {
            actionOcr.setOnClickListener(v -> startActivity(new android.content.Intent(getActivity(), OcrActivity.class)));
        }

        if (actionTranslate != null) {
            actionTranslate.setOnClickListener(v -> startActivity(new android.content.Intent(getActivity(), TranslateActivity.class)));
        }
    }

    private void setupThemeButton(View view) {
        View themeButton = view.findViewById(R.id.btn_theme);
        ImageView themeIcon = view.findViewById(R.id.iv_theme_icon);
        updateThemeButtonIcon(themeIcon);

        if (themeButton != null) {
            themeButton.setOnClickListener(v -> {
                ThemeManager.toggleDarkMode(requireContext());
                updateThemeButtonIcon(themeIcon);
            });
        }
    }

    private void updateThemeButtonIcon(@Nullable ImageView themeIcon) {
        if (themeIcon == null || !isAdded()) {
            return;
        }

        themeIcon.setImageResource(
                ThemeManager.isDarkModeEnabled(requireContext())
                        ? R.drawable.ic_moon
                        : R.drawable.ic_sun
        );
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
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            setRecentCardsVisible(false);
            showRecentStatus(true);
            return;
        }

        List<DocumentFB> cachedDocuments = documentRepository.getCachedDocuments(userId);
        if (!cachedDocuments.isEmpty()) {
            bindRecentDocuments(cachedDocuments);
            hasShownLiveDocuments = true;
        } else if (!hasShownLiveDocuments) {
            setRecentCardsVisible(false);
            showRecentStatus(true);
        }

        documentRepository.getDocuments(userId, new DocumentCallback.GetDocumentsCallback() {
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
        binding.metaView.setText(documentService.buildMeta(document));
        FileTypeHelper.bindFileType(requireContext(), binding.iconContainer, binding.iconView, document.getFileType());
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
                    NavigationHelper.launchEditor(requireContext(), document);
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

        NavigationHelper.launchEditor(requireContext(), document);
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

package hcmute.edu.vn.documentfileeditor.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.DocumentService;
import hcmute.edu.vn.documentfileeditor.Util.FileTypeHelper;

/**
 * RecyclerView Adapter for displaying live documents from the repository.
 * Uses FileTypeHelper and DocumentService to eliminate duplicate logic.
 */
public class LiveDocumentAdapter extends RecyclerView.Adapter<LiveDocumentAdapter.DocumentViewHolder> {
    public interface OnDocumentClickListener {
        void onDocumentClick(DocumentFB document);
    }

    public interface OnDocumentMoreClickListener {
        void onDocumentMoreClick(View anchor, DocumentFB document);
    }

    private final List<DocumentFB> documents = new ArrayList<>();
    private final OnDocumentClickListener onDocumentClickListener;
    private final OnDocumentMoreClickListener onDocumentMoreClickListener;
    private final DocumentService documentService;

    public LiveDocumentAdapter(OnDocumentClickListener onDocumentClickListener,
                               OnDocumentMoreClickListener onDocumentMoreClickListener) {
        this.onDocumentClickListener = onDocumentClickListener;
        this.onDocumentMoreClickListener = onDocumentMoreClickListener;
        this.documentService = new DocumentService();
    }

    public void submitList(List<DocumentFB> newDocuments) {
        documents.clear();
        documents.addAll(newDocuments);
        notifyDataSetChanged();
    }

    public void upsertItem(DocumentFB document) {
        for (int i = 0; i < documents.size(); i++) {
            DocumentFB existing = documents.get(i);
            if (existing.getId() != null && existing.getId().equals(document.getId())) {
                documents.set(i, document);
                notifyItemChanged(i);
                return;
            }
        }
        documents.add(0, document);
        notifyItemInserted(0);
    }

    public void removeItem(String documentId) {
        for (int i = 0; i < documents.size(); i++) {
            DocumentFB existing = documents.get(i);
            if (existing.getId() != null && existing.getId().equals(documentId)) {
                documents.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document_live, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        holder.bind(documents.get(position), onDocumentClickListener, onDocumentMoreClickListener, documentService);
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView iconContainer;
        private final ImageView fileIcon;
        private final TextView fileName;
        private final TextView fileMeta;
        private final ImageView moreButton;

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.file_icon_container);
            fileIcon = itemView.findViewById(R.id.iv_file_icon);
            fileName = itemView.findViewById(R.id.tv_file_name);
            fileMeta = itemView.findViewById(R.id.tv_file_meta);
            moreButton = itemView.findViewById(R.id.iv_more);
        }

        void bind(DocumentFB document,
                  OnDocumentClickListener onDocumentClickListener,
                  OnDocumentMoreClickListener onDocumentMoreClickListener,
                  DocumentService documentService) {
            fileName.setText(document.getFileName());

            // Use DocumentService for meta (includes size info)
            String sizeText = documentService.formatFileSize(document.getSizeBytes());
            String metaText = documentService.buildMeta(document);
            fileMeta.setText(sizeText + " | " + metaText);

            // Use FileTypeHelper for consistent icon binding
            FileTypeHelper.bindFileType(itemView.getContext(), iconContainer, fileIcon, document.getFileType());

            itemView.setOnClickListener(v -> onDocumentClickListener.onDocumentClick(document));
            moreButton.setOnClickListener(v -> onDocumentMoreClickListener.onDocumentMoreClick(v, document));
        }
    }
}

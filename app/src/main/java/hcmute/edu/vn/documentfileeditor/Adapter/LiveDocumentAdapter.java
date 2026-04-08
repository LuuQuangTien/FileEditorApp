package hcmute.edu.vn.documentfileeditor.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.R;

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

    public LiveDocumentAdapter(OnDocumentClickListener onDocumentClickListener,
                               OnDocumentMoreClickListener onDocumentMoreClickListener) {
        this.onDocumentClickListener = onDocumentClickListener;
        this.onDocumentMoreClickListener = onDocumentMoreClickListener;
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

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document_live, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        holder.bind(documents.get(position), onDocumentClickListener, onDocumentMoreClickListener);
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
                  OnDocumentMoreClickListener onDocumentMoreClickListener) {
            fileName.setText(document.getFileName());
            fileMeta.setText(buildMeta(document));
            bindFileType(document.getFileType());
            itemView.setOnClickListener(v -> onDocumentClickListener.onDocumentClick(document));
            moreButton.setOnClickListener(v -> onDocumentMoreClickListener.onDocumentMoreClick(v, document));
        }

        private String buildMeta(DocumentFB document) {
            String sizeText = formatFileSize(document.getSizeBytes());
            String dateText = document.getLastModified() > 0
                    ? DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(document.getLastModified()))
                    : "Unknown date";
            String syncText = (document.getCloudStorageUrl() == null || document.getCloudStorageUrl().isEmpty())
                    ? "Pending sync"
                    : "Synced";
            return sizeText + " | " + dateText + " | " + syncText;
        }

        private String formatFileSize(long sizeBytes) {
            if (sizeBytes <= 0) {
                return "Unknown size";
            }
            if (sizeBytes < 1024) {
                return sizeBytes + " B";
            }
            double sizeKb = sizeBytes / 1024.0;
            if (sizeKb < 1024) {
                return String.format(Locale.US, "%.1f KB", sizeKb);
            }
            double sizeMb = sizeKb / 1024.0;
            if (sizeMb < 1024) {
                return String.format(Locale.US, "%.1f MB", sizeMb);
            }
            return String.format(Locale.US, "%.1f GB", sizeMb / 1024.0);
        }

        private void bindFileType(FileType fileType) {
            if (fileType == FileType.EXCEL) {
                iconContainer.setCardBackgroundColor(itemView.getContext().getColor(R.color.green_100));
                fileIcon.setImageResource(R.drawable.ic_sheet);
                fileIcon.setColorFilter(itemView.getContext().getColor(R.color.green_600));
                return;
            }

            if (fileType == FileType.PDF) {
                iconContainer.setCardBackgroundColor(itemView.getContext().getColor(R.color.red_100));
                fileIcon.setImageResource(R.drawable.ic_file);
                fileIcon.setColorFilter(itemView.getContext().getColor(R.color.red_600));
                return;
            }

            iconContainer.setCardBackgroundColor(itemView.getContext().getColor(R.color.blue_100));
            fileIcon.setImageResource(R.drawable.ic_file_text);
            fileIcon.setColorFilter(itemView.getContext().getColor(R.color.blue_600));
        }
    }
}

package hcmute.edu.vn.documentfileeditor.Adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.DocumentService;

public class PdfFileAdapter extends RecyclerView.Adapter<PdfFileAdapter.PdfViewHolder> {

    private final List<Uri> pdfUris;
    private final Context context;
    private final DocumentService documentService;
    private final OnItemRemoveListener removeListener;

    public interface OnItemRemoveListener {
        void onRemove(int position);
    }

    public PdfFileAdapter(Context context, List<Uri> pdfUris, OnItemRemoveListener removeListener) {
        this.context = context;
        this.pdfUris = pdfUris;
        this.removeListener = removeListener;
        this.documentService = new DocumentService();
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf_file, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        Uri uri = pdfUris.get(position);
        String name = documentService.resolveFileName(context, uri);
        holder.tvFileName.setText(name);

        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return pdfUris.size();
    }

    static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        ImageView btnRemove;
        ImageView ivDragHandle;

        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            btnRemove = itemView.findViewById(R.id.btn_remove);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
        }
    }
}

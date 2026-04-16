package hcmute.edu.vn.documentfileeditor.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Model.Entity.FolderItem;
import hcmute.edu.vn.documentfileeditor.R;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {
    public interface OnFolderClickListener {
        void onFolderClick(FolderItem folderItem);
    }

    public interface OnFolderMoreClickListener {
        void onFolderMoreClick(View anchor, FolderItem folderItem);
    }

    private final List<FolderItem> folders = new ArrayList<>();
    private final OnFolderClickListener onFolderClickListener;
    private final OnFolderMoreClickListener onFolderMoreClickListener;

    public FolderAdapter(OnFolderClickListener onFolderClickListener,
                         OnFolderMoreClickListener onFolderMoreClickListener) {
        this.onFolderClickListener = onFolderClickListener;
        this.onFolderMoreClickListener = onFolderMoreClickListener;
    }

    public void submitList(List<FolderItem> newFolders) {
        folders.clear();
        folders.addAll(newFolders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        holder.bind(folders.get(position), onFolderClickListener, onFolderMoreClickListener);
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        private final TextView folderName;
        private final TextView folderMeta;
        private final ImageView moreButton;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.tv_folder_name);
            folderMeta = itemView.findViewById(R.id.tv_folder_meta);
            moreButton = itemView.findViewById(R.id.iv_folder_more);
        }

        void bind(FolderItem folderItem,
                  OnFolderClickListener onFolderClickListener,
                  OnFolderMoreClickListener onFolderMoreClickListener) {
            folderName.setText(folderItem.getName());
            String createdDate = DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(new Date(folderItem.getCreatedAt()));
            folderMeta.setText("Tạo ngày " + createdDate);
            itemView.setOnClickListener(v -> onFolderClickListener.onFolderClick(folderItem));
            moreButton.setOnClickListener(v -> onFolderMoreClickListener.onFolderMoreClick(v, folderItem));
        }
    }
}

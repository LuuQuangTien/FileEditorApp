package hcmute.edu.vn.documentfileeditor.Util;

import android.content.Context;
import android.widget.ImageView;

import com.google.android.material.card.MaterialCardView;

import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.R;

/**
 * Utility class providing consistent FileType-related UI mappings.
 * Eliminates duplicate bindFileType() / buildTypeLabel() code across
 * LiveDocumentAdapter, HomeFragment, and other UI components.
 */
public final class FileTypeHelper {

    private FileTypeHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Binds the correct icon, container background, and tint color for a given FileType.
     */
    public static void bindFileType(Context context, MaterialCardView iconContainer,
                                    ImageView iconView, FileType fileType) {
        if (fileType == FileType.EXCEL) {
            iconContainer.setCardBackgroundColor(context.getColor(R.color.green_100));
            iconView.setImageResource(R.drawable.ic_sheet);
            iconView.setColorFilter(context.getColor(R.color.green_600));
            return;
        }

        if (fileType == FileType.PDF) {
            iconContainer.setCardBackgroundColor(context.getColor(R.color.red_100));
            iconView.setImageResource(R.drawable.ic_file);
            iconView.setColorFilter(context.getColor(R.color.red_600));
            return;
        }

        // Default: WORD, IMAGE, OTHER
        iconContainer.setCardBackgroundColor(context.getColor(R.color.blue_100));
        iconView.setImageResource(R.drawable.ic_file_text);
        iconView.setColorFilter(context.getColor(R.color.blue_600));
    }

    /**
     * Returns a human-readable label for the given FileType.
     */
    public static String getTypeLabel(FileType fileType) {
        if (fileType == FileType.EXCEL) return "Excel";
        if (fileType == FileType.PDF) return "PDF";
        if (fileType == FileType.IMAGE) return "Image";
        if (fileType == FileType.OTHER) return "File";
        return "Word";
    }
}

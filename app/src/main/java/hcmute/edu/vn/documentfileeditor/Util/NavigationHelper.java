package hcmute.edu.vn.documentfileeditor.Util;

import android.content.Context;
import android.content.Intent;

import hcmute.edu.vn.documentfileeditor.Activity.DocumentEditorActivity;
import hcmute.edu.vn.documentfileeditor.Activity.ExcelEditorActivity;
import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;

/**
 * Utility class for navigation-related operations shared across
 * HomeFragment, DocumentsFragment, and other components.
 * Eliminates duplicate launchEditor() implementations.
 */
public final class NavigationHelper {

    private NavigationHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Launches the appropriate editor activity for the given document.
     * Routes to ExcelEditorActivity for EXCEL files, DocumentEditorActivity for all others.
     */
    public static void launchEditor(Context context, DocumentFB document) {
        if (document.getFileType() == FileType.PDF) {
            openPdfExternal(context, document);
            return;
        }

        Class<?> targetActivity = document.getFileType() == FileType.EXCEL
                ? ExcelEditorActivity.class
                : DocumentEditorActivity.class;

        Intent intent = new Intent(context, targetActivity);
        intent.putExtra(DocumentEditorActivity.EXTRA_DOCUMENT_ID, document.getId());
        intent.putExtra(DocumentEditorActivity.EXTRA_DOCUMENT_NAME, document.getFileName());
        intent.putExtra(DocumentEditorActivity.EXTRA_LOCAL_PATH, document.getLocalPath());
        intent.putExtra(DocumentEditorActivity.EXTRA_CLOUD_URL, document.getCloudStorageUrl());
        intent.putExtra(
                DocumentEditorActivity.EXTRA_FILE_TYPE,
                document.getFileType() != null ? document.getFileType().name() : FileType.WORD.name()
        );
        context.startActivity(intent);
    }

    private static void openPdfExternal(Context context, DocumentFB document) {
        try {
            java.io.File pdfFile = new java.io.File(document.getLocalPath());
            if (!pdfFile.exists()) {
                android.widget.Toast.makeText(context, "File không tồn tại", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    context, context.getPackageName() + ".fileprovider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            android.widget.Toast.makeText(context, "Không có ứng dụng để mở PDF", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}

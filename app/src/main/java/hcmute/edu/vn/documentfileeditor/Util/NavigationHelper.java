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
}

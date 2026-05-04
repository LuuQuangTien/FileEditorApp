package hcmute.edu.vn.documentfileeditor.Service;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;

public class DocumentService {
    public String readTextFromLocalFile(Context context, String localPath) {
        if (localPath == null || localPath.isEmpty()) {
            return null;
        }
        try {
            File file = new File(localPath);
            if (!file.exists()) {
                return null;
            }
            InputStream input = context.getContentResolver().openInputStream(Uri.fromFile(file));
            if (input == null) {
                return null;
            }
            byte[] bytes = new byte[(int) file.length()];
            int read = input.read(bytes);
            input.close();
            if (read <= 0) {
                return null;
            }
            return new String(bytes, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean saveTextToLocalFile(String localPath, String content) {
        if (localPath == null || localPath.isEmpty()) {
            return false;
        }
        try {
            FileOutputStream output = new FileOutputStream(new File(localPath), false);
            output.write(content.getBytes(StandardCharsets.UTF_8));
            output.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String buildFileName(String rawName, FileType fileType) {
        String trimmed = rawName == null ? "" : rawName.trim();
        if (trimmed.isEmpty()) {
            trimmed = fileType == FileType.EXCEL ? "New Spreadsheet" : "New Document";
        }

        String extension = fileType == FileType.EXCEL ? ".xlsx" : ".docx";
        String lowerName = trimmed.toLowerCase(Locale.US);
        if (!lowerName.endsWith(extension)) {
            trimmed += extension;
        }
        return trimmed;
    }

    public String resolveFileName(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        );
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        String displayName = cursor.getString(nameIndex);
                        if (displayName != null && !displayName.isEmpty()) {
                            return displayName;
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return "Imported file " + System.currentTimeMillis();
    }

    public FileType resolveFileType(String fileName, String mimeType) {
        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.US);
        String lowerMimeType = mimeType == null ? "" : mimeType.toLowerCase(Locale.US);

        if (lowerName.endsWith(".pdf") || lowerMimeType.contains("pdf")) {
            return FileType.PDF;
        }
        if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx") || lowerMimeType.contains("excel") || lowerMimeType.contains("spreadsheet")) {
            return FileType.EXCEL;
        }
        if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx") || lowerMimeType.contains("word") || lowerName.endsWith(".txt") || lowerMimeType.contains("text")) {
            return FileType.WORD;
        }
        if (lowerMimeType.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {
            return FileType.IMAGE;
        }
        return FileType.OTHER;
    }

    public String buildMeta(DocumentFB document) {
        String typeText = hcmute.edu.vn.documentfileeditor.Util.FileTypeHelper.getTypeLabel(document.getFileType());
        String dateText = document.getLastModified() > 0L
                ? DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(document.getLastModified()))
                : "Unknown date";
        String syncText = (document.getCloudStorageUrl() == null || document.getCloudStorageUrl().isEmpty())
                ? "Pending sync"
                : "Synced";
        return typeText + " | " + dateText + " | " + syncText;
    }

    public String formatFileSize(long sizeBytes) {
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
}

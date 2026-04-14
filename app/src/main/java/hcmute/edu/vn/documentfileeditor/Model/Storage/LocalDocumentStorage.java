package hcmute.edu.vn.documentfileeditor.Model.Storage;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LocalDocumentStorage {
    private static final String DOCUMENT_DIR = "documents";

    public String saveFile(Context context, Uri sourceUri, String fileName) {
        try {
            File file = getDocumentFile(context, fileName);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            InputStream input = context.getContentResolver().openInputStream(sourceUri);
            if (input == null) {
                return null;
            }

            FileOutputStream output = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }

            input.close();
            output.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String createFile(Context context, String fileName, String initialContent) {
        try {
            File file = getDocumentFile(context, fileName);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            FileOutputStream output = new FileOutputStream(file);
            if (initialContent != null) {
                output.write(initialContent.getBytes(StandardCharsets.UTF_8));
            }
            output.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public File getDocumentFile(Context context, String fileName) {
        return new File(new File(context.getFilesDir(), DOCUMENT_DIR), fileName);
    }

    public boolean deleteFile(String localPath) {
        if (localPath == null || localPath.isEmpty()) {
            return false;
        }
        File file = new File(localPath);
        return !file.exists() || file.delete();
    }
}

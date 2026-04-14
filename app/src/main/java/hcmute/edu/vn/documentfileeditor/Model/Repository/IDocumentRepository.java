package hcmute.edu.vn.documentfileeditor.Model.Repository;

import android.content.Context;
import android.net.Uri;

import java.util.List;

import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;

/**
 * Interface defining the contract for document repository operations.
 * Follows the Dependency Inversion Principle (DIP) - high-level modules
 * depend on this abstraction rather than concrete implementations.
 */
public interface IDocumentRepository {

    void uploadDocument(Context context, Uri sourceUri, DocumentFB documentMeta,
                        DocumentCallback.UploadCallback callback);

    void createDocument(Context context, DocumentFB documentMeta, String initialContent,
                        DocumentCallback.UploadCallback callback);

    void saveDocument(DocumentFB documentMeta, DocumentCallback.UploadCallback callback);

    void getDocuments(String userId, DocumentCallback.GetDocumentsCallback callback);

    void downloadIfNeeded(Context context, DocumentFB document,
                          DocumentCallback.DownloadCallback callback);

    void deleteDocument(DocumentFB document, DocumentCallback.SimpleCallback callback);

    void retrySync(DocumentFB document);

    List<DocumentFB> getCachedDocuments(String userId);
}

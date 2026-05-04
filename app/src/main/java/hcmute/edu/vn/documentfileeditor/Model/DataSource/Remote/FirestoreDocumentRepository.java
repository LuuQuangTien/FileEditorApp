package hcmute.edu.vn.documentfileeditor.Model.DataSource.Remote;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Service.CloudinaryStorageService;

public class FirestoreDocumentRepository {
    private static final String COLLECTION_PATH = "User_Documents";
    private static final String TAG = "FirestoreDocRepo";

    private final FirebaseFirestore db;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final ExecutorService networkExecutor;

    public FirestoreDocumentRepository() {
        db = FirebaseFirestore.getInstance();
        cloudinaryStorageService = new CloudinaryStorageService();
        networkExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean isCloudSyncConfigured() {
        return cloudinaryStorageService.isConfigured();
    }

    public void uploadDocument(Uri localFileUri, DocumentFB documentMeta, DocumentCallback.UploadCallback callback) {
        upsertDocument(localFileUri, documentMeta, callback, true);
    }

    public void syncDocument(Uri localFileUri, DocumentFB documentMeta, DocumentCallback.UploadCallback callback) {
        upsertDocument(localFileUri, documentMeta, callback, true);
    }

    public void downloadDocument(DocumentFB document, File targetFile, DocumentCallback.DownloadCallback callback) {
        if (document == null || document.getCloudStorageUrl() == null || document.getCloudStorageUrl().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Document khong co cloudStorageUrl"));
            return;
        }

        networkExecutor.execute(() -> {
            try {
                cloudinaryStorageService.downloadToFile(document.getCloudStorageUrl(), targetFile);
                callback.onSuccess(targetFile.getAbsolutePath());
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void getDocuments(String userId, DocumentCallback.GetDocumentsCallback callback) {
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentFB> documents = new ArrayList<>();
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        DocumentFB doc = snapshot.toObject(DocumentFB.class);
                        documents.add(doc);
                    }
                    callback.onSuccess(documents);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteDocument(DocumentFB document, DocumentCallback.SimpleCallback callback) {
        deleteFirestoreDocRef(document.getId(), callback);
    }

    private void upsertDocument(Uri localFileUri, DocumentFB documentMeta, DocumentCallback.UploadCallback callback, boolean keepExistingId) {
        if (localFileUri == null || documentMeta == null) {
            callback.onFailure(new IllegalArgumentException("URI hoac thong tin document khong hop le"));
            return;
        }

        String resolvedDocumentId = documentMeta.getId();
        if (!keepExistingId || resolvedDocumentId == null || resolvedDocumentId.isEmpty()) {
            resolvedDocumentId = db.collection(COLLECTION_PATH).document().getId();
            documentMeta.setId(resolvedDocumentId);
        }
        final String documentId = resolvedDocumentId;

        networkExecutor.execute(() -> {
            try {
                File localFile = resolveLocalFile(localFileUri);
                CloudinaryStorageService.UploadResult uploadResult = cloudinaryStorageService.uploadDocument(
                        localFile,
                        buildCloudinaryPublicId(documentMeta),
                        buildCloudinaryFolder(documentMeta)
                );

                long currentTime = System.currentTimeMillis();
                documentMeta.setCloudStorageUrl(uploadResult.getSecureUrl());
                if (documentMeta.getCreatedDate() == 0L) {
                    documentMeta.setCreatedDate(currentTime);
                }
                documentMeta.setLastModified(currentTime);

                db.collection(COLLECTION_PATH).document(documentId).set(documentMeta)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(documentMeta))
                        .addOnFailureListener(callback::onFailure);
            } catch (Exception e) {
                Log.e(TAG, "Cloudinary upload failed for " + documentMeta.getFileName(), e);
                callback.onFailure(e);
            }
        });
    }

    private File resolveLocalFile(Uri localFileUri) {
        String path = localFileUri.getPath();
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Khong the doc duong dan file local");
        }

        File file = new File(path);
        if (!file.exists()) {
            throw new IllegalArgumentException("Khong tim thay file local de sync");
        }
        return file;
    }

    private String buildCloudinaryFolder(DocumentFB documentMeta) {
        String userId = sanitizePathSegment(documentMeta.getUserId(), "anonymous");
        return "document_file_editor/" + userId;
    }

    private String buildCloudinaryPublicId(DocumentFB documentMeta) {
        String documentId = sanitizePathSegment(documentMeta.getId(), "document");
        String fileName = sanitizePathSegment(documentMeta.getFileName(), "file");
        return documentId + "_" + fileName;
    }

    private String sanitizePathSegment(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim().replaceAll("[\\\\/#?\\[\\]]", "_");
    }

    private void deleteFirestoreDocRef(String docId, DocumentCallback.SimpleCallback callback) {
        db.collection(COLLECTION_PATH).document(docId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}

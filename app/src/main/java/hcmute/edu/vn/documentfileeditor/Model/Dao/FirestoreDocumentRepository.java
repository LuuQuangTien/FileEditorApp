package hcmute.edu.vn.documentfileeditor.Model.Dao;

import android.net.Uri;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;

public class FirestoreDocumentRepository {
    private static final String COLLECTION_PATH = "User_Documents";

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    public FirestoreDocumentRepository() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
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

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            StorageReference ref = storage.getReferenceFromUrl(document.getCloudStorageUrl());
            ref.getFile(targetFile)
                    .addOnSuccessListener(task -> callback.onSuccess(targetFile.getAbsolutePath()))
                    .addOnFailureListener(e -> callback.onFailure(e instanceof Exception ? (Exception) e : new Exception(e)));
        } catch (Exception e) {
            callback.onFailure(e);
        }
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
        if (document.getCloudStorageUrl() != null && !document.getCloudStorageUrl().isEmpty()) {
            StorageReference fileRef = storage.getReferenceFromUrl(document.getCloudStorageUrl());
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> deleteFirestoreDocRef(document.getId(), callback))
                    .addOnFailureListener(e -> callback.onFailure(e instanceof Exception ? (Exception) e : new Exception(e)));
            return;
        }

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

        String storagePath = "users/"
                + documentMeta.getUserId()
                + "/documents/"
                + documentMeta.getFileName()
                + "_"
                + System.currentTimeMillis();
        StorageReference fileRef = storage.getReference().child(storagePath);
        UploadTask uploadTask = fileRef.putFile(localFileUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            int progress = (int) (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
            callback.onProgress(progress);
        }).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return fileRef.getDownloadUrl();
        }).addOnSuccessListener(downloadUri -> {
            long currentTime = System.currentTimeMillis();
            documentMeta.setCloudStorageUrl(downloadUri.toString());
            if (documentMeta.getCreatedDate() == 0L) {
                documentMeta.setCreatedDate(currentTime);
            }
            documentMeta.setLastModified(currentTime);

            db.collection(COLLECTION_PATH).document(documentId).set(documentMeta)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(documentMeta))
                    .addOnFailureListener(callback::onFailure);
        }).addOnFailureListener(callback::onFailure);
    }

    private void deleteFirestoreDocRef(String docId, DocumentCallback.SimpleCallback callback) {
        db.collection(COLLECTION_PATH).document(docId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}

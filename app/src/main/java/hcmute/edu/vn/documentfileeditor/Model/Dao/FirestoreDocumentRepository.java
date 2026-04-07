package hcmute.edu.vn.documentfileeditor.Model.Dao;

import android.net.Uri;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;

public class FirestoreDocumentRepository {
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final String COLLECTION_PATH = "User_Documents";

    public FirestoreDocumentRepository() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Tải file từ local lên Firebase Storage, sau đó lấy URL lưu vào Firestore.
     * @param localFileUri URI của file local
     * @param documentMeta Đối tượng metadata chuẩn bị tạo
     * @param callback Lắng nghe sự kiện
     */
    public void uploadDocument(Uri localFileUri, DocumentFB documentMeta, DocumentCallback.UploadCallback callback) {
        if (localFileUri == null || documentMeta == null) {
            callback.onFailure(new IllegalArgumentException("URI hoặc thông tin document không hợp lệ"));
            return;
        }

        // Tạo Document ID mới trên Firestore
        String documentId = db.collection(COLLECTION_PATH).document().getId();
        documentMeta.setId(documentId);

        // Chuẩn bị đường dẫn File Storage: ví dụ: users/uid/documents/filename_timestamp.pdf
        String storagePath = "users/" + documentMeta.getUserId() + "/documents/" + documentMeta.getFileName() + "_" + System.currentTimeMillis();
        StorageReference fileRef = storage.getReference().child(storagePath);

        UploadTask uploadTask = fileRef.putFile(localFileUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            int progress = (int) (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
            callback.onProgress(progress);
        }).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            // Tiếp tục tải URL của file vừa upload
            return fileRef.getDownloadUrl();
        }).addOnSuccessListener(downloadUri -> {
            // Gán URL Cloud Storage vào Entity
            documentMeta.setCloudStorageUrl(downloadUri.toString());
            
            // Cập nhật ngày tháng tạo/chỉnh sửa
            long currentTime = System.currentTimeMillis();
            documentMeta.setCreatedDate(currentTime);
            documentMeta.setLastModified(currentTime);

            // Ghi dữ liệu vào Firestore Database
            db.collection(COLLECTION_PATH).document(documentId).set(documentMeta)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(documentMeta))
                    .addOnFailureListener(callback::onFailure);

        }).addOnFailureListener(callback::onFailure);
    }

    /**
     * Lấy danh sách Document hiển thị ở Document / Home (Hybrid)
     * Thư viện Firestore đã hỗ trợ mặc định cấu hình Caching Offline-first
     * Hàm lấy này sẽ lấy data từ cache khi offline, khi có mạng nó sẽ đồng bộ.
     */
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

    /**
     * Xoá Document ở Storage, sau khi thành công sẽ xoá ở Firestore
     */
    public void deleteDocument(DocumentFB document, DocumentCallback.SimpleCallback callback) {
        if (document.getCloudStorageUrl() != null && !document.getCloudStorageUrl().isEmpty()) {
            StorageReference fileRef = storage.getReferenceFromUrl(document.getCloudStorageUrl());
            fileRef.delete().addOnSuccessListener(aVoid -> {
                // Xoá file ở Storage thành công -> Xoá Node metadata ở Firestore
                deleteFirestoreDocRef(document.getId(), callback);
            }).addOnFailureListener(e -> {
                // Đôi khi file bị xoá nhưng xoá lỗi, xử lý linh hoạt (tuỳ requirement, ở đây báo lỗi luôn)
                callback.onFailure(e);
            });
        } else {
            // Xoá hẳn trên Firestore nếu không có URL CloudStorage
            deleteFirestoreDocRef(document.getId(), callback);
        }
    }

    private void deleteFirestoreDocRef(String docId, DocumentCallback.SimpleCallback callback) {
        db.collection(COLLECTION_PATH).document(docId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}

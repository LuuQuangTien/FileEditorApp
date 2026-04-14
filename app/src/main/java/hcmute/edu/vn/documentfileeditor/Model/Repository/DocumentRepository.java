package hcmute.edu.vn.documentfileeditor.Model.Repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Dao.DocumentDao;
import hcmute.edu.vn.documentfileeditor.Model.DataSource.Remote.FirestoreDocumentRepository;
import hcmute.edu.vn.documentfileeditor.Model.Database.DocumentDatabase;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentEntity;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Mapper.DocumentMapper;
import hcmute.edu.vn.documentfileeditor.Model.Storage.LocalDocumentStorage;

/**
 * Concrete implementation of IDocumentRepository.
 * Orchestrates data flow between local (Room + file storage) and remote (Firestore + Firebase Storage).
 */
public class DocumentRepository implements IDocumentRepository {
    private static final String TAG = "DocumentRepository";
    private static volatile DocumentRepository instance;

    private final DocumentDao documentDao;
    private final FirestoreDocumentRepository remoteRepository;
    private final LocalDocumentStorage localDocumentStorage;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler;
    private final Set<String> syncingDocumentIds;
    private final Map<String, DocumentFB> cachedDocumentsById;

    private DocumentRepository(Context context) {
        DocumentDatabase database = DocumentDatabase.getInstance(context);
        this.documentDao = database.documentDao();
        this.remoteRepository = new FirestoreDocumentRepository();
        this.localDocumentStorage = new LocalDocumentStorage();
        this.ioExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.syncingDocumentIds = Collections.synchronizedSet(new HashSet<>());
        this.cachedDocumentsById = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    public static DocumentRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (DocumentRepository.class) {
                if (instance == null) {
                    instance = new DocumentRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @Override
    public void uploadDocument(Context context, Uri sourceUri, DocumentFB documentMeta, DocumentCallback.UploadCallback callback) {
        ioExecutor.execute(() -> {
            String localPath = localDocumentStorage.saveFile(context, sourceUri, documentMeta.getFileName());
            if (localPath == null) {
                mainHandler.post(() -> callback.onFailure(new Exception("Khong luu duoc file local")));
                return;
            }
            completeNewUpload(documentMeta, localPath);
            mainHandler.post(() -> {
                callback.onProgress(100);
                callback.onSuccess(documentMeta);
            });
            syncDocumentInBackground(documentMeta);
        });
    }

    @Override
    public void createDocument(Context context, DocumentFB documentMeta, String initialContent, DocumentCallback.UploadCallback callback) {
        ioExecutor.execute(() -> {
            String localPath = localDocumentStorage.createFile(context, documentMeta.getFileName(), initialContent);
            if (localPath == null) {
                mainHandler.post(() -> callback.onFailure(new Exception("Khong tao duoc file local")));
                return;
            }
            completeNewUpload(documentMeta, localPath);
            mainHandler.post(() -> {
                callback.onProgress(100);
                callback.onSuccess(documentMeta);
            });
            syncDocumentInBackground(documentMeta);
        });
    }

    @Override
    public void saveDocument(DocumentFB documentMeta, DocumentCallback.UploadCallback callback) {
        if (documentMeta == null || documentMeta.getLocalPath() == null || documentMeta.getLocalPath().isEmpty()) {
            callback.onFailure(new Exception("Document khong co localPath"));
            return;
        }

        File localFile = new File(documentMeta.getLocalPath());
        if (!localFile.exists()) {
            callback.onFailure(new Exception("Khong tim thay file local de luu"));
            return;
        }

        ioExecutor.execute(() -> {
            documentMeta.setSizeBytes(localFile.length());
            documentMeta.setLastModified(System.currentTimeMillis());
            cacheDocument(documentMeta);
            documentDao.upsert(DocumentMapper.toEntity(documentMeta));
            mainHandler.post(() -> {
                callback.onProgress(100);
                callback.onSuccess(documentMeta);
            });
            syncDocumentInBackground(documentMeta);
        });
    }

    @Override
    public void getDocuments(String userId, DocumentCallback.GetDocumentsCallback callback) {
        List<DocumentFB> cachedDocuments = getCachedDocuments(userId);
        if (!cachedDocuments.isEmpty()) {
            mainHandler.post(() -> callback.onSuccess(cachedDocuments));
        }

        ioExecutor.execute(() -> {
            List<DocumentFB> localDocuments = DocumentMapper.toModels(documentDao.getDocumentsByUserId(userId));
            cacheDocuments(localDocuments);
            syncPendingDocumentsInBackground(localDocuments);
            if (!localDocuments.isEmpty()) {
                mainHandler.post(() -> callback.onSuccess(localDocuments));
            }

            remoteRepository.getDocuments(userId, new DocumentCallback.GetDocumentsCallback() {
                @Override
                public void onSuccess(List<DocumentFB> documents) {
                    ioExecutor.execute(() -> {
                        List<DocumentFB> latestLocalDocuments = DocumentMapper.toModels(documentDao.getDocumentsByUserId(userId));
                        mergeRemoteDocumentsWithLocalState(documents);
                        List<DocumentFB> mergedDocuments = mergeWithLocalOnlyDocuments(documents, latestLocalDocuments);
                        cacheDocuments(mergedDocuments);
                        documentDao.upsertAll(DocumentMapper.toEntities(mergedDocuments));
                        mainHandler.post(() -> callback.onSuccess(mergedDocuments));
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    if (localDocuments.isEmpty()) {
                        mainHandler.post(() -> callback.onFailure(e));
                    }
                }
            });
        });
    }

    @Override
    public void downloadIfNeeded(Context context, DocumentFB document, DocumentCallback.DownloadCallback callback) {
        if (document.getLocalPath() != null && new File(document.getLocalPath()).exists()) {
            callback.onSuccess(document.getLocalPath());
            return;
        }

        File targetFile = localDocumentStorage.getDocumentFile(context, document.getFileName());
        remoteRepository.downloadDocument(document, targetFile, new DocumentCallback.DownloadCallback() {
            @Override
            public void onSuccess(String localPath) {
                document.setLocalPath(localPath);
                document.setLastModified(System.currentTimeMillis());
                cacheDocument(document);
                ioExecutor.execute(() -> {
                    documentDao.upsert(DocumentMapper.toEntity(document));
                    mainHandler.post(() -> callback.onSuccess(localPath));
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    @Override
    public void deleteDocument(DocumentFB document, DocumentCallback.SimpleCallback callback) {
        remoteRepository.deleteDocument(document, new DocumentCallback.SimpleCallback() {
            @Override
            public void onSuccess() {
                ioExecutor.execute(() -> {
                    documentDao.deleteById(document.getId());
                    removeCachedDocument(document.getId());
                    localDocumentStorage.deleteFile(document.getLocalPath());
                    mainHandler.post(callback::onSuccess);
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

    @Override
    public void retrySync(DocumentFB document) {
        syncDocumentInBackground(document);
    }

    @Override
    public List<DocumentFB> getCachedDocuments(String userId) {
        List<DocumentFB> cachedDocuments = new ArrayList<>();
        synchronized (cachedDocumentsById) {
            for (DocumentFB document : cachedDocumentsById.values()) {
                if (document != null && userId.equals(document.getUserId())) {
                    cachedDocuments.add(copyDocument(document));
                }
            }
        }
        cachedDocuments.sort((left, right) -> Long.compare(getSortTime(right), getSortTime(left)));
        return cachedDocuments;
    }

    private void completeNewUpload(DocumentFB documentMeta, String localPath) {
        if (documentMeta.getId() == null || documentMeta.getId().isEmpty()) {
            documentMeta.setId(UUID.randomUUID().toString());
        }

        File localFile = new File(localPath);
        documentMeta.setLocalPath(localPath);
        documentMeta.setSizeBytes(localFile.length());
        long currentTime = System.currentTimeMillis();
        if (documentMeta.getCreatedDate() == 0L) {
            documentMeta.setCreatedDate(currentTime);
        }
        documentMeta.setLastModified(currentTime);

        cacheDocument(documentMeta);
        documentDao.upsert(DocumentMapper.toEntity(documentMeta));
    }

    private void syncDocumentInBackground(DocumentFB documentMeta) {
        if (documentMeta == null || documentMeta.getLocalPath() == null || documentMeta.getLocalPath().isEmpty()) {
            return;
        }
        if (documentMeta.getId() == null || documentMeta.getId().isEmpty()) {
            return;
        }
        if (documentMeta.getCloudStorageUrl() != null && !documentMeta.getCloudStorageUrl().isEmpty()) {
            return;
        }
        if (!syncingDocumentIds.add(documentMeta.getId())) {
            return;
        }

        File localFile = new File(documentMeta.getLocalPath());
        if (!localFile.exists()) {
            Log.w(TAG, "Skip cloud sync because local file does not exist: " + documentMeta.getLocalPath());
            syncingDocumentIds.remove(documentMeta.getId());
            return;
        }

        remoteRepository.syncDocument(Uri.fromFile(localFile), documentMeta, new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB documentFB) {
                cacheDocument(documentFB);
                ioExecutor.execute(() -> documentDao.upsert(DocumentMapper.toEntity(documentFB)));
                syncingDocumentIds.remove(documentMeta.getId());
                Log.d(TAG, "Cloud sync success for " + documentFB.getFileName());
            }

            @Override
            public void onProgress(int progressPercentage) {
                Log.d(TAG, "Cloud sync progress " + progressPercentage + "% for " + documentMeta.getFileName());
            }

            @Override
            public void onFailure(Exception e) {
                syncingDocumentIds.remove(documentMeta.getId());
                Log.e(TAG, "Cloud sync failed for " + documentMeta.getFileName(), e);
            }
        });
    }

    private void syncPendingDocumentsInBackground(List<DocumentFB> documents) {
        for (DocumentFB document : documents) {
            if (document.getCloudStorageUrl() == null || document.getCloudStorageUrl().isEmpty()) {
                syncDocumentInBackground(document);
            }
        }
    }

    private void cacheDocuments(List<DocumentFB> documents) {
        synchronized (cachedDocumentsById) {
            for (DocumentFB document : documents) {
                cacheDocumentLocked(document);
            }
        }
    }

    private void cacheDocument(DocumentFB document) {
        synchronized (cachedDocumentsById) {
            cacheDocumentLocked(document);
        }
    }

    private void cacheDocumentLocked(DocumentFB document) {
        if (document == null || document.getId() == null || document.getId().isEmpty()) {
            return;
        }
        cachedDocumentsById.put(document.getId(), copyDocument(document));
    }

    private void removeCachedDocument(String documentId) {
        if (documentId == null || documentId.isEmpty()) {
            return;
        }
        synchronized (cachedDocumentsById) {
            cachedDocumentsById.remove(documentId);
        }
    }

    private long getSortTime(DocumentFB document) {
        if (document.getLastModified() > 0L) {
            return document.getLastModified();
        }
        return document.getCreatedDate();
    }

    private DocumentFB copyDocument(DocumentFB source) {
        DocumentFB copy = new DocumentFB();
        copy.setId(source.getId());
        copy.setUserId(source.getUserId());
        copy.setFileName(source.getFileName());
        copy.setFileType(source.getFileType());
        copy.setCloudStorageUrl(source.getCloudStorageUrl());
        copy.setLocalPath(source.getLocalPath());
        copy.setSizeBytes(source.getSizeBytes());
        copy.setCreatedDate(source.getCreatedDate());
        copy.setLastModified(source.getLastModified());
        return copy;
    }

    private void mergeRemoteDocumentsWithLocalState(List<DocumentFB> remoteDocuments) {
        for (DocumentFB remoteDocument : remoteDocuments) {
            DocumentEntity localEntity = documentDao.getById(remoteDocument.getId());
            if (localEntity == null) {
                continue;
            }

            if (localEntity.getLocalPath() != null && !localEntity.getLocalPath().isEmpty()) {
                remoteDocument.setLocalPath(localEntity.getLocalPath());
            }
            if (remoteDocument.getSizeBytes() == 0L) {
                remoteDocument.setSizeBytes(localEntity.getSizeBytes());
            }
            if (remoteDocument.getCreatedDate() == 0L) {
                remoteDocument.setCreatedDate(localEntity.getCreatedDate());
            }
        }
    }

    private List<DocumentFB> mergeWithLocalOnlyDocuments(List<DocumentFB> remoteDocuments, List<DocumentFB> localDocuments) {
        List<DocumentFB> mergedDocuments = new ArrayList<>(remoteDocuments);
        Set<String> remoteIds = new HashSet<>();

        for (DocumentFB remoteDocument : remoteDocuments) {
            if (remoteDocument.getId() != null && !remoteDocument.getId().isEmpty()) {
                remoteIds.add(remoteDocument.getId());
            }
        }

        for (DocumentFB localDocument : localDocuments) {
            if (localDocument.getId() == null || localDocument.getId().isEmpty()) {
                continue;
            }
            if (remoteIds.contains(localDocument.getId())) {
                continue;
            }
            mergedDocuments.add(copyDocument(localDocument));
        }

        mergedDocuments.sort((left, right) -> Long.compare(getSortTime(right), getSortTime(left)));
        return mergedDocuments;
    }
}

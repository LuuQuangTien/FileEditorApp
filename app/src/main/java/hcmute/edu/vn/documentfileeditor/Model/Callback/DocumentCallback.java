package hcmute.edu.vn.documentfileeditor.Model.Callback;

import java.util.List;

import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;

/**
 * Callback interfaces for asynchronous document operations.
 * Separated from DAO package to follow proper layered architecture.
 */
public interface DocumentCallback {

    interface UploadCallback {
        void onSuccess(DocumentFB documentFB);
        void onProgress(int progressPercentage);
        void onFailure(Exception e);
    }

    interface GetDocumentsCallback {
        void onSuccess(List<DocumentFB> documents);
        void onFailure(Exception e);
    }

    interface DownloadCallback {
        void onSuccess(String localPath);
        void onFailure(Exception e);
    }

    interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}

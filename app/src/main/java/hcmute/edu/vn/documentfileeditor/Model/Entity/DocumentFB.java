package hcmute.edu.vn.documentfileeditor.Model.Entity;

import hcmute.edu.vn.documentfileeditor.Enum.FileType;

public class DocumentFB {
    private String id;
    private String userId;
    private String fileName;
    private FileType fileType;
    private String cloudStorageUrl; // URL tải file trừ Firebase Storage
    private String localPath; // Đường dẫn cache ở máy (Offline)
    private long sizeBytes;
    private long createdDate;
    private long lastModified;

    public DocumentFB() {
    }

    public DocumentFB(String id, String userId, String fileName, FileType fileType, String cloudStorageUrl, String localPath, long sizeBytes, long createdDate, long lastModified) {
        this.id = id;
        this.userId = userId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.cloudStorageUrl = cloudStorageUrl;
        this.localPath = localPath;
        this.sizeBytes = sizeBytes;
        this.createdDate = createdDate;
        this.lastModified = lastModified;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getCloudStorageUrl() {
        return cloudStorageUrl;
    }

    public void setCloudStorageUrl(String cloudStorageUrl) {
        this.cloudStorageUrl = cloudStorageUrl;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}

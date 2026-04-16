package hcmute.edu.vn.documentfileeditor.Model.Entity;

public class FolderItem {
    private final String name;
    private final long createdAt;

    public FolderItem(String name, long createdAt) {
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

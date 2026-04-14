package hcmute.edu.vn.documentfileeditor.Model.Dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentEntity;

@Dao
public interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(DocumentEntity document);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<DocumentEntity> documents);

    @Query("SELECT * FROM documents WHERE userId = :userId ORDER BY lastModified DESC")
    List<DocumentEntity> getDocumentsByUserId(String userId);

    @Query("SELECT * FROM documents WHERE id = :documentId LIMIT 1")
    DocumentEntity getById(String documentId);

    @Query("DELETE FROM documents WHERE id = :documentId")
    void deleteById(String documentId);
}

package hcmute.edu.vn.documentfileeditor.Model.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import hcmute.edu.vn.documentfileeditor.Model.Dao.DocumentDao;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentEntity;

@Database(entities = {DocumentEntity.class}, version = 1, exportSchema = false)
@TypeConverters({FileTypeConverter.class})
public abstract class DocumentDatabase extends RoomDatabase {
    private static final String DB_NAME = "documents.db";
    private static volatile DocumentDatabase instance;

    public abstract DocumentDao documentDao();

    public static DocumentDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (DocumentDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            DocumentDatabase.class,
                            DB_NAME
                    ).build();
                }
            }
        }
        return instance;
    }
}

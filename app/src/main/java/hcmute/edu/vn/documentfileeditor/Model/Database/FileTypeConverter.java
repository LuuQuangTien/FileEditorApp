package hcmute.edu.vn.documentfileeditor.Model.Database;

import androidx.room.TypeConverter;

import hcmute.edu.vn.documentfileeditor.Enum.FileType;

public class FileTypeConverter {

    @TypeConverter
    public static String fromFileType(FileType fileType) {
        return fileType == null ? null : fileType.name();
    }

    @TypeConverter
    public static FileType toFileType(String value) {
        return value == null ? null : FileType.valueOf(value);
    }
}

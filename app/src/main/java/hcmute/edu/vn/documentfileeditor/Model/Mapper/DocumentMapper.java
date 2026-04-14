package hcmute.edu.vn.documentfileeditor.Model.Mapper;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentEntity;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;

public final class DocumentMapper {
    private DocumentMapper() {
    }

    public static DocumentEntity toEntity(DocumentFB documentFB) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(documentFB.getId());
        entity.setUserId(documentFB.getUserId());
        entity.setFileName(documentFB.getFileName());
        entity.setFileType(documentFB.getFileType());
        entity.setCloudStorageUrl(documentFB.getCloudStorageUrl());
        entity.setLocalPath(documentFB.getLocalPath());
        entity.setSizeBytes(documentFB.getSizeBytes());
        entity.setCreatedDate(documentFB.getCreatedDate());
        entity.setLastModified(documentFB.getLastModified());
        return entity;
    }

    public static DocumentFB toModel(DocumentEntity entity) {
        DocumentFB documentFB = new DocumentFB();
        documentFB.setId(entity.getId());
        documentFB.setUserId(entity.getUserId());
        documentFB.setFileName(entity.getFileName());
        documentFB.setFileType(entity.getFileType());
        documentFB.setCloudStorageUrl(entity.getCloudStorageUrl());
        documentFB.setLocalPath(entity.getLocalPath());
        documentFB.setSizeBytes(entity.getSizeBytes());
        documentFB.setCreatedDate(entity.getCreatedDate());
        documentFB.setLastModified(entity.getLastModified());
        return documentFB;
    }

    public static List<DocumentFB> toModels(List<DocumentEntity> entities) {
        List<DocumentFB> models = new ArrayList<>();
        for (DocumentEntity entity : entities) {
            models.add(toModel(entity));
        }
        return models;
    }

    public static List<DocumentEntity> toEntities(List<DocumentFB> documents) {
        List<DocumentEntity> entities = new ArrayList<>();
        for (DocumentFB document : documents) {
            entities.add(toEntity(document));
        }
        return entities;
    }
}

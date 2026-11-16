package com.filebridge.repository;

import com.filebridge.model.FileMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {

    Optional<FileMetadata> findByStorageKey(String storageKey);

    List<FileMetadata> findByOwnerId(String ownerId);

    List<FileMetadata> findByOwnerIdAndInTrashFalse(String ownerId);

    List<FileMetadata> findByOwnerIdAndFolderIdAndInTrashFalse(String ownerId, String folderId);

    List<FileMetadata> findByOwnerIdAndFolderIdIsNullAndInTrashFalse(String ownerId);

    List<FileMetadata> findByOwnerIdAndInTrashTrue(String ownerId);

    List<FileMetadata> findByFolderId(String folderId);

    Optional<FileMetadata> findByIdAndOwnerId(String id, String ownerId);

    boolean existsByIdAndOwnerId(String id, String ownerId);

    void deleteByIdAndOwnerId(String id, String ownerId);

    long countByOwnerIdAndInTrashFalse(String ownerId);
}

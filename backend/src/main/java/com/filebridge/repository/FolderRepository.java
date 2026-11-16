package com.filebridge.repository;

import com.filebridge.model.Folder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends MongoRepository<Folder, String> {

    List<Folder> findByOwnerIdAndInTrashFalse(String ownerId);

    List<Folder> findByOwnerIdAndParentIdAndInTrashFalse(String ownerId, String parentId);

    List<Folder> findByOwnerIdAndParentIdIsNullAndInTrashFalse(String ownerId);

    List<Folder> findByOwnerIdAndInTrashTrue(String ownerId);

    List<Folder> findByParentId(String parentId);

    Optional<Folder> findByIdAndOwnerId(String id, String ownerId);

    boolean existsByIdAndOwnerId(String id, String ownerId);

    boolean existsByOwnerIdAndParentIdAndNameIgnoreCaseAndInTrashFalse(String ownerId, String parentId, String name);

    boolean existsByOwnerIdAndParentIdIsNullAndNameIgnoreCaseAndInTrashFalse(String ownerId, String name);

    void deleteByIdAndOwnerId(String id, String ownerId);

    long countByOwnerIdAndInTrashFalse(String ownerId);
}

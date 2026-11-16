package com.filebridge.repository;

import com.filebridge.model.FileShare;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepository extends MongoRepository<FileShare, String> {

    List<FileShare> findByFileIdAndRevokedFalse(String fileId);

    List<FileShare> findByFileId(String fileId);

    Optional<FileShare> findByFileIdAndSharedWithEmailIgnoreCaseAndRevokedFalse(String fileId, String sharedWithEmail);

    Optional<FileShare> findByFileIdAndSharedWithUserIdAndRevokedFalse(String fileId, String sharedWithUserId);

    List<FileShare> findBySharedWithUserIdAndRevokedFalse(String sharedWithUserId);

    List<FileShare> findBySharedWithEmailIgnoreCaseAndRevokedFalse(String sharedWithEmail);

    Optional<FileShare> findByShareTokenAndRevokedFalse(String shareToken);

    Optional<FileShare> findByFileIdAndIsLinkShareTrueAndRevokedFalse(String fileId);

    List<FileShare> findByOwnerIdAndRevokedFalse(String ownerId);

    void deleteByFileId(String fileId);
}

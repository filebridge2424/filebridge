package com.filebridge.repository;

import com.filebridge.model.Activity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends MongoRepository<Activity, String> {

    List<Activity> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    List<Activity> findByUserIdOrderByTimestampDesc(String userId);

    void deleteByUserId(String userId);
}

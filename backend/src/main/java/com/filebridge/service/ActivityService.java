package com.filebridge.service;

import com.filebridge.dto.ActivityResponse;
import com.filebridge.model.Activity;
import com.filebridge.model.ActivityType;
import com.filebridge.repository.ActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final ActivityRepository activityRepository;

    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public void logActivity(String userId, ActivityType eventType, String fileId, String fileName, String description) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        try {
            Activity activity = new Activity(userId, eventType, fileId, fileName, description);
            activityRepository.save(activity);
            log.debug("Logged activity: {} for user: {}", eventType, userId);
        } catch (Exception e) {
            log.warn("Failed to log activity: {}", e.getMessage());
        }
    }

    public List<ActivityResponse> getUserActivities(String userId, int page, int size) {
        int pageIndex = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);

        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        return activityRepository.findByUserIdOrderByTimestampDesc(userId, pageRequest)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ActivityResponse toResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getEventType(),
                activity.getFileId(),
                activity.getFileName(),
                activity.getDescription(),
                activity.getTimestamp()
        );
    }
}

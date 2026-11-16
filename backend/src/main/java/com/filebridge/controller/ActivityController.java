package com.filebridge.controller;

import com.filebridge.dto.ActivityResponse;
import com.filebridge.security.UserPrincipal;
import com.filebridge.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Get paginated timeline of activities for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getUserActivities(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(activityService.getUserActivities(principal.getId(), page, size));
    }
}

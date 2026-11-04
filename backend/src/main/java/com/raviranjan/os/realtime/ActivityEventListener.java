package com.raviranjan.os.realtime;

import com.raviranjan.os.domain.entity.DeveloperActivity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActivityEventListener {

    private final SseEmitterRegistry emitterRegistry;

    public ActivityEventListener(SseEmitterRegistry emitterRegistry) {
        this.emitterRegistry = emitterRegistry;
    }

    @EventListener
    public void onActivitySynchronized(ActivitySynchronizedEvent event) {
        List<DeveloperActivity> activities = event.getActivities();
        if (activities != null && !activities.isEmpty()) {
            emitterRegistry.broadcast("activity-update", activities);
        }
    }
}

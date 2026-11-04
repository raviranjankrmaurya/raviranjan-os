package com.raviranjan.os.realtime;

import com.raviranjan.os.domain.entity.DeveloperActivity;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class ActivitySynchronizedEvent extends ApplicationEvent {
    private final List<DeveloperActivity> activities;

    public ActivitySynchronizedEvent(Object source, List<DeveloperActivity> activities) {
        super(source);
        this.activities = activities;
    }

    public List<DeveloperActivity> getActivities() {
        return activities;
    }
}

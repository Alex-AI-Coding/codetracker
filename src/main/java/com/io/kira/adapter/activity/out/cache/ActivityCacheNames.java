package com.io.kira.adapter.activity.out.cache;

public final class ActivityCacheNames {

    /**
     * Versioned names prevent data written before the activity-id UUID migration
     * from being read with the current domain model. Old Redis entries expire on
     * their normal TTL and do not need a destructive cache flush.
     */
    public static final String ACTIVITY = "activities-v2";
    public static final String STUDENT_ACTIVITY = "studentActivities-v2";
    public static final String ACTIVITY_INFO = "activityInfo-v2";

    private ActivityCacheNames() {}
}

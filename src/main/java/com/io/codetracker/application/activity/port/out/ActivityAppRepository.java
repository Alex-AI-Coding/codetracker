package com.io.codetracker.application.activity.port.out;

import com.io.codetracker.application.activity.result.StudentActivityViewData;
import com.io.codetracker.domain.activity.entity.Activity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityAppRepository {

    Activity save(Activity data);

    List<Activity> findActivitiesByClassroomIdAndInstructorUserId(
            UUID classroomId,
            UUID instructorId
    );

    List<Activity> findActivitiesByClassroomId(UUID classroomId);

    Optional<Activity> findById(String activityId);

    void deleteByActivityId(String activityId);

    void update(Activity updatedActivity);

    List<StudentActivityViewData> findStudentActivities(
            UUID classroomId,
            UUID userId
    );
}

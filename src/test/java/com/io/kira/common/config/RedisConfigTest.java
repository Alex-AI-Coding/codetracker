package com.io.kira.common.config;

import com.io.kira.domain.activity.entity.Activity;
import com.io.kira.domain.activity.entity.StudentActivity;
import com.io.kira.domain.activity.valueObject.ActivityStatus;
import com.io.kira.domain.activity.valueObject.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    private final GenericJacksonJsonRedisSerializer serializer = RedisConfig.redisValueSerializer();

    @Test
    void roundTripsActivityListsAfterUuidMigration() {
        UUID activityId = UUID.randomUUID();
        Activity activity = Activity.reconstitute(
                activityId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Cache compatibility",
                "Verify UUID activity IDs survive Redis serialization",
                Instant.parse("2026-09-01T00:00:00Z"),
                ActivityStatus.PUBLISHED,
                100,
                Instant.parse("2026-08-30T00:00:00Z"),
                Instant.parse("2026-08-30T00:00:00Z")
        );

        Object restored = serializer.deserialize(serializer.serialize(List.of(activity)));

        assertThat(restored).isInstanceOf(List.class);
        List<?> restoredActivities = (List<?>) restored;
        assertThat(restoredActivities).hasSize(1);
        assertThat(restoredActivities.get(0)).isInstanceOf(Activity.class);
        Activity restoredActivity = (Activity) restoredActivities.get(0);
        assertThat(restoredActivity.getActivityId()).isEqualTo(activityId);
    }

    @Test
    void roundTripsStudentActivitiesWithUuidActivityIds() {
        UUID studentActivityId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        StudentActivity studentActivity = StudentActivity.reconstitute(
                studentActivityId,
                activityId,
                UUID.randomUUID(),
                SubmissionStatus.SUBMITTED,
                null,
                null,
                "abc123"
        );

        Object restored = serializer.deserialize(serializer.serialize(studentActivity));

        assertThat(restored).isInstanceOf(StudentActivity.class);
        StudentActivity restoredStudentActivity = (StudentActivity) restored;
        assertThat(restoredStudentActivity.getStudentActivityId()).isEqualTo(studentActivityId);
        assertThat(restoredStudentActivity.getActivityId()).isEqualTo(activityId);
    }
}

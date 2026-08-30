package com.io.kira.adapter.activity.out.persistence.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.io.kira.domain.activity.entity.StudentActivity;
import com.io.kira.domain.activity.valueObject.SubmissionStatus;

import java.util.UUID;

public abstract class StudentActivityJacksonMixIn {

    @JsonCreator
    static StudentActivity reconstitute(
            UUID studentActivityId,
            UUID activityId,
            UUID userId,
            SubmissionStatus submissionStatus,
            String feedback,
            Integer score,
            String submittedCommitSha) {
        throw new UnsupportedOperationException();
    }
}

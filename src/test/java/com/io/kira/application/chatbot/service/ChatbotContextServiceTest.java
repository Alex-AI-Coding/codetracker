package com.io.kira.application.chatbot.service;

import com.io.kira.application.activity.port.out.ActivityAppRepository;
import com.io.kira.application.activity.port.out.StudentActivityInfoAppRepository;
import com.io.kira.application.activity.result.StudentActivityOverviewData;
import com.io.kira.application.classroom.port.out.ClassroomAppRepository;
import com.io.kira.application.classroom.port.out.ClassroomStudentAppRepository;
import com.io.kira.domain.activity.entity.Activity;
import com.io.kira.domain.activity.valueObject.ActivityStatus;
import com.io.kira.domain.activity.valueObject.SubmissionStatus;
import com.io.kira.domain.classroom.aggregate.ClassroomAggregate;
import com.io.kira.domain.classroom.entity.Classroom;
import com.io.kira.domain.classroom.entity.ClassroomSettings;
import com.io.kira.domain.classroom.valueObject.ClassroomStatus;
import com.io.kira.domain.github.valueobject.GithubSubmissionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatbotContextServiceTest {

    private StudentActivityInfoAppRepository studentActivityInfoRepository;
    private ActivityAppRepository activityRepository;
    private ClassroomAppRepository classroomRepository;
    private ClassroomStudentAppRepository classroomStudentRepository;
    private ChatbotContextService service;

    @BeforeEach
    void setUp() {
        studentActivityInfoRepository = mock(StudentActivityInfoAppRepository.class);
        activityRepository = mock(ActivityAppRepository.class);
        classroomRepository = mock(ClassroomAppRepository.class);
        classroomStudentRepository = mock(ClassroomStudentAppRepository.class);
        service = new ChatbotContextService(
                studentActivityInfoRepository,
                activityRepository,
                classroomRepository,
                classroomStudentRepository
        );
    }

    @Test
    void studentContextUsesOnlyTheCurrentStudentsOwnActivityView() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        UUID instructorId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Instant now = Instant.now();

        Activity activity = Activity.reconstitute(
                activityId,
                classroomId,
                instructorId,
                "REST API Project",
                "Build the project",
                now.plusSeconds(86_400),
                ActivityStatus.PUBLISHED,
                100,
                now,
                now
        );

        StudentActivityOverviewData ownActivity = new StudentActivityOverviewData(
                activityId,
                activity.getTitle(),
                activity.getDescription(),
                activity.getMaxScore(),
                activity.getDueDate(),
                UUID.randomUUID(),
                "student-rest-api",
                "https://github.com/student/rest-api",
                GithubSubmissionMode.EXISTING,
                now,
                "abc123",
                SubmissionStatus.GRADED,
                92,
                "Good work",
                ActivityStatus.PUBLISHED
        );

        Classroom classroom = new Classroom(
                classroomId,
                instructorId,
                "Backend Development",
                "Backend classroom",
                "BACK01",
                ClassroomStatus.ACTIVE,
                now,
                now
        );

        when(activityRepository.findActivitiesByClassroomId(classroomId))
                .thenReturn(List.of(activity));
        when(activityRepository.findStudentActivities(classroomId, userId))
                .thenReturn(List.of(ownActivity));
        when(classroomRepository.findByClassroomId(classroomId))
                .thenReturn(Optional.of(new ClassroomAggregate(
                        classroom,
                        new ClassroomSettings(classroomId, false, null, 30)
                )));

        String context = service.buildClassroomContext(
                userId,
                classroomId,
                ChatbotAccessService.ClassroomAccess.STUDENT
        );

        assertTrue(context.contains("student-rest-api"));
        assertTrue(context.contains("Score: 92"));
        assertTrue(context.contains("Feedback: Good work"));
        assertFalse(context.contains("STUDENT ACTIVITY PROGRESS"));
        verifyNoInteractions(studentActivityInfoRepository);
    }
}

package com.io.codetracker.adapter.chatbot;

import com.io.codetracker.application.activity.port.out.ActivityAppRepository;
import com.io.codetracker.application.activity.port.out.StudentActivityInfoAppRepository;
import com.io.codetracker.application.activity.result.StudentActivityInfoData;
import com.io.codetracker.application.activity.result.StudentActivityInfoStudentData;
import com.io.codetracker.application.activity.result.StudentActivityViewData;
import com.io.codetracker.domain.activity.entity.Activity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatbotContextService {

    private static final int PASSING_SCORE = 75;

    private final StudentActivityInfoAppRepository studentActivityInfoRepository;
    private final ActivityAppRepository activityRepository;

    public ChatbotContextService(
            StudentActivityInfoAppRepository studentActivityInfoRepository,
            ActivityAppRepository activityRepository
    ) {
        this.studentActivityInfoRepository = studentActivityInfoRepository;
        this.activityRepository = activityRepository;
    }

    public String buildContext(
            UUID userId,
            String classroomId,
            ChatbotAccessService.ClassroomAccess access
    ) {

        if (
                classroomId == null ||
                classroomId.isBlank() ||
                access == ChatbotAccessService.ClassroomAccess.NONE
        ) {
            return "";
        }

        if (access == ChatbotAccessService.ClassroomAccess.INSTRUCTOR) {

            List<StudentActivityInfoData> activityInfos =
                    studentActivityInfoRepository
                            .findStudentActivityInfos(classroomId);

            return buildInstructorContext(
                    classroomId,
                    activityInfos
            );
        }

        if (access == ChatbotAccessService.ClassroomAccess.STUDENT) {

            List<Activity> classroomActivities =
                    activityRepository
                            .findActivitiesByClassroomId(classroomId);

            List<StudentActivityViewData> studentActivities =
                    activityRepository
                            .findStudentActivities(
                                    classroomId,
                                    userId
                            );

            return buildStudentContext(
                    classroomActivities,
                    studentActivities
            );
        }

        return "";
    }

    private String buildInstructorContext(
            String classroomId,
            List<StudentActivityInfoData> activityInfos
    ) {

        List<StudentActivityInfoStudentData> students =
                studentActivityInfoRepository
                        .findClassroomStudents(classroomId);

        Map<UUID, String> studentNames =
                students.stream()
                        .collect(
                                Collectors.toMap(
                                        StudentActivityInfoStudentData::userId,
                                        student -> (
                                                safe(student.firstName())
                                                        + " "
                                                        + safe(student.lastName())
                                        ).trim(),
                                        (existing, duplicate) -> existing
                                )
                        );

        StringBuilder context = new StringBuilder();

        context.append("""
                VERIFIED CLASSROOM DATA FOR INSTRUCTOR

                Passing rule:
                A score of 75 or higher on a specific activity/project is PASS.
                A score below 75 on that specific activity/project is FAIL.
                This is a raw project score, NOT a percentage and NOT the student's overall classroom grade.

                """);

        for (StudentActivityInfoData info : activityInfos) {

            String studentName =
                    studentNames.getOrDefault(
                            info.userId(),
                            "Unknown Student"
                    );

            context.append("Student: ")
                    .append(studentName)
                    .append("\n");

            context.append("Activity ID: ")
                    .append(safe(info.activityId()))
                    .append("\n");

            context.append("Activity Title: ")
                    .append(safe(info.title()))
                    .append("\n");

            context.append("Submission Status: ")
                    .append(
                            info.submissionStatus() != null
                                    ? info.submissionStatus()
                                    : "NOT_SUBMITTED"
                    )
                    .append("\n");

            context.append("Score: ")
                    .append(
                            info.score() != null
                                    ? info.score()
                                    : "NOT_GRADED"
                    )
                    .append("\n");

            context.append("Maximum Score: ")
                    .append(
                            info.maxScore() != null
                                    ? info.maxScore()
                                    : "UNKNOWN"
                    )
                    .append("\n");

            context.append("Result: ")
                    .append(getResult(info.score()))
                    .append("\n");

            context.append("Feedback: ")
                    .append(safe(info.feedback()))
                    .append("\n");

            context.append("---\n");
        }

        return context.toString();
    }

    private String buildStudentContext(
            List<Activity> classroomActivities,
            List<StudentActivityViewData> studentActivities
    ) {

        Map<String, StudentActivityViewData> studentActivityMap =
                studentActivities.stream()
                        .collect(
                                Collectors.toMap(
                                        StudentActivityViewData::activityId,
                                        info -> info,
                                        (existing, duplicate) -> existing
                                )
                        );

        StringBuilder context = new StringBuilder();

        context.append("""
                VERIFIED CLASSROOM ACTIVITIES FOR CURRENT STUDENT ONLY

                Privacy rule:
                Only the currently logged-in student's personal submission
                and grade information is included.
                Never provide information about another student's score,
                submission, feedback, or work.

                Passing rule:
                A score of 75 or higher on a specific activity/project is PASS.
                A score below 75 on that specific activity/project is FAIL.
                This is a raw project score, NOT a percentage and NOT the student's overall classroom grade.

                Pending activity rule:
                A PUBLISHED activity with no linked repository or no submission
                may require action from the student.

                An activity that appears in the classroom but has no student
                activity record must NOT be ignored. It may mean the student
                still needs to link a repository or make a submission.

                CLOSED or ARCHIVED activities should not normally be described
                as currently pending work.

                """);

        for (Activity activity : classroomActivities) {

            StudentActivityViewData info =
                    studentActivityMap.get(
                            activity.getActivityId()
                    );

            context.append("Activity ID: ")
                    .append(safe(activity.getActivityId()))
                    .append("\n");

            context.append("Activity Title: ")
                    .append(safe(activity.getTitle()))
                    .append("\n");

            context.append("Description: ")
                    .append(safe(activity.getDescription()))
                    .append("\n");

            context.append("Activity Status: ")
                    .append(
                            activity.getStatus() != null
                                    ? activity.getStatus()
                                    : "UNKNOWN"
                    )
                    .append("\n");

            context.append("Due Date: ")
                    .append(
                            activity.getDueDate() != null
                                    ? activity.getDueDate()
                                    : "UNKNOWN"
                    )
                    .append("\n");

            context.append("Maximum Score: ")
                    .append(
                            activity.getMaxScore() != null
                                    ? activity.getMaxScore()
                                    : "UNKNOWN"
                    )
                    .append("\n");

            if (info == null) {

                context.append("Student Activity Record: NOT_CREATED\n");
                context.append("Repository: NOT_LINKED\n");
                context.append("Submission Status: NOT_SUBMITTED\n");
                context.append("Score: NOT_GRADED\n");
                context.append("Result: NOT_GRADED\n");

                if (
                        activity.getStatus() != null &&
                        activity.getStatus().name().equals("PUBLISHED")
                ) {
                    context.append(
                            "Student Action Needed: YES - REPOSITORY OR SUBMISSION REQUIRED\n"
                    );
                } else {
                    context.append(
                            "Student Action Needed: NO CURRENT ACTION\n"
                    );
                }

            } else {

                context.append("Student Activity Record: EXISTS\n");

                context.append("Repository: ")
                        .append(
                                info.repositoryUrl() != null &&
                                !info.repositoryUrl().isBlank()
                                        ? info.repositoryUrl()
                                        : "NOT_LINKED"
                        )
                        .append("\n");

                context.append("Submission Status: ")
                        .append(
                                info.submissionStatus() != null
                                        ? info.submissionStatus()
                                        : "NOT_SUBMITTED"
                        )
                        .append("\n");

                context.append("Score: ")
                        .append(
                                info.score() != null
                                        ? info.score()
                                        : "NOT_GRADED"
                        )
                        .append("\n");

                context.append("Result: ")
                        .append(getResult(info.score()))
                        .append("\n");

                context.append("Feedback: ")
                        .append(safe(info.feedback()))
                        .append("\n");

                context.append("Student Action Needed: ")
                        .append(
                                getStudentAction(
                                        activity,
                                        info
                                )
                        )
                        .append("\n");
            }

            context.append("---\n");
        }

        return context.toString();
    }

    private String getStudentAction(
            Activity activity,
            StudentActivityViewData info
    ) {

        if (activity.getStatus() == null) {
            return "UNKNOWN";
        }

        if (!activity.getStatus().name().equals("PUBLISHED")) {
            return "NO CURRENT ACTION - ACTIVITY IS "
                    + activity.getStatus();
        }

        if (
                info.repositoryUrl() == null ||
                info.repositoryUrl().isBlank()
        ) {
            return "YES - REPOSITORY NOT LINKED";
        }

        if (info.submissionStatus() == null) {
            return "YES - NOT SUBMITTED";
        }

        return switch (info.submissionStatus()) {

            case PENDING ->
                    "YES - SUBMISSION PENDING";

            case SUBMITTED ->
                    "NO - SUBMITTED AND WAITING FOR GRADING";

            case GRADED ->
                    "NO - COMPLETED AND GRADED";
        };
    }

    private String getResult(Integer score) {

        if (score == null) {
            return "NOT_GRADED";
        }

        return score >= PASSING_SCORE
                ? "PASS"
                : "FAIL";
    }

    private String safe(String value) {
        return value != null
                ? value
                : "";
    }
}
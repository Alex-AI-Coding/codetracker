package com.io.codetracker.adapter.chatbot;

import com.io.codetracker.domain.classroom.entity.Classroom;
import com.io.codetracker.domain.classroom.repository.ClassroomDomainRepository;
import com.io.codetracker.domain.classroom.repository.ClassroomStudentDomainRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChatbotAccessService {

    private final ClassroomDomainRepository classroomRepository;
    private final ClassroomStudentDomainRepository classroomStudentRepository;

    public ChatbotAccessService(
            ClassroomDomainRepository classroomRepository,
            ClassroomStudentDomainRepository classroomStudentRepository
    ) {
        this.classroomRepository = classroomRepository;
        this.classroomStudentRepository = classroomStudentRepository;
    }

    public ClassroomAccess getAccess(
            UUID userId,
            String classroomId
    ) {

        if (classroomId == null || classroomId.isBlank()) {
            return ClassroomAccess.NONE;
        }

        Classroom classroom = classroomRepository
                .findByClassroomId(classroomId)
                .orElse(null);

        if (classroom == null) {
            return ClassroomAccess.NONE;
        }

        if (userId.equals(classroom.getInstructorUserId())) {
            return ClassroomAccess.INSTRUCTOR;
        }

        boolean isStudent =
                classroomStudentRepository
                        .existsByClassroomIdAndStudentUserId(
                                classroomId,
                                userId
                        );

        if (isStudent) {
            return ClassroomAccess.STUDENT;
        }

        return ClassroomAccess.NONE;
    }

    public enum ClassroomAccess {
        INSTRUCTOR,
        STUDENT,
        NONE
    }
}
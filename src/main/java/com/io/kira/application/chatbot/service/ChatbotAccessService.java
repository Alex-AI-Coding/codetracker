package com.io.kira.application.chatbot.service;

import com.io.kira.domain.classroom.entity.Classroom;
import com.io.kira.domain.classroom.repository.ClassroomDomainRepository;
import com.io.kira.domain.classroom.repository.ClassroomStudentDomainRepository;
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

    public ClassroomAccess getAccess(UUID userId, UUID classroomId) {
        if (userId == null || classroomId == null) {
            return ClassroomAccess.NONE;
        }

        Classroom classroom = classroomRepository.findByClassroomId(classroomId).orElse(null);
        if (classroom == null) {
            return ClassroomAccess.NONE;
        }

        if (userId.equals(classroom.getInstructorUserId())) {
            return ClassroomAccess.INSTRUCTOR;
        }

        return classroomStudentRepository.existsByClassroomIdAndStudentUserId(classroomId, userId)
                ? ClassroomAccess.STUDENT
                : ClassroomAccess.NONE;
    }

    public enum ClassroomAccess {
        INSTRUCTOR,
        STUDENT,
        NONE
    }
}

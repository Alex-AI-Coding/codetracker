package com.io.kira.application.chatbot.service;

import com.io.kira.domain.classroom.entity.Classroom;
import com.io.kira.domain.classroom.repository.ClassroomDomainRepository;
import com.io.kira.domain.classroom.repository.ClassroomStudentDomainRepository;
import com.io.kira.domain.classroom.valueObject.ClassroomStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatbotAccessServiceTest {

    private ClassroomDomainRepository classroomRepository;
    private ClassroomStudentDomainRepository classroomStudentRepository;
    private ChatbotAccessService service;

    @BeforeEach
    void setUp() {
        classroomRepository = mock(ClassroomDomainRepository.class);
        classroomStudentRepository = mock(ClassroomStudentDomainRepository.class);
        service = new ChatbotAccessService(classroomRepository, classroomStudentRepository);
    }

    @Test
    void returnsInstructorForTheClassroomOwner() {
        UUID classroomId = UUID.randomUUID();
        UUID instructorId = UUID.randomUUID();
        when(classroomRepository.findByClassroomId(classroomId))
                .thenReturn(Optional.of(classroom(classroomId, instructorId)));

        assertEquals(
                ChatbotAccessService.ClassroomAccess.INSTRUCTOR,
                service.getAccess(instructorId, classroomId)
        );
    }

    @Test
    void returnsStudentOnlyForAnActiveEnrollment() {
        UUID classroomId = UUID.randomUUID();
        UUID instructorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(classroomRepository.findByClassroomId(classroomId))
                .thenReturn(Optional.of(classroom(classroomId, instructorId)));
        when(classroomStudentRepository.existsByClassroomIdAndStudentUserId(classroomId, studentId))
                .thenReturn(true);

        assertEquals(
                ChatbotAccessService.ClassroomAccess.STUDENT,
                service.getAccess(studentId, classroomId)
        );
    }

    @Test
    void deniesUsersWhoDoNotBelongToTheClassroom() {
        UUID classroomId = UUID.randomUUID();
        UUID instructorId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        when(classroomRepository.findByClassroomId(classroomId))
                .thenReturn(Optional.of(classroom(classroomId, instructorId)));

        assertEquals(
                ChatbotAccessService.ClassroomAccess.NONE,
                service.getAccess(outsiderId, classroomId)
        );
    }

    private Classroom classroom(UUID classroomId, UUID instructorId) {
        Instant now = Instant.now();
        return new Classroom(
                classroomId,
                instructorId,
                "Software Engineering",
                "Classroom description",
                "ABC123",
                ClassroomStatus.ACTIVE,
                now,
                now
        );
    }
}

package com.io.kira.adapter;

import com.io.kira.adapter.activity.in.mapper.AddActivityHttpMapper;
import com.io.kira.adapter.activity.in.mapper.EditActivityHttpMapper;
import com.io.kira.adapter.activity.in.mapper.GetActivityHttpMapper;
import com.io.kira.adapter.activity.in.mapper.MarkStudentAsGradedHttpMapper;
import com.io.kira.adapter.activity.in.mapper.RemoveActivityHttpMapper;
import com.io.kira.adapter.activity.in.mapper.SubmitActivityHttpMapper;
import com.io.kira.adapter.activity.in.mapper.SubmitExistingRepositoryHttpMapper;
import com.io.kira.adapter.activity.in.mapper.SubmitNewRepositoryHttpMapper;
import com.io.kira.adapter.classroom.in.mapper.EditClassroomHttpMapper;
import com.io.kira.adapter.classroom.in.mapper.GetClassroomRecentActivitiesHttpMapper;
import com.io.kira.adapter.classroom.in.mapper.SimpleClassroomHttpMapper;
import com.io.kira.application.activity.error.AddActivityError;
import com.io.kira.application.activity.error.EditActivityError;
import com.io.kira.application.activity.error.GetClassroomOwnerActivityError;
import com.io.kira.application.activity.error.GetClassroomStudentActivityError;
import com.io.kira.application.activity.error.MarkStudentAsGradedError;
import com.io.kira.application.activity.error.RemoveActivityError;
import com.io.kira.application.activity.error.SubmitActivityError;
import com.io.kira.application.activity.error.SubmitExistingRepositoryError;
import com.io.kira.application.activity.error.SubmitNewRepositoryError;
import com.io.kira.application.classroom.error.EditClassroomError;
import com.io.kira.application.classroom.error.GetClassroomRecentActivitiesError;
import com.io.kira.application.classroom.error.SimpleClassroomError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizationHttpMapperTest {

    @Test
    void authenticatedUsersWithoutResourcePermissionReceiveForbiddenNotUnauthorized() {
        assertAll(
                () -> assertEquals(HttpStatus.FORBIDDEN, AddActivityHttpMapper.toStatus(
                        AddActivityError.NOT_CLASSROOM_INSTRUCTOR
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, EditActivityHttpMapper.toStatus(
                        EditActivityError.NOT_INSTRUCTOR
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, GetActivityHttpMapper.ownerToStatus(
                        GetClassroomOwnerActivityError.USER_NOT_CLASSROOM_INSTRUCTOR
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, GetActivityHttpMapper.studentToStatus(
                        GetClassroomStudentActivityError.USER_NOT_CLASSROOM_STUDENT
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, MarkStudentAsGradedHttpMapper.toStatus(
                        MarkStudentAsGradedError.USER_NOT_CLASSROOM_INSTRUCTOR
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, RemoveActivityHttpMapper.toStatus(
                        RemoveActivityError.USER_NOT_CLASSROOM_INSTRUCTOR
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, SubmitActivityHttpMapper.toStatus(
                        SubmitActivityError.USER_NOT_CLASSROOM_STUDENT
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, SubmitExistingRepositoryHttpMapper.toStatus(
                        SubmitExistingRepositoryError.USER_NOT_CLASSROOM_STUDENT
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, SubmitNewRepositoryHttpMapper.toStatus(
                        SubmitNewRepositoryError.USER_NOT_CLASSROOM_STUDENT
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, EditClassroomHttpMapper.toStatus(
                        EditClassroomError.NOT_INSTRUCTOR
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, GetClassroomRecentActivitiesHttpMapper.toStatus(
                        GetClassroomRecentActivitiesError.USER_NOT_CLASSROOM_MEMBER
                )),
                () -> assertEquals(HttpStatus.FORBIDDEN, SimpleClassroomHttpMapper.toStatus(
                        SimpleClassroomError.USER_NOT_CLASSROOM_INSTRUCTOR
                ))
        );
    }
}

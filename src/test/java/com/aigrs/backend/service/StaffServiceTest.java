package com.aigrs.backend.service;

import com.aigrs.backend.dto.response.UserResponse;
import com.aigrs.backend.entity.Department;
import com.aigrs.backend.entity.User;
import com.aigrs.backend.enums.UserRole;
import com.aigrs.backend.exception.ForbiddenException;
import com.aigrs.backend.exception.ResourceNotFoundException;
import com.aigrs.backend.repository.StaffAssignmentRepository;
import com.aigrs.backend.repository.UserRepository;
import com.aigrs.backend.util.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffAssignmentRepository staffAssignmentRepository;

    @InjectMocks
    private StaffService staffService;

    private UUID orgId;
    private UUID staffId;
    private User staffUser;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        staffId = UUID.randomUUID();

        staffUser = User.builder()
                .name("Staff Member")
                .email("staff@example.com")
                .phone("9876543210")
                .role(UserRole.STAFF)
                .isActive(true)
                .emailNotificationsEnabled(true)
                .smsNotificationsEnabled(true)
                .pushNotificationsEnabled(true)
                .build();
        staffUser.setId(staffId);
        staffUser.setOrgId(orgId);

        TenantContext.setOrgId(orgId.toString());
    }

    @Test
    void testGetStaffDetails() {
        // Arrange
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffUser));

        // Act
        UserResponse response = staffService.getStaffDetails(staffId);

        // Assert
        assertNotNull(response);
        assertEquals(staffUser.getId(), response.getId());
        assertEquals(staffUser.getName(), response.getName());
        verify(userRepository, times(1)).findById(staffId);
    }

    @Test
    void testGetStaffDetailsNotFound() {
        // Arrange
        when(userRepository.findById(staffId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> staffService.getStaffDetails(staffId));
    }

    @Test
    void testUpdateNotificationPreferences() {
        // Arrange
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffUser));
        when(userRepository.save(any(User.class))).thenReturn(staffUser);

        // Act
        UserResponse response = staffService.updateNotificationPreferences(staffId, false, true, true);

        // Assert
        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testGetAssignedGrievanceCount() {
        // Arrange
        when(staffAssignmentRepository.countByStaffIdAndIsActive(staffId, true))
                .thenReturn(5L);

        // Act
        Long count = staffService.getAssignedGrievanceCount(staffId);

        // Assert
        assertEquals(5L, count);
    }

    @Test
    void testValidateStaffAccessSuccess() {
        // Arrange
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffUser));

        // Act & Assert
        assertDoesNotThrow(() -> staffService.validateStaffAccess(staffId));
    }

    @Test
    void testValidateStaffAccessNotFound() {
        // Arrange
        when(userRepository.findById(staffId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> staffService.validateStaffAccess(staffId));
    }

    @Test
    void testValidateStaffAccessForbidden() {
        // Arrange
        User citizenUser = User.builder()
                .role(UserRole.CITIZEN)
                .build();
        citizenUser.setId(UUID.randomUUID());
        citizenUser.setOrgId(orgId);

        when(userRepository.findById(citizenUser.getId())).thenReturn(Optional.of(citizenUser));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> staffService.validateStaffAccess(citizenUser.getId()));
    }
}

package com.aigrs.backend.service;

import com.aigrs.backend.dto.request.LoginRequest;
import com.aigrs.backend.dto.request.RegisterRequest;
import com.aigrs.backend.dto.response.AuthResponse;
import com.aigrs.backend.entity.Organization;
import com.aigrs.backend.entity.User;
import com.aigrs.backend.enums.UserRole;
import com.aigrs.backend.exception.DuplicateResourceException;
import com.aigrs.backend.exception.ResourceNotFoundException;
import com.aigrs.backend.repository.OrganizationRepository;
import com.aigrs.backend.repository.UserRepository;
import com.aigrs.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthService authService;

    private UUID orgId;
    private Organization organization;
    private User testUser;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        organization = Organization.builder()
                .code("TEST")
                .name("Test Organization")
                .isActive(true)
                .build();
        organization.setId(orgId);

        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .phone("1234567890")
                .passwordHash("hashed_password")
                .role(UserRole.CITIZEN)
                .isActive(true)
                .build();
        testUser.setOrgId(orgId);
    }

    @Test
    void testRegisterSuccess() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .name("New User")
                .email("newuser@example.com")
                .phone("9876543210")
                .password("password123")
                .orgId(orgId)
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(userRepository.existsByPhoneAndOrgId(request.getPhone(), orgId)).thenReturn(false);
        when(userRepository.existsByEmailAndOrgId(request.getEmail(), orgId)).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act & Assert
        assertDoesNotThrow(() -> authService.register(request));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicatePhone() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .name("New User")
                .email("newuser@example.com")
                .phone("1234567890")
                .password("password123")
                .orgId(orgId)
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(userRepository.existsByPhoneAndOrgId(request.getPhone(), orgId)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void testRegisterOrgNotFound() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .name("New User")
                .email("newuser@example.com")
                .phone("9876543210")
                .password("password123")
                .orgId(UUID.randomUUID())
                .build();

        when(organizationRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> authService.register(request));
    }

    @Test
    void testLoginSuccess() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .phone("1234567890")
                .password("password123")
                .orgId(orgId)
                .build();

        when(userRepository.findByPhoneAndOrgId(request.getPhone(), orgId))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPasswordHash()))
                .thenReturn(true);

        // Act & Assert
        assertDoesNotThrow(() -> authService.login(request));
    }
}

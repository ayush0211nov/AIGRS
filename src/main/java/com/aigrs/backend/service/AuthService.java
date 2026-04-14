package com.aigrs.backend.service;

import com.aigrs.backend.dto.request.*;
import com.aigrs.backend.dto.response.AuthResponse;
import com.aigrs.backend.entity.Organization;
import com.aigrs.backend.entity.User;
import com.aigrs.backend.enums.UserRole;
import com.aigrs.backend.exception.*;
import com.aigrs.backend.repository.OrganizationRepository;
import com.aigrs.backend.repository.UserRepository;
import com.aigrs.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final Random random = new Random();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Verify organization exists
        Organization org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (!org.getIsActive()) {
            throw new BadRequestException("Organization is suspended");
        }

        // Check for duplicate phone
        if (userRepository.existsByPhoneAndOrgId(request.getPhone(), request.getOrgId())) {
            throw new DuplicateResourceException("Phone number already registered in this organization");
        }

        // Check for duplicate email
        if (request.getEmail() != null && userRepository.existsByEmailAndOrgId(request.getEmail(), request.getOrgId())) {
            throw new DuplicateResourceException("Email already registered in this organization");
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CITIZEN;

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        user.setOrgId(request.getOrgId());

        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneAndOrgId(request.getPhone(), request.getOrgId())
                .orElseThrow(() -> new UnauthorizedException("Invalid phone number or password"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid phone number or password");
        }

        return buildAuthResponse(user);
    }

    public void sendOtp(OtpRequest request) {
        // Find user by phone or email
        User user = findUserByPhoneOrEmail(request.getPhoneOrEmail(), request.getOrgId());

        // Generate 6-digit OTP
        String otp = String.format("%06d", random.nextInt(1000000));
        String key = "otp:" + request.getPhoneOrEmail() + ":" + request.getOrgId();

        // Store in Redis with 10 minute TTL
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(10));

        // In dev mode, just log the OTP. In prod, send via SMS/email.
        log.info("OTP for {}: {}", request.getPhoneOrEmail(), otp);

        // TODO: Integrate with SMS/Email service in production
    }

    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        String key = "otp:" + request.getPhoneOrEmail() + ":" + request.getOrgId();
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            throw new BadRequestException("OTP expired or not found. Please request a new one.");
        }

        if (!storedOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        // Delete OTP after successful verification
        redisTemplate.delete(key);

        User user = findUserByPhoneOrEmail(request.getPhoneOrEmail(), request.getOrgId());
        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!jwtUtil.isTokenValid(token)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // Check blacklist
        Boolean isBlacklisted = redisTemplate.hasKey("blacklist:" + token);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            throw new UnauthorizedException("Token has been revoked");
        }

        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
        String orgId = jwtUtil.extractOrgId(token);

        // Generate new access token only
        String newAccessToken = jwtUtil.generateAccessToken(
                UUID.fromString(userId), role, UUID.fromString(orgId));

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token) // Return the same refresh token
                .userId(UUID.fromString(userId))
                .role(UserRole.valueOf(role))
                .orgId(UUID.fromString(orgId))
                .build();
    }

    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token != null && jwtUtil.isTokenValid(token)) {
            long ttl = jwtUtil.getRemainingTtlSeconds(token);
            redisTemplate.opsForValue().set("blacklist:" + token, "1", ttl, TimeUnit.SECONDS);
        }
    }

    public void forgotPassword(OtpRequest request) {
        findUserByPhoneOrEmail(request.getPhoneOrEmail(), request.getOrgId());
        sendOtp(request);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String key = "otp:" + request.getPhoneOrEmail() + ":" + request.getOrgId();
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null || !storedOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        redisTemplate.delete(key);

        User user = findUserByPhoneOrEmail(request.getPhoneOrEmail(), request.getOrgId());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ---- Helpers ----

    private User findUserByPhoneOrEmail(String identifier, UUID orgId) {
        // Try phone first, then email
        return userRepository.findByPhoneAndOrgId(identifier, orgId)
                .or(() -> userRepository.findByEmailAndOrgId(identifier, orgId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name(), user.getOrgId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getRole().name(), user.getOrgId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .orgId(user.getOrgId())
                .build();
    }
}

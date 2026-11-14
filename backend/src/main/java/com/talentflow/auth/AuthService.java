package com.talentflow.auth;

import com.talentflow.application.Application;
import com.talentflow.application.ApplicationRepository;
import com.talentflow.audit.AuditService;
import com.talentflow.auth.dto.AuthResponse;
import com.talentflow.auth.dto.LoginRequest;
import com.talentflow.auth.dto.RefreshRequest;
import com.talentflow.auth.dto.RegisterRequest;
import com.talentflow.auth.dto.UpdateProfileRequest;
import com.talentflow.common.BadRequestException;
import com.talentflow.common.EmailAlreadyUsedException;
import com.talentflow.common.ResourceNotFoundException;
import com.talentflow.notification.DomainEvents;
import com.talentflow.storage.StorageService;
import com.talentflow.user.Role;
import com.talentflow.user.User;
import com.talentflow.user.UserDto;
import com.talentflow.user.UserMapper;
import com.talentflow.user.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final Duration EMAIL_TOKEN_TTL = Duration.ofHours(48);
    private static final Duration PASSWORD_TOKEN_TTL = Duration.ofHours(2);

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final StorageService storageService;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    @Value("${app.auth.require-email-verification:false}")
    private boolean requireEmailVerification;

    /** Inscription publique : toujours le rôle CANDIDATE. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setRole(Role.CANDIDATE);
        user.setEmailVerified(false);
        User saved = userRepository.save(user);
        issueEmailVerification(saved);
        return buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (requireEmailVerification && !user.isEmailVerified()) {
            throw new DisabledException("Email address not verified");
        }
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        String email;
        try {
            email = jwtService.parseRefreshToken(request.refreshToken()).getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        User user = userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return userMapper.toDto(find(userId));
    }

    /** Mise à jour du profil (RF-04). */
    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = find(userId);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setLinkedinUrl(validateUrl(request.linkedinUrl(), "LinkedIn"));
        user.setPhotoUrl(validateUrl(request.photoUrl(), "Photo"));
        return userMapper.toDto(user);
    }

    /** Vérifie une adresse email via le jeton reçu par email (RF-01). */
    @Transactional
    public UserDto verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationTokenHash(AccountTokens.hash(token))
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification link"));
        if (user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired verification link");
        }
        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        return userMapper.toDto(user);
    }

    /** Renvoie un lien de vérification. La réponse est identique que le compte existe ou non. */
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(normalize(email))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::issueEmailVerification);
    }

    /** Mot de passe oublié : on ne révèle jamais si l'adresse existe (RF-03). */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(normalize(email))
                .filter(User::isEnabled)
                .ifPresent(user -> {
                    String token = AccountTokens.randomToken();
                    user.setPasswordResetTokenHash(AccountTokens.hash(token));
                    user.setPasswordResetExpiresAt(Instant.now().plus(PASSWORD_TOKEN_TTL));
                    events.publishEvent(new DomainEvents.PasswordResetRequested(user.getId(), token));
                });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetTokenHash(AccountTokens.hash(token))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link"));
        if (user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired reset link");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        auditService.log(user.getId(), "PASSWORD_RESET", "User", user.getId());
    }

    /**
     * Suppression RGPD d'un compte candidat : les candidatures et CV sont supprimés, puis
     * l'utilisateur est anonymisé afin de conserver l'intégrité du journal d'audit.
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = find(userId);
        if (user.getRole() != Role.CANDIDATE) {
            throw new BadRequestException("Only candidate accounts can be deleted from this endpoint");
        }
        List<Application> applications = applicationRepository.findAllByCandidateId(userId);
        List<String> cvKeys = applications.stream().map(Application::getCvFileKey).toList();
        applicationRepository.deleteAll(applications);

        user.setEmail("deleted-" + user.getId() + "@anonymized.talentflow.local");
        user.setPasswordHash(passwordEncoder.encode(AccountTokens.randomToken()));
        user.setFirstName("Compte");
        user.setLastName("supprimé");
        user.setPhone(null);
        user.setLinkedinUrl(null);
        user.setPhotoUrl(null);
        user.setEnabled(false);
        user.setEmailVerified(false);
        auditService.log(userId, "ACCOUNT_ANONYMIZED", "User", userId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String key : cvKeys) {
                    try {
                        storageService.delete(key);
                    } catch (RuntimeException ex) {
                        log.warn("Could not delete CV {} after account deletion", key, ex);
                    }
                }
            }
        });
    }

    private void issueEmailVerification(User user) {
        String token = AccountTokens.randomToken();
        user.setEmailVerificationTokenHash(AccountTokens.hash(token));
        user.setEmailVerificationExpiresAt(Instant.now().plus(EMAIL_TOKEN_TTL));
        events.publishEvent(new DomainEvents.EmailVerificationRequested(user.getId(), token));
    }

    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user.getEmail(), user.getRole()),
                jwtService.generateRefreshToken(user.getEmail()),
                "Bearer",
                jwtService.accessTokenTtlSeconds(),
                userMapper.toDto(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String validateUrl(String value, String label) {
        String url = blankToNull(value);
        if (url == null) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new BadRequestException(label + " URL must start with http:// or https://");
            }
            return url;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(label + " URL is invalid");
        }
    }
}

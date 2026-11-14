package com.talentflow.auth;

import com.talentflow.auth.dto.AuthResponse;
import com.talentflow.auth.dto.ForgotPasswordRequest;
import com.talentflow.auth.dto.LoginRequest;
import com.talentflow.auth.dto.RefreshRequest;
import com.talentflow.auth.dto.RegisterRequest;
import com.talentflow.auth.dto.ResendVerificationRequest;
import com.talentflow.auth.dto.ResetPasswordRequest;
import com.talentflow.auth.dto.UpdateProfileRequest;
import com.talentflow.auth.dto.VerifyEmailRequest;
import com.talentflow.user.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal CustomUserDetails principal) {
        return authService.me(principal.getId());
    }

    @PatchMapping("/me")
    public UserDto updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        return authService.updateProfile(principal.getId(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CANDIDATE')")
    public void deleteAccount(@AuthenticationPrincipal CustomUserDetails principal) {
        authService.deleteAccount(principal.getId());
    }

    /** Le lien reçu par email contient le jeton brut, jamais stocké en base. */
    @PostMapping("/verify-email")
    public UserDto verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request.token());
    }

    @PostMapping("/verify-email/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
    }
}

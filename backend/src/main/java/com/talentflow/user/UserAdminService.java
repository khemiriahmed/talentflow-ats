package com.talentflow.user;

import com.talentflow.audit.AuditService;
import com.talentflow.common.BadRequestException;
import com.talentflow.common.EmailAlreadyUsedException;
import com.talentflow.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Gestion des comptes par l'Admin (RF-05) + trace dans le journal d'audit (RF-70). */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<UserDto> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    @Transactional
    public UserDto create(CreateUserRequest request, Long adminId) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setEmailVerified(true);
        User saved = userRepository.save(user);
        auditService.log(adminId, "USER_CREATED", "User", saved.getId());
        return userMapper.toDto(saved);
    }

    @Transactional
    public UserDto changeRole(Long userId, Role role, Long adminId) {
        if (userId.equals(adminId)) {
            throw new BadRequestException("You cannot change your own role");
        }
        User user = find(userId);
        user.setRole(role);
        auditService.log(adminId, "USER_ROLE_CHANGED", "User", userId);
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto setEnabled(Long userId, boolean enabled, Long adminId) {
        if (userId.equals(adminId)) {
            throw new BadRequestException("You cannot enable or disable your own account");
        }
        User user = find(userId);
        user.setEnabled(enabled);
        auditService.log(adminId, enabled ? "USER_ENABLED" : "USER_DISABLED", "User", userId);
        return userMapper.toDto(user);
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

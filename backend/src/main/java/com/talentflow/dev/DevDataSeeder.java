package com.talentflow.dev;

import com.talentflow.user.Role;
import com.talentflow.user.User;
import com.talentflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Comptes de démonstration, actifs uniquement avec le profil "dev". Ne jamais utiliser en production. */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seed("admin@talentflow.local", "Admin123!", "Admin", "Root", Role.ADMIN);
        seed("hr@talentflow.local", "Hr123456!", "Hana", "Recruiter", Role.HR);
        seed("manager@talentflow.local", "Manager123!", "Mehdi", "Manager", Role.MANAGER);
    }

    private void seed(String email, String password, String firstName, String lastName, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Seeded dev account {} ({})", email, role);
    }
}

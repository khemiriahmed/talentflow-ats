package com.talentflow.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailVerificationTokenHash(String hash);

    Optional<User> findByPasswordResetTokenHash(String hash);

    List<User> findByRoleInAndEnabledTrueOrderByFirstNameAscLastNameAsc(Collection<Role> roles);
}

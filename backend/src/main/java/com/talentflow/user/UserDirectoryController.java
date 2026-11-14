package com.talentflow.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.List;

/**
 * Annuaire restreint des membres du staff, utilisé pour assigner un manager à une offre
 * et pour choisir les participants d'un entretien. Aucune donnée candidat n'y figure.
 */
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class UserDirectoryController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public List<StaffMemberDto> staff() {
        return userRepository
                .findByRoleInAndEnabledTrueOrderByFirstNameAscLastNameAsc(
                        EnumSet.of(Role.HR, Role.MANAGER, Role.ADMIN))
                .stream()
                .map(user -> new StaffMemberDto(user.getId(), user.getFirstName(), user.getLastName(), user.getRole()))
                .toList();
    }
}

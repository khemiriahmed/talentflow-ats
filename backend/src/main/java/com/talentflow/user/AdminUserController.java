package com.talentflow.user;

import com.talentflow.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    public Page<UserDto> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return userAdminService.list(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest request,
                          @AuthenticationPrincipal CustomUserDetails admin) {
        return userAdminService.create(request, admin.getId());
    }

    @PatchMapping("/{id}/role")
    public UserDto changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request,
                              @AuthenticationPrincipal CustomUserDetails admin) {
        return userAdminService.changeRole(id, request.role(), admin.getId());
    }

    @PatchMapping("/{id}/enabled")
    public UserDto setEnabled(@PathVariable Long id, @Valid @RequestBody SetEnabledRequest request,
                              @AuthenticationPrincipal CustomUserDetails admin) {
        return userAdminService.setEnabled(id, request.enabled(), admin.getId());
    }
}

package com.talentflow.notification;

import com.talentflow.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Notifications in-app ("cloche") de l'utilisateur connecté. */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    public record UnreadCount(long count) {
    }

    public record ReadAllResult(int updated) {
    }

    private final InAppNotificationService service;

    @GetMapping
    public Page<NotificationDto> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @AuthenticationPrincipal CustomUserDetails user) {
        return service.list(user.getId(), unreadOnly, page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCount unreadCount(@AuthenticationPrincipal CustomUserDetails user) {
        return new UnreadCount(service.unreadCount(user.getId()));
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markRead(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return service.markRead(id, user.getId());
    }

    @PostMapping("/read-all")
    public ReadAllResult markAllRead(@AuthenticationPrincipal CustomUserDetails user) {
        return new ReadAllResult(service.markAllRead(user.getId()));
    }
}

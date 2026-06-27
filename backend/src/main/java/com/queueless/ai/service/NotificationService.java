package com.queueless.ai.service;

import com.queueless.ai.dto.NotificationDtos.NotificationResponse;
import com.queueless.ai.dto.PageResponse;
import com.queueless.ai.entity.Notification;
import com.queueless.ai.entity.User;
import com.queueless.ai.exception.ResourceNotFoundException;
import com.queueless.ai.repository.NotificationRepository;
import com.queueless.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.notifications.email-enabled}")
    private boolean emailEnabled;

    @Value("${app.notifications.from}")
    private String mailFrom;

    @Transactional
    public NotificationResponse notifyUser(User user, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .build();
        Notification saved = notificationRepository.save(notification);
        sendEmail(user, title, message);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForUser(Long userId, int page, int size) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderBySentAtDesc(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"))
        );
        return new PageResponse<>(
                notifications.getContent().stream().map(this::toResponse).toList(),
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages()
        );
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", id);
        }
        notification.setReadAt(java.time.Instant.now());
        return toResponse(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getSentAt(),
                notification.getReadAt()
        );
    }

    private void sendEmail(User user, String title, String message) {
        if (!emailEnabled) {
            return;
        }
        
        // Log the email content to the console for easy local development testing!
        log.info("================ EMAIL NOTIFICATION ================\nTo: {}\nSubject: {}\n\n{}\n==================================================", user.getEmail(), title, message);

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, "utf-8");
                
                String display = "QueueLess AI";
                String email = mailFrom;
                if (mailFrom.contains("<") && mailFrom.endsWith(">")) {
                    int start = mailFrom.indexOf("<");
                    display = mailFrom.substring(0, start).trim();
                    email = mailFrom.substring(start + 1, mailFrom.length() - 1).trim();
                }
                
                helper.setFrom(email, display);
                helper.setTo(user.getEmail());
                helper.setSubject(title);
                helper.setText(message, false);
                mailSender.send(mimeMessage);
            } catch (Exception exception) {
                log.warn("Failed to send email notification via SMTP (Did you set the MAIL_PASSWORD app password in application.yml?): {}", exception.getMessage());
            }
        });
    }
}

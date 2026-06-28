package com.queueless.ai.service;

import com.queueless.ai.entity.Token;
import com.queueless.ai.repository.TokenRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nightly scheduled job that activates future-dated tokens at midnight.
 * Assigns queue positions based on booking order (first-booked = first-served).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenSchedulerService {

    private final TokenRepository tokenRepository;
    private final NotificationService notificationService;

    /**
     * Runs every day at 12:01 AM.
     * Finds all WAITING tokens whose scheduledDate is today,
     * assigns their queue position, and notifies users.
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void activateScheduledTokens() {
        LocalDate today = LocalDate.now();
        log.info("TokenSchedulerService: Activating scheduled tokens for date {}", today);

        try {
            List<Token> todayTokens = tokenRepository.findWaitingByScheduledDate(today);
            log.info("TokenSchedulerService: Found {} token(s) to activate for {}", todayTokens.size(), today);

            for (int i = 0; i < todayTokens.size(); i++) {
                Token token = todayTokens.get(i);
                token.setQueuePosition(i + 1);
                tokenRepository.save(token);

                try {
                    notificationService.notifyUser(
                            token.getUser(),
                            "Your event is today! Token: " + token.getTokenNumber(),
                            "Your scheduled booking is active today at " +
                                    token.getCounter().getCounterName() + ".\n" +
                                    "You are #" + (i + 1) + " in the queue.\n" +
                                    "Please show your QR code at the counter."
                    );
                } catch (Exception emailException) {
                    log.error("TokenSchedulerService: Failed to notify user {} for token {}",
                            token.getUser().getEmail(), token.getTokenNumber(), emailException);
                }
            }

            log.info("TokenSchedulerService: Successfully activated {} token(s) for {}", todayTokens.size(), today);

        } catch (Exception exception) {
            log.error("TokenSchedulerService: Critical error during scheduled token activation for {}", today, exception);
        }
    }
}

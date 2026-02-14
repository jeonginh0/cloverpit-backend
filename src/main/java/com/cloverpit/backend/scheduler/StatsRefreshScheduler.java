package com.cloverpit.backend.scheduler;

import com.cloverpit.backend.service.PubgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsRefreshScheduler {

    private final PubgService pubgService;

    /**
     * 매일 자정(00:00)에 전적 자동 갱신
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void refreshStats() {
        log.info("⏰ Starting scheduled stats refresh...");
        try {
            pubgService.refreshAllPlayersStats();
            log.info("✅ Scheduled stats refresh completed successfully");
        } catch (Exception e) {
            log.error("❌ Scheduled stats refresh failed", e);
        }
    }
}

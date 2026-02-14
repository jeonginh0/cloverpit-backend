package com.cloverpit.backend.service;

import com.cloverpit.backend.dto.ApplicationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DiscordWebhookService {

    @Value("${discord.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendApplicationNotification(ApplicationRequest application) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Discord webhook URL not configured. Skipping notification.");
            return;
        }

        try {
            // 디스코드 임베드 메시지 생성
            Map<String, Object> embed = new HashMap<>();
            embed.put("title", "🎮 새로운 클랜 가입 신청");
            embed.put("color", 0x7c3aed); // 보라색
            embed.put("timestamp", java.time.Instant.now().toString());
            
            // 필드 추가
            embed.put("fields", List.of(
                Map.of("name", "PUBG 닉네임", "value", application.getPubgName(), "inline", true),
                Map.of("name", "Discord 닉네임", "value", application.getDiscordName(), "inline", true),
                Map.of("name", "나이", "value", String.valueOf(application.getAge()), "inline", true),
                Map.of("name", "자기소개", "value", application.getIntroduction(), "inline", false)
            ));

            // 웹훅 페이로드
            Map<String, Object> payload = new HashMap<>();
            payload.put("embeds", List.of(embed));

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // 디스코드 웹훅으로 전송
            restTemplate.postForEntity(webhookUrl, request, String.class);
            
            log.info("Successfully sent application notification to Discord for: {}", application.getPubgName());
        } catch (Exception e) {
            log.error("Failed to send Discord webhook notification", e);
        }
    }
}

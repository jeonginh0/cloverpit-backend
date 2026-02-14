package com.cloverpit.backend.service;

import com.cloverpit.backend.dto.ApplicationRequest;
import com.cloverpit.backend.model.Application;
import com.cloverpit.backend.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final DiscordWebhookService discordWebhookService;

    public List<Application> getAllApplications() {
        return applicationRepository.findAllByOrderByCreatedAtDesc();
    }

    public Application createApplication(ApplicationRequest request) {
        // 디스코드 웹훅으로 알림 전송
        discordWebhookService.sendApplicationNotification(request);
        
        Application application = Application.builder()
                .pubgName(request.getPubgName())
                .discordName(request.getDiscordName())
                .age(request.getAge())
                .introduction(request.getIntroduction())
                .status(Application.ApplicationStatus.PENDING)
                .build();

        return applicationRepository.save(application);
    }

    public Application updateStatus(String id, Application.ApplicationStatus status) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        
        application.setStatus(status);
        return applicationRepository.save(application);
    }

    public void deleteApplication(String id) {
        applicationRepository.deleteById(id);
    }
}

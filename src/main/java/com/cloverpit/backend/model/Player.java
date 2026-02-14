package com.cloverpit.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "players")
public class Player {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String pubgName;
    
    private String discordName;
    
    private String pubgId;
    
    private PlayerStats stats;
    
    private Double score;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerStats {
        // 전체 통계
        @Builder.Default
        private Integer kills = 0;
        @Builder.Default
        private Integer deaths = 0;
        @Builder.Default
        private Double kd = 0.0;
        @Builder.Default
        private Double averageDamage = 0.0;
        @Builder.Default
        private Integer totalMatches = 0;
        @Builder.Default
        private Integer wins = 0;
        @Builder.Default
        private Integer top10 = 0;
        
        // 경쟁전 통계
        @Builder.Default
        private Integer rankedMatches = 0;
        @Builder.Default
        private Double rankedKd = 0.0;
        @Builder.Default
        private Double rankedAvgDamage = 0.0;
        @Builder.Default
        private Integer rankedWins = 0;
        
        // 일반전 통계
        @Builder.Default
        private Integer normalMatches = 0;
        @Builder.Default
        private Double normalKd = 0.0;
        @Builder.Default
        private Double normalAvgDamage = 0.0;
        @Builder.Default
        private Integer normalWins = 0;
        
        // 모드별 플레이 수
        @Builder.Default
        private Integer soloMatches = 0;
        @Builder.Default
        private Integer duoMatches = 0;
        @Builder.Default
        private Integer squadMatches = 0;
    }
}

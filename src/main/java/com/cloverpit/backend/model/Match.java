package com.cloverpit.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "matches")
public class Match {
    
    @Id
    private String id;
    
    private String playerId;
    
    private String playerName;
    
    private String gameMode; // solo, duo, squad
    
    private String matchType; // ranked, normal
    
    private Integer kills;
    
    private Integer deaths;
    
    private Double damage;
    
    private Integer placement;
    
    private Integer timeSurvived;
    
    private String matchId;
    
    private String seasonId; // PUBG 시즌 ID
    
    // 매치 참가자 닉네임 목록
    private List<String> participants;
    
    // PUBG API에서 받은 실제 매치 플레이 시간
    private LocalDateTime createdAt;
}

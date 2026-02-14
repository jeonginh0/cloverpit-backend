package com.cloverpit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClanStatsResponse {
    private Integer totalMembers;
    private Double avgKD;
    private Double avgDamage;
    private Integer totalMatches;
}

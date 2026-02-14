package com.cloverpit.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequest {
    @NotBlank
    private String pubgName;
    
    @NotBlank
    private String discordName;
    
    @NotNull
    @Min(18)
    private Integer age;
    
    @NotBlank
    private String introduction;
}

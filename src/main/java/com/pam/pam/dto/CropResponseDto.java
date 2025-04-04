package com.pam.pam.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropResponseDto {
    private String cropName;
    private String description;
    private String cultivationMethod;
}
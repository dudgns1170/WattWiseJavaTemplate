package com.app.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DailyTemperatureAvgDto {

    private OffsetDateTime dayStart;

    private Long count;

    private Double avgTemperature;
}

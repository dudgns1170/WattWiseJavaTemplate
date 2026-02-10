package com.app.entity.timeseries;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDataEntity {
    private OffsetDateTime time;
    private Integer sensorId;
    private Double temperature;
    private Double cpu;
}

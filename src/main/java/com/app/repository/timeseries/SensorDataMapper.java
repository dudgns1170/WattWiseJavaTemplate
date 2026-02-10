package com.app.repository.timeseries;

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.app.dto.DailyTemperatureAvgDto;
import com.app.entity.timeseries.SensorDataEntity;
import com.app.repository.TimescaleMapper;

@TimescaleMapper
public interface SensorDataMapper {

    int insert(SensorDataEntity data);

    List<SensorDataEntity> findBySensorIdAndTimeRange(@Param("sensorId") Integer sensorId,
                                                      @Param("start") OffsetDateTime start,
                                                      @Param("end") OffsetDateTime end);

    List<DailyTemperatureAvgDto> findDailyAvgTemperatureBySensorIdAndTimeRange(@Param("sensorId") Integer sensorId,
                                                                               @Param("start") OffsetDateTime start,
                                                                               @Param("end") OffsetDateTime end);
}

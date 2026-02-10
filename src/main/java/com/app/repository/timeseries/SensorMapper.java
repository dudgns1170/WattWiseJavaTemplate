package com.app.repository.timeseries;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import com.app.entity.timeseries.SensorEntity;
import com.app.repository.TimescaleMapper;

@TimescaleMapper
public interface SensorMapper {

    List<SensorEntity> findAll();

    Optional<SensorEntity> findById(@Param("id") Integer id);

    int insertIfNotExists(@Param("id") Integer id,
                          @Param("type") String type,
                          @Param("location") String location);
}

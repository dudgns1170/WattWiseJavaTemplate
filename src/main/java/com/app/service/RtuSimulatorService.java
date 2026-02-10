package com.app.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.app.entity.timeseries.SensorDataEntity;
import com.app.repository.timeseries.SensorDataMapper;
import com.app.repository.timeseries.SensorMapper;

/**
 * RTU 시뮬레이터 서비스
 *
 * TimescaleDB(sensor_data)에 측정 데이터를 적재하는 시뮬레이션 기능을 제공합니다.
 */
@Service
public class RtuSimulatorService {

    private static final List<Integer> SENSOR_IDS = Arrays.asList(
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10
    );

    private final SensorMapper sensorMapper;
    private final SensorDataMapper sensorDataMapper;

    public RtuSimulatorService(SensorMapper sensorMapper, SensorDataMapper sensorDataMapper) {
        this.sensorMapper = sensorMapper;
        this.sensorDataMapper = sensorDataMapper;
    }

    /**
     * 특정 sensorId에 대해 임의의 측정 데이터 1건을 생성하여 sensor_data 테이블에 insert
     * 
     * @return 저장된 레코드의 식별 문자열
     */
    public String sendRandomSensorData(Integer sensorId) {
        sensorMapper.insertIfNotExists(sensorId, "sim", "sensor-" + sensorId);

        Random rnd = new Random();
        double temperature = 10 + rnd.nextDouble() * 30;
        double cpu = rnd.nextDouble() * 100;

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
        SensorDataEntity data = SensorDataEntity.builder()
                .time(now)
                .sensorId(sensorId)
                .temperature(temperature)
                .cpu(cpu)
                .build();

        sensorDataMapper.insert(data);
        return sensorId + "@" + now;
    }

    /***
     * 데이터 10곳 바로 오는는 Test
     */
    public List<String> sendRandomSensorDataForAllSensorsOnce() {
        List<String> ids = new ArrayList<>();
        for (Integer sensorId : SENSOR_IDS) {
            ids.add(sendRandomSensorData(sensorId));
        }
        return ids;
    }

    /**
     * 1분마다 각 sensorId에 대해 한 건씩 측정 데이터를 적재
     */
    @Scheduled(fixedRate = 60 * 1000L)
    public void sendRandomSensorDataPeriodically() {
        for (Integer sensorId : SENSOR_IDS) {
            sendRandomSensorData(sensorId);
        }
    }
}
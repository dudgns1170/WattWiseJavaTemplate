package com.app.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.app.dto.DailyTemperatureAvgDto;
import com.app.entity.timeseries.SensorDataEntity;
import com.app.repository.timeseries.SensorDataMapper;


/**
 * TimescaleDB(sensor_data) 데이터를 집계하는 서비스.
 */
@Service
public class LedgerHistoryService {

    private final SensorDataMapper sensorDataMapper;

    public LedgerHistoryService(SensorDataMapper sensorDataMapper) {
        this.sensorDataMapper = sensorDataMapper;
    }

    /**
     * 주어진 sensorId와 시작 시각(UTC 기준)을 기준으로 1시간 구간의 sensor_data를 집계한다.
     *
     * @param sensorId     집계 대상 sensor ID
     * @param hourStartUtc 집계 구간 시작 시각 (UTC 기준, 시 단위로 내림 처리됨)
     * @return 생성된 LedgerHistory st_id
     */
    public String aggregateHourForSensor(Integer sensorId, ZonedDateTime hourStartUtc) {
        ZonedDateTime hourStart = hourStartUtc
                .withZoneSameInstant(ZoneOffset.UTC)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        ZonedDateTime hourEnd = hourStart.plusHours(1);

        OffsetDateTime start = hourStart.toOffsetDateTime();
        OffsetDateTime end = hourEnd.toOffsetDateTime();

        List<SensorDataEntity> items = sensorDataMapper.findBySensorIdAndTimeRange(sensorId, start, end);
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("No sensor_data found for sensorId=" + sensorId + " and hour=" + hourStart.toInstant());
        }

        double sumTemp = 0.0;
        double sumCpu = 0.0;
        for (SensorDataEntity item : items) {
            if (item.getTemperature() != null) {
                sumTemp += item.getTemperature();
            }
            if (item.getCpu() != null) {
                sumCpu += item.getCpu();
            }
        }

        double avgTemp = sumTemp / items.size();
        double avgCpu = sumCpu / items.size();

        String payload = "avgTemp=" + avgTemp + ",avgCpu=" + avgCpu + ",count=" + items.size();
        String snapshotHash = Base64.getEncoder().encodeToString(payload.getBytes());

        return "hour_" + sensorId + "_" + hourStart.toInstant() + "_" + snapshotHash;
    }

    /**
     * 주어진 sensorId와 15분 구간 시작 시각(UTC 기준)을 기준으로 해당 구간의 sensor_data를 집계한다.
     *
     * @param sensorId         집계 대상 sensor ID
     * @param quarterStartUtc  집계 구간 시작 시각 (UTC 기준, 15분 단위로 내림 처리됨)
     * @return 생성된 LedgerHistory st_id
     */
    public String aggregateQuarterForSensor(Integer sensorId, ZonedDateTime quarterStartUtc) {
        ZonedDateTime quarterStart = quarterStartUtc
                .withZoneSameInstant(ZoneOffset.UTC)
                .withSecond(0)
                .withNano(0);

        int minute = quarterStart.getMinute();
        int roundedMinute = (minute / 15) * 15;
        quarterStart = quarterStart.withMinute(roundedMinute);
        ZonedDateTime quarterEnd = quarterStart.plusMinutes(15);

        OffsetDateTime start = quarterStart.toOffsetDateTime();
        OffsetDateTime end = quarterEnd.toOffsetDateTime();

        List<SensorDataEntity> items = sensorDataMapper.findBySensorIdAndTimeRange(sensorId, start, end);
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("No sensor_data found for sensorId=" + sensorId + " and quarterStart=" + quarterStart.toInstant());
        }

        String payload = "count=" + items.size() + ",createdAt=" + Instant.now();
        String snapshotHash = Base64.getEncoder().encodeToString(payload.getBytes());
        return "quarter_" + sensorId + "_" + quarterStart.toInstant() + "_" + snapshotHash;
    }

    public List<DailyTemperatureAvgDto> findDailyAvgTemperature(Integer sensorId, ZonedDateTime startUtc, ZonedDateTime endUtc) {
        ZonedDateTime start = startUtc
                .withZoneSameInstant(ZoneOffset.UTC)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        ZonedDateTime end = endUtc
                .withZoneSameInstant(ZoneOffset.UTC)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start. start=" + start.toInstant() + ", end=" + end.toInstant());
        }

        OffsetDateTime startOffset = start.toOffsetDateTime();
        OffsetDateTime endOffset = end.toOffsetDateTime();

        return sensorDataMapper.findDailyAvgTemperatureBySensorIdAndTimeRange(sensorId, startOffset, endOffset);
    }

    /**
     * 15분마다 직전 15분 구간을 자동 집계하여 LedgerHistory에 적재한다.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void aggregateLastQuarterPeriodically() {
        // 1. 현재 UTC 시각 기준으로 직전 15분 구간의 시작 시각 계산
        ZonedDateTime nowUtc = Instant.now().atZone(ZoneOffset.UTC);
        ZonedDateTime targetStart = nowUtc.minusMinutes(15);

        Integer sensorId = 1; // TODO: 나중에는 활성 sensor 목록을 조회하도록 변경

        try {
            String ledgerId = aggregateQuarterForSensor(sensorId, targetStart);
            System.out.println("[LEDGER][15m] sensorId=" + sensorId
                    + ", quarterStartUtc=" + targetStart
                    + ", ledgerId=" + ledgerId);
        } catch (Exception e) {
            // 테스트용으로 단순 로그 출력만 수행
            System.out.println("[LEDGER][15m][ERROR] sensorId=" + sensorId
                    + ", quarterStartUtc=" + targetStart
                    + ", message=" + e.getMessage());
        }
    }
}

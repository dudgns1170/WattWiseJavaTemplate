package com.app.controller;

import com.app.common.ApiResponse;
import com.app.dto.DailyTemperatureAvgDto;
import com.app.service.LedgerHistoryService;
import com.app.service.RtuSimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rtu")
@Tag(name = "RTU 시뮬레이터", description = "TimescaleDB(sensor_data) 측정 데이터 적재/집계 시뮬레이션 API")
public class RtuSimulatorController {

	private final RtuSimulatorService rtuSimulatorService;
	private final LedgerHistoryService ledgerHistoryService;

	public RtuSimulatorController(RtuSimulatorService rtuSimulatorService, LedgerHistoryService ledgerHistoryService) {
		this.rtuSimulatorService = rtuSimulatorService;
		this.ledgerHistoryService = ledgerHistoryService;
	}

	/**
	 * 임의의 측정 데이터(temperature/cpu) 여러 건을 TimescaleDB(sensor_data)에 적재.
	 * 
	 * @return 생성된 식별 문자열 목록
	 */
	@Operation(summary = "측정 데이터 적재", description = "임의의 측정 데이터를 TimescaleDB(sensor_data)에 적재")
	@PostMapping("/send-once")
	public ResponseEntity<ApiResponse> sendOnce() {
		/// 단건
		// String stId = rtuSimulatorService.sendRandomSettlement();
		// 복수건
		List<String> ids = rtuSimulatorService.sendRandomSensorDataForAllSensorsOnce();
		ApiResponse body = ApiResponse.ok(java.util.Map.of("message", "sensor_data inserted", "ids", ids));
		System.out.println("body::::" + body);
		return ResponseEntity.ok(body);
	}

	@Operation(summary = "1시간 집계", description = "특정 sensorId의 1시간 구간 sensor_data를 집계")

	@PostMapping("/aggregate-hour")
	public ResponseEntity<ApiResponse> aggregateHour(@RequestParam("sensorId") Integer sensorId,
			@RequestParam("hourStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime hourStart,
			@RequestHeader("X-Client-Platform") String clientPlatform) {
		String ledgerId = ledgerHistoryService.aggregateHourForSensor(sensorId, hourStart);
		ApiResponse body = ApiResponse
				.ok(java.util.Map.of("message", "Hourly sensor_data aggregated", "ledger_id", ledgerId));
		return ResponseEntity.ok(body);
	}

	@Operation(summary = "1일 평균 집계", description = "특정 sensorId의 일 단위 평균 temperature를 집계 (TimescaleDB sensor_data 기반)")
	@PostMapping("/aggregate-day")
	public ResponseEntity<ApiResponse> aggregateDay(@RequestParam("sensorId") Integer sensorId,
			@RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
			@RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end,
			@RequestHeader("X-Client-Platform") String clientPlatform) {
		List<DailyTemperatureAvgDto> items = ledgerHistoryService.findDailyAvgTemperature(sensorId, start, end);
		ApiResponse body = ApiResponse.ok(java.util.Map.of("message", "Daily average temperature aggregated", "items", items));
		return ResponseEntity.ok(body);
	}
}

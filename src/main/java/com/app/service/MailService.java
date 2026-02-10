package com.app.service;

import com.app.config.AppProps;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 이메일 인증 서비스
 * 
 * 이메일 인증 코드 생성, 전송, 검증 기능을 제공합니다.
 * 인증 코드는 Redis에 TTL과 함께 저장됩니다.
 */
@Service
@RequiredArgsConstructor
public class MailService {
	private final JavaMailSender mailSender;
	private final AppProps appProps;
	private final StringRedisTemplate redisTemplate;

	/**
	 * 이메일로 인증 코드 전송
	 * 
	 * @param email 수신자 이메일 주소
	 */
	public void sendCodeToEmail(String email) {
		String code = createAuthCode();
		sendMail(email, code);
		storeCode(email, code);
	}

	/**
	 * 실제 메일 전송 처리
	 */
	private void sendMail(String email, String code) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setSubject("이메일 인증 코드");
		message.setText("인증 코드는 " + code + " 입니다.");
		message.setFrom(appProps.getMail().getFrom());
		mailSender.send(message);
	}

	private String createAuthCode() {
		return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
	}

	private void storeCode(String email, String code) {
		long expireMillis = appProps.getMail().getAuthCodeExpirationMillis();
		
		redisTemplate.opsForValue().set("MAIL:AUTH:" + email, code, Duration.ofMillis(expireMillis));
	}
	
	/**
	 * 인증 코드 검증
	 * 
	 * @param email 이메일 주소
	 * @param inputCode 사용자가 입력한 인증 코드
	 * @throws IllegalStateException 인증 코드가 만료되었거나 존재하지 않을 때
	 * @throws IllegalArgumentException 인증 코드가 일치하지 않을 때
	 */
	public void verifyCode(String email, String inputCode) {
	    String key = "MAIL:AUTH:" + email;
	    String saved = redisTemplate.opsForValue().get(key);
	    if (saved == null) {
	        throw new IllegalStateException("인증 코드가 만료되었거나 존재하지 않습니다.");
	    }
	    if (!saved.equals(inputCode)) {
	        throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
	    }
	    redisTemplate.delete(key); // 일회성 사용이면 삭제
	}
}
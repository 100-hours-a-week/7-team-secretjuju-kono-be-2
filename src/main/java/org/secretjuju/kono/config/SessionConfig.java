package org.secretjuju.kono.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SessionConfig {

	@Bean
	public CookieSerializer cookieSerializer() {
		log.info("세션 쿠키 설정을 초기화합니다...");

		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("JSESSIONID"); // 스프링 부트 기본 세션 쿠키 이름으로 변경
		log.info("세션 쿠키 이름: JSESSIONID");

		serializer.setCookiePath("/"); // 전체 경로에서 쿠키 사용 가능
		log.info("쿠키 경로: /");

		// SameSite 설정 - 크로스 도메인 요청에서 쿠키가 전송되도록 설정
		serializer.setSameSite("None"); // CORS 환경에서 쿠키를 전송하기 위해 필요
		log.info("SameSite 설정: None");

		// 개발 환경에서는 false로 설정하여 HTTP로도 쿠키가 전송되도록 함
		// 프로덕션 환경에서는 true로 설정해야 함
		serializer.setUseSecureCookie(false);
		log.info("Secure 쿠키 사용: false (개발 환경)");

		serializer.setUseHttpOnlyCookie(true); // JavaScript에서 쿠키 접근 방지
		log.info("HttpOnly 쿠키 사용: true");

		serializer.setCookieMaxAge(-1); // -1: 브라우저 종료 시 삭제
		log.info("쿠키 최대 수명: -1 (브라우저 종료 시 삭제)");

		// localhost 도메인 설정
		serializer.setDomainNamePattern("^.+$"); // 모든 도메인에서 쿠키 사용 가능
		log.info("쿠키 도메인 패턴: ^.+$ (모든 도메인)");

		log.info("세션 쿠키 설정이 완료되었습니다.");
		return serializer;
	}

	@Bean
	public HttpSessionIdResolver httpSessionIdResolver() {
		log.info("HttpSessionIdResolver 설정: CookieHttpSessionIdResolver 사용");
		return new CookieHttpSessionIdResolver();
	}
}
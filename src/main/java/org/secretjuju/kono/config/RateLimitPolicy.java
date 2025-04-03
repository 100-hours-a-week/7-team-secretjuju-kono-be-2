package org.secretjuju.kono.config;

import lombok.Getter;

@Getter
public enum RateLimitPolicy {
	// 인증 관련 엔드포인트 (IP 기반)
	OAUTH_LOGIN("/oauth2/**", 30, 1), // OAuth 로그인 1분당 5회->30회

	// 인증된 사용자의 서비스 엔드포인트 (세션 기반)
	COINS("/api/v1/coins/**", 300, 1), // 코인 조회및 매도매수 1분당 60회->300
	USER_UPDATE("/api/v1/users/**", 100, 1), // 사용자 정보 수정 1분당 20회->100
	DEFAULT("/api/**", 300, 1); // 기타 API 요청 60->300

	private final String pathPattern;
	private final int limit;
	private final int minutes;

	RateLimitPolicy(String pathPattern, int limit, int minutes) {
		this.pathPattern = pathPattern;
		this.limit = limit;
		this.minutes = minutes;
	}

}
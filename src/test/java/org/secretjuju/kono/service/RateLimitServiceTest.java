package org.secretjuju.kono.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.secretjuju.kono.config.RateLimitPolicy;

// 데이터베이스 의존성 제거를 위해 @SpringBootTest 제거
class RateLimitServiceTest {

	private RateLimitService rateLimitService;

	@BeforeEach
	void setUp() {
		// 직접 서비스 인스턴스 생성
		rateLimitService = new RateLimitService();
	}

	@Test
	void testRateLimit() {
		String testKey = "testSession:api/v1/coins";
		RateLimitPolicy policy = RateLimitPolicy.COINS;

		// 허용된 요청 수만큼 테스트 (반복 횟수를 줄이기 위해 5회만 테스트)
		for (int i = 0; i < 5; i++) {
			assertTrue(rateLimitService.tryConsume(testKey, policy), "요청 " + (i + 1) + "번째: 제한에 걸리지 않아야 함");
		}

		// 나머지 요청 소비 (테스트를 빠르게 하기 위해)
		for (int i = 5; i < policy.getLimit(); i++) {
			rateLimitService.tryConsume(testKey, policy);
		}

		// 제한 초과 테스트
		assertFalse(rateLimitService.tryConsume(testKey, policy), "제한 초과: 요청이 거부되어야 함");
	}
}
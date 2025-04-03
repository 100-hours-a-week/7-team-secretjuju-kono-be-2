package org.secretjuju.kono.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.secretjuju.kono.config.RateLimitPolicy;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RateLimitService {
	private final Cache<String, Bucket> buckets;

	public RateLimitService() {
		this.buckets = Caffeine.newBuilder().maximumSize(100000).expireAfterWrite(1, TimeUnit.HOURS).build();
	}

	/**
	 * 주어진 키와 정책에 따라 요청이 허용되는지 확인
	 * 
	 * @param key
	 *            요청 식별 키
	 * @param policy
	 *            적용할 정책
	 * @return 요청이 허용되면 true, 제한되면 false
	 */
	public boolean tryConsume(String key, RateLimitPolicy policy) {
		Bucket bucket = buckets.get(key, k -> createNewBucket(policy));
		boolean consumed = bucket.tryConsume(1);
		if (!consumed) {
			log.warn("Rate limit exceeded for key: {}, policy: {}", key, policy.name());
		}
		return consumed;
	}

	/**
	 * 정책에 따른 새 버킷을 생성
	 */
	private Bucket createNewBucket(RateLimitPolicy policy) {
		Refill refill = Refill.greedy(policy.getLimit(), Duration.ofMinutes(policy.getMinutes()));
		Bandwidth limit = Bandwidth.classic(policy.getLimit(), refill);
		return Bucket4j.builder().addLimit(limit).build();
	}

	/**
	 * 요청에 대한 고유 키를 생성 OAuth 요청은 IP 기반, 인증된 요청은 세션 ID 기반으로 생성
	 */
	public String generateKey(HttpServletRequest request, HttpSession session) {
		String path = request.getRequestURI();

		// OAuth 로그인 관련 경로는 IP 기반으로 제한
		if (path.startsWith("/oauth2/")) {
			return getClientIP(request) + ":" + path;
		}

		// 나머지 API는 세션 ID 기반으로 제한
		return session != null ? session.getId() + ":" + path : getClientIP(request) + ":" + path;
	}

	/**
	 * 클라이언트 IP를 가져와서 프록시 환경을 고려
	 */
	private String getClientIP(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null || xfHeader.isEmpty()) {
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0].trim();
	}

	/**
	 * 특정 키의 현재 남은 요청 횟수를 확인 모니터링 및 디버깅용
	 */
	public long getAvailableTokens(String key, RateLimitPolicy policy) {
		Bucket bucket = buckets.getIfPresent(key);
		if (bucket == null) {
			bucket = createNewBucket(policy);
			buckets.put(key, bucket);
		}
		return bucket.getAvailableTokens();
	}

	/**
	 * 특정 키의 버킷을 리셋 테스트용
	 */
	public void resetBucket(String key) {
		buckets.invalidate(key);
	}
}
package org.secretjuju.kono.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.secretjuju.kono.dto.response.DailyRankingResponseDto;
import org.secretjuju.kono.dto.response.TotalRankingResponseDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;

	// 캐시 키 정의
	private static final String DAILY_RANKING_KEY = "daily:ranking";
	private static final String TOTAL_RANKING_KEY = "total:ranking";
	private static final String USER_DAILY_RANKING_KEY = "user:daily:ranking:";
	private static final String USER_TOTAL_RANKING_KEY = "user:total:ranking:";
	private static final long CACHE_EXPIRY = 5; // 분 단위

	// 일간 랭킹 전체 저장
	public void cacheDailyRankings(List<DailyRankingResponseDto> rankings) {
		try {
			// 기존 캐시 삭제
			redisTemplate.delete(DAILY_RANKING_KEY);

			// 객체를 JSON으로 직렬화하여 저장
			String rankingsJson = objectMapper.writeValueAsString(rankings);
			redisTemplate.opsForValue().set(DAILY_RANKING_KEY, rankingsJson, CACHE_EXPIRY, TimeUnit.MINUTES);
			log.info("일간 랭킹 캐시 업데이트 완료: {} 개 항목", rankings.size());
		} catch (JsonProcessingException e) {
			log.error("일간 랭킹 캐시 저장 실패", e);
		}
	}

	// 전체 랭킹 저장
	public void cacheTotalRankings(List<TotalRankingResponseDto> rankings) {
		try {
			redisTemplate.delete(TOTAL_RANKING_KEY);
			String rankingsJson = objectMapper.writeValueAsString(rankings);
			redisTemplate.opsForValue().set(TOTAL_RANKING_KEY, rankingsJson, CACHE_EXPIRY, TimeUnit.MINUTES);
			log.info("전체 랭킹 캐시 업데이트 완료: {} 개 항목", rankings.size());
		} catch (JsonProcessingException e) {
			log.error("전체 랭킹 캐시 저장 실패", e);
		}
	}

	// 일간 랭킹 조회
	@SuppressWarnings("unchecked")
	public List<DailyRankingResponseDto> getDailyRankings() {
		Object cachedRankings = redisTemplate.opsForValue().get(DAILY_RANKING_KEY);
		if (cachedRankings != null) {
			try {
				return objectMapper.readValue(cachedRankings.toString(), objectMapper.getTypeFactory()
						.constructCollectionType(List.class, DailyRankingResponseDto.class));
			} catch (JsonProcessingException e) {
				log.error("일간 랭킹 캐시 읽기 실패", e);
			}
		}
		return null; // 캐시가 없는 경우
	}

	// 전체 랭킹 조회
	@SuppressWarnings("unchecked")
	public List<TotalRankingResponseDto> getTotalRankings() {
		Object cachedRankings = redisTemplate.opsForValue().get(TOTAL_RANKING_KEY);
		if (cachedRankings != null) {
			try {
				return objectMapper.readValue(cachedRankings.toString(), objectMapper.getTypeFactory()
						.constructCollectionType(List.class, TotalRankingResponseDto.class));
			} catch (JsonProcessingException e) {
				log.error("전체 랭킹 캐시 읽기 실패", e);
			}
		}
		return null; // 캐시가 없는 경우
	}

	// 사용자 개인 랭킹 저장
	public void cacheUserDailyRanking(Integer userId, DailyRankingResponseDto ranking) {
		try {
			String key = USER_DAILY_RANKING_KEY + userId;
			String rankingJson = objectMapper.writeValueAsString(ranking);
			redisTemplate.opsForValue().set(key, rankingJson, CACHE_EXPIRY, TimeUnit.MINUTES);
		} catch (JsonProcessingException e) {
			log.error("사용자 일간 랭킹 캐시 저장 실패: userId={}", userId, e);
		}
	}

	// 사용자 개인 전체 랭킹 저장
	public void cacheUserTotalRanking(Integer userId, TotalRankingResponseDto ranking) {
		try {
			String key = USER_TOTAL_RANKING_KEY + userId;
			String rankingJson = objectMapper.writeValueAsString(ranking);
			redisTemplate.opsForValue().set(key, rankingJson, CACHE_EXPIRY, TimeUnit.MINUTES);
		} catch (JsonProcessingException e) {
			log.error("사용자 전체 랭킹 캐시 저장 실패: userId={}", userId, e);
		}
	}

	// 사용자 개인 일간 랭킹 조회
	public DailyRankingResponseDto getUserDailyRanking(Integer userId) {
		String key = USER_DAILY_RANKING_KEY + userId;
		Object cachedRanking = redisTemplate.opsForValue().get(key);
		if (cachedRanking != null) {
			try {
				return objectMapper.readValue(cachedRanking.toString(), DailyRankingResponseDto.class);
			} catch (JsonProcessingException e) {
				log.error("사용자 일간 랭킹 캐시 읽기 실패: userId={}", userId, e);
			}
		}
		return null;
	}

	// 사용자 개인 전체 랭킹 조회
	public TotalRankingResponseDto getUserTotalRanking(Integer userId) {
		String key = USER_TOTAL_RANKING_KEY + userId;
		Object cachedRanking = redisTemplate.opsForValue().get(key);
		if (cachedRanking != null) {
			try {
				return objectMapper.readValue(cachedRanking.toString(), TotalRankingResponseDto.class);
			} catch (JsonProcessingException e) {
				log.error("사용자 전체 랭킹 캐시 읽기 실패: userId={}", userId, e);
			}
		}
		return null;
	}

	// 캐시 만료 설정
	public void setExpiry(String key, long timeout, TimeUnit unit) {
		redisTemplate.expire(key, timeout, unit);
	}

	// 캐시 삭제
	public void deleteCache(String key) {
		redisTemplate.delete(key);
	}
}

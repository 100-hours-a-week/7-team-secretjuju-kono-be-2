package org.secretjuju.kono.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class CorsConfig {

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		log.info("CORS 설정을 초기화합니다...");

		CorsConfiguration configuration = new CorsConfiguration();

		// 허용할 오리진 설정 (프론트엔드 URL)
		configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000", "http://localhost:5173", // Vite
																												// 기본 개발
																												// 서버 포트
				"http://127.0.0.1:5173", // 로컬 IP도 허용
				"https://playkono.com" // 프로덕션 도메인 (필요시 수정)
		));
		log.info("허용된 오리진 패턴: {}", configuration.getAllowedOriginPatterns());

		// HTTP 메서드 설정
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		log.info("허용된 HTTP 메서드: {}", configuration.getAllowedMethods());

		// HTTP 헤더 설정
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept",
				"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
		log.info("허용된 헤더: {}", configuration.getAllowedHeaders());

		// 응답에 노출할 헤더
		configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
		log.info("노출된 헤더: {}", configuration.getExposedHeaders());

		// 인증 정보(쿠키) 허용
		configuration.setAllowCredentials(true);
		log.info("인증 정보 허용: {}", configuration.getAllowCredentials());

		// pre-flight 요청 캐싱 시간
		configuration.setMaxAge(3600L); // 1시간 동안 pre-flight 요청 캐싱
		log.info("pre-flight 요청 캐싱 시간: {}", configuration.getMaxAge());

		// 모든 경로에 CORS 설정 적용
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		log.info("CORS 설정이 완료되었습니다. 모든 경로('/**')에 적용됩니다.");

		return source;
	}
}
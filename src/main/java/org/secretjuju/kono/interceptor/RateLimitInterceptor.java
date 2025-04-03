package org.secretjuju.kono.interceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.secretjuju.kono.config.RateLimitPolicy;
import org.secretjuju.kono.service.RateLimitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

	private final RateLimitService rateLimitService;
	private final ObjectMapper objectMapper;
	private final AntPathMatcher antPathMatcher = new AntPathMatcher();

	public RateLimitInterceptor(RateLimitService rateLimitService, ObjectMapper objectMapper) {
		this.rateLimitService = rateLimitService;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String path = request.getRequestURI();

		// OAuth 로그인 경로가 아닌데 세션이 없으면 401 반환
		if (!path.startsWith("/oauth2/")) {
			HttpSession session = request.getSession(false);
			if (session == null || session.getAttribute("SPRING_SECURITY_CONTEXT") == null) {
				handleUnauthorized(response);
				return false;
			}
		}

		RateLimitPolicy policy = findMatchingPolicy(path);
		HttpSession session = request.getSession(false);
		String key = rateLimitService.generateKey(request, session);

		if (!rateLimitService.tryConsume(key, policy)) {
			handleRateLimitExceeded(response, policy);
			return false;
		}

		return true;
	}

	private RateLimitPolicy findMatchingPolicy(String path) {
		return Arrays.stream(RateLimitPolicy.values())
				.filter(policy -> antPathMatcher.match(policy.getPathPattern(), path)).findFirst()
				.orElse(RateLimitPolicy.DEFAULT);
	}

	private void handleUnauthorized(HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("error", "Unauthorized");
		errorResponse.put("message", "로그인이 필요한 서비스입니다.");

		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}

	private void handleRateLimitExceeded(HttpServletResponse response, RateLimitPolicy policy) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("error", "Rate limit exceeded");
		errorResponse.put("message", String.format("많은 요청을 시도했습니다. %d분 뒤에 다시 시도해주세요", policy.getMinutes()));
		errorResponse.put("path", policy.getPathPattern());
		errorResponse.put("limit", policy.getLimit());

		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}
}
package org.secretjuju.kono.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	@Value("${spring.profiles.active:default}")
	private String activeProfile;

	// 프론트엔드 URL 직접 지정
	private static final String DEV_FRONTEND_URL = "http://localhost:5173";
	private static final String PROD_FRONTEND_URL = "https://playkono.com";

	private final Environment environment;

	public OAuth2SuccessHandler(Environment environment) {
		this.environment = environment;
		// 기본 URL 설정은 아래의 determineFrontendUrl()에서 동적으로 수행
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		log.info("OAuth2 로그인 성공: 사용자={}, 세션ID={}", authentication.getName(), request.getSession().getId());

		// 보안 헤더 설정
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "0");

		// 세션에 사용자 정보 저장
		HttpSession session = request.getSession();
		session.setAttribute("USER_ID", authentication.getName());
		session.setAttribute("LOGIN_TIME", System.currentTimeMillis());

		log.debug("세션 정보: ID={}, 생성시간={}, 마지막 접근시간={}", session.getId(), session.getCreationTime(),
				session.getLastAccessedTime());

		// 세션 쿠키가 설정되었는지 확인
		boolean hasSessionCookie = false;
		if (request.getCookies() != null) {
			for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
				if ("JSESSIONID".equals(cookie.getName())) {
					hasSessionCookie = true;
					log.info("세션 쿠키 확인됨: name={}, value={}, domain={}, path={}", cookie.getName(), "[hidden]",
							cookie.getDomain(), cookie.getPath());
					break;
				}
			}
		}
		if (!hasSessionCookie) {
			log.warn("세션 쿠키가 설정되지 않았을 수 있습니다.");
		}

		// 리다이렉트 URL 결정
		String targetUrl = determineFrontendUrl() + "/favorite";
		log.info("프론트엔드 리다이렉트 URL: {}", targetUrl);

		// 부모 클래스의 메소드를 이용해 리다이렉트 처리
		clearAuthenticationAttributes(request);
		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}

	/**
	 * 환경에 따라 프론트엔드 URL 결정
	 */
	private String determineFrontendUrl() {
		// 환경 변수에서 먼저 확인
		String frontendUrl = environment.getProperty("frontend.url");
		if (frontendUrl != null && !frontendUrl.trim().isEmpty()) {
			log.info("설정에서 프론트엔드 URL 로드: {}", frontendUrl);
			return frontendUrl;
		}

		// 환경 변수가 없으면, 하드코딩된 URL 사용
		if ("prod".equals(activeProfile)) {
			frontendUrl = PROD_FRONTEND_URL;
		} else {
			frontendUrl = DEV_FRONTEND_URL;
		}

		log.info("환경({})에 따른 프론트엔드 URL 사용: {}", activeProfile, frontendUrl);
		return frontendUrl;
	}
}
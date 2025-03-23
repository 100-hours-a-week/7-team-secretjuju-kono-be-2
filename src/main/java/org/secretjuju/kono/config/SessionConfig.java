package org.secretjuju.kono.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

@Configuration
public class SessionConfig {

	@Bean
	public CookieSerializer cookieSerializer() {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("SESSIONID"); // 세션 쿠키 이름
		serializer.setCookiePath("/"); // 전체 경로에서 쿠키 사용 가능
		serializer.setCookieMaxAge(3600); // 쿠키 만료 시간(초)
		serializer.setSameSite(null); // SameSite 설정 (필요에 따라 조정)
		return serializer;
	}

	@Bean
	public HttpSessionIdResolver httpSessionIdResolver() {
		return new CookieHttpSessionIdResolver();
	}
}
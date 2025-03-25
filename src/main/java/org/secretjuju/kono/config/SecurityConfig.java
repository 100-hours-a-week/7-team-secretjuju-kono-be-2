package org.secretjuju.kono.config;

import java.util.Arrays;

import org.secretjuju.kono.service.OAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final OAuth2UserService oAuth2UserService;
	private final ClientRegistrationRepository clientRegistrationRepository;
	private final ObjectMapper objectMapper;
	private final OAuth2SuccessHandler oauth2SuccessHandler;
	private final Environment environment;
	private final CorsConfigurationSource corsConfigurationSource; // CorsConfig에서 제공하는 빈 주입

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource)) // 주입된 CORS 설정
																											// 사용

				// 1. 요청 경로별 인증 설정
				.authorizeHttpRequests(auth -> auth.requestMatchers("/", "/login", "/error", "/css/**", "/js/**")
						.permitAll().requestMatchers("/api/**").authenticated().anyRequest().permitAll())

				// 2. 인증 처리 분리: API vs 웹 페이지
				.exceptionHandling(exceptionHandling -> exceptionHandling
						// API 요청에 대한 인증 실패 처리 - 401 응답
						.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
								new RequestHeaderRequestMatcher("Accept", MediaType.APPLICATION_JSON_VALUE))
						// API 경로에 대한 인증 실패 처리 - 401 응답
						.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
								new AntPathRequestMatcher("/api/**"))
						// 일반 웹 요청에 대한 인증 실패 처리 - 로그인 페이지 리다이렉션
						.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"),
								new AntPathRequestMatcher("/**")))

				// 3. 세션 관리 간소화 및 명확화
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS) // IF_REQUIRED에서
																											// ALWAYS로
																											// 변경
						.maximumSessions(1).sessionRegistry(sessionRegistry()))

				// 4. SecurityContext 저장소 설정 (세션에 SecurityContext 저장)
				.securityContext(securityContext -> securityContext
						.securityContextRepository(new HttpSessionSecurityContextRepository())
						.requireExplicitSave(false) // 자동 저장 활성화
				)

				// 5. OAuth2 로그인 설정 - 간소화된 성공 핸들러 사용
				.oauth2Login(oauth2 -> oauth2.loginPage("/login")
						.userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
						.successHandler(oauth2SuccessHandler));

		return http.build();
	}

	@Bean
	public OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver() {
		DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
				clientRegistrationRepository, "/oauth2/authorization");

		// 개발 환경에서만 prompt=login 설정 적용
		/*
		 * if (isDevEnvironment()) { resolver.setAuthorizationRequestCustomizer(
		 * customizer -> customizer.additionalParameters(params -> params.put("prompt",
		 * "login"))); }
		 */

		return resolver;
	}

	private boolean isDevEnvironment() {
		return Arrays.asList(environment.getActiveProfiles()).contains("dev")
				|| environment.getActiveProfiles().length == 0; // 프로필이 지정되지 않은 경우 개발 환경으로 간주
	}

	@Bean
	public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
		StrictHttpFirewall firewall = new StrictHttpFirewall();
		firewall.setAllowSemicolon(true);
		return firewall;
	}

	// 세션 레지스트리 빈 추가
	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}
}

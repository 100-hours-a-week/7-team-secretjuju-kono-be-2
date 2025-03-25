package org.secretjuju.kono.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.secretjuju.kono.entity.CashBalance;
import org.secretjuju.kono.entity.User;
import org.secretjuju.kono.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

	private final UserRepository userRepository;

	public OAuth2UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);
		Map<String, Object> attributes = oAuth2User.getAttributes();

		Long kakaoId = Long.valueOf(attributes.get("id").toString());
		Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
		String nickname = (String) properties.get("nickname");
		String profileImageUrl = (String) properties.get("profile_image");

		// 사용자 존재 여부 확인 및 저장
		User user = userRepository.findByKakaoId(kakaoId).orElseGet(() -> saveUser(kakaoId, nickname, profileImageUrl));

		log.info("User logged in: {}", user.getNickname());

		// 새로운 속성 맵 생성 (기존 속성 + 우리 DB의 정보)
		Map<String, Object> enhancedAttributes = new HashMap<>(attributes);

		// 중요: 우리 DB의 사용자 정보 추가
		enhancedAttributes.put("user_id", user.getId());
		enhancedAttributes.put("internal_nickname", user.getNickname());
		enhancedAttributes.put("profile_url", user.getProfileImageUrl());

		// 현금 잔액 정보 추가
		if (user.getCashBalance() != null) {
			enhancedAttributes.put("cash_balance", user.getCashBalance().getBalance());
			enhancedAttributes.put("total_invest", user.getCashBalance().getTotalInvest());
		}

		// 확장된 속성을 가진 OAuth2User 반환
		return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), enhancedAttributes,
				"id" // 여전히 카카오 ID를 기본 이름으로 사용
		);
	}

	private User saveUser(Long kakaoId, String nickname, String profileImageUrl) {
		User user = new User();
		user.setKakaoId(kakaoId);
		user.setNickname(nickname);
		user.setProfileImageUrl(profileImageUrl);
		user.setCreatedAt(LocalDateTime.now());

		log.info("New user saved: {}", nickname);
		// 2. 사용자 엔티티 먼저 저장 (ID 생성을 위해)
		user = userRepository.save(user);

		// 3. 현금 잔액 생성 및 연결
		CashBalance cashBalance = new CashBalance();
		cashBalance.setBalance(5000000L); // 500만원
		cashBalance.setTotalInvest(0L); // 초기 투자금 0원
		cashBalance.setUser(user); // 사용자 연결

		// 4. 사용자에 현금 잔액 설정
		user.setCashBalance(cashBalance);

		// 5. 최종 저장 및 반환
		log.info("New user saved: {}, initial cash balance: {}", nickname, cashBalance.getBalance());
		return userRepository.save(user);
	}
}
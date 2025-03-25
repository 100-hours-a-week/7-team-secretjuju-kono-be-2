package org.secretjuju.kono.controller;

import org.secretjuju.kono.dto.response.UserResponseDto;
import org.secretjuju.kono.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainController {

	private static final Logger log = LoggerFactory.getLogger(MainController.class);
	private final UserService userService;

	@GetMapping("/main")
	public String main(@AuthenticationPrincipal OAuth2User oauth2User, Model model) {
		if (oauth2User != null) {
			// 카카오 ID 추출
			Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
			log.info("User with kakaoId {} accessed main page", kakaoId);

			// DB에서 사용자 정보 조회
			UserResponseDto userInfo = userService.getUserInfo(kakaoId);

			// 사용자 정보를 모델에 추가
			model.addAttribute("nickname", userInfo.getNickname());
			model.addAttribute("user", userInfo);
		}
		return "main";
	}
}
package org.secretjuju.kono.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Controller

@RequestMapping("/login")
@Slf4j

public class LoginController {

	// 로그인 페이지를 반환하는 메서드
	@GetMapping
	public String loginPage() {
		return "login";
	}
}
package org.secretjuju.kono.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/v1/auth")
public class LogoutController {

	// 로그이웃
	@PostMapping("/logout")
	public String logout(HttpServletRequest request, HttpServletResponse response) {
		// 현재 세션 삭제
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}

		// SecurityContextHolder 초기화
		SecurityContextHolder.clearContext();

		// JSESSIONID 쿠키 삭제
		jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("SESSIONID", null);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setMaxAge(0); // 즉시 만료
		response.addCookie(cookie);

		return "로그아웃 성공";
	}
}
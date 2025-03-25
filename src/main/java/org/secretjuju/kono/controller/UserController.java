package org.secretjuju.kono.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.secretjuju.kono.dto.request.NicknameUpdateRequest;
import org.secretjuju.kono.dto.request.UserRequestDto;
import org.secretjuju.kono.dto.response.ApiResponseDto;
import org.secretjuju.kono.dto.response.CoinInfoResponseDto;
import org.secretjuju.kono.dto.response.UserResponseDto;
import org.secretjuju.kono.entity.User;
import org.secretjuju.kono.exception.NicknameAlreadyExistsException;
import org.secretjuju.kono.exception.PermissionDeniedException;
import org.secretjuju.kono.exception.UnauthorizedException;
import org.secretjuju.kono.exception.UserNotFoundException;
import org.secretjuju.kono.repository.UserRepository;
import org.secretjuju.kono.service.CoinFavoriteService;
import org.secretjuju.kono.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
// @RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final CoinFavoriteService coinFavoriteService;
	private final UserRepository userRepository;

	public UserController(UserService userService, CoinFavoriteService coinFavoriteService,
			UserRepository userRepository) {
		this.userService = userService;
		this.coinFavoriteService = coinFavoriteService;
		this.userRepository = userRepository;
	}
	@CrossOrigin(origins = "http://localhost:5173")
	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
		if (oauth2User == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
		}

		// OAuth2User에서 카카오 ID 추출
		Long kakaoId = Long.valueOf(oauth2User.getName());

		// 데이터베이스에서 사용자 정보 조회
		return userRepository.findByKakaoId(kakaoId).map(user -> {
			Map<String, Object> userInfo = new HashMap<>();
			userInfo.put("id", user.getId());
			userInfo.put("nickname", user.getNickname());
			userInfo.put("profileImage", user.getProfileImageUrl());

			if (user.getCashBalance() != null) {
				userInfo.put("cashBalance", user.getCashBalance().getBalance());
				userInfo.put("totalInvest", user.getCashBalance().getTotalInvest());
			}

			return ResponseEntity.ok(userInfo);
		}).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "사용자를 찾을 수 없습니다.")));
	}

	@GetMapping("")
	public UserResponseDto getUsernickname(@AuthenticationPrincipal OAuth2User oauth2User) {
		Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
		UserRequestDto userRequestDto = new UserRequestDto(kakaoId);
		UserResponseDto userResponseDto = userService.getUserById(userRequestDto);
		return userResponseDto;
	}

	@PutMapping("/nickname")
	public ResponseEntity<ApiResponseDto<UserResponseDto>> updateNickname(
			@AuthenticationPrincipal OAuth2User oauth2User, @Valid @RequestBody NicknameUpdateRequest request) {

		if (oauth2User == null) {
			throw new UnauthorizedException("Authentication required");
		}

		Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
		log.info("닉네임 업데이트 요청: kakaoId={}, newNickname={}", kakaoId, request.getNickname());

		try {
			UserResponseDto updatedUser = userService.updateNickname(kakaoId, request);
			return ResponseEntity.ok(new ApiResponseDto<>("User information updated", updatedUser));
		} catch (NicknameAlreadyExistsException e) {
			throw e; // 글로벌 예외 핸들러로 전달
		} catch (Exception e) {
			log.error("닉네임 업데이트 중 오류 발생", e);
			throw e; // 글로벌 예외 핸들러로 전달
		}
	}

	@GetMapping("/favorites")
	public ResponseEntity<ApiResponseDto<List<CoinInfoResponseDto>>> getUserFavorites(
			@AuthenticationPrincipal OAuth2User oauth2User) {

		if (oauth2User == null) {
			throw new UnauthorizedException("Authentication required");
		}

		try {
			Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());

			// 사용자 정보 확인
			User user = userService.getUserByKakaoId(kakaoId);
			if (user == null) {
				throw new UserNotFoundException("User not found with id: " + kakaoId);
			}

			// 관심 코인 목록 조회
			List<CoinInfoResponseDto> favoriteCoins = coinFavoriteService.getFavoriteCoinsDto(user.getId());

			// 빈 목록이어도 200 OK와 함께 빈 배열 반환
			return ResponseEntity.ok(new ApiResponseDto<>("Favorite coin list retrieved", favoriteCoins));

		} catch (UserNotFoundException e) {
			throw e; // GlobalExceptionHandler로 전달
		} catch (Exception e) {
			log.error("관심 코인 조회 중 오류 발생: {}", e.getMessage(), e);
			throw e;
		}
	}

	// 관심 코인 등록
	@PostMapping("/favorites/{ticker}")
	public ResponseEntity<ApiResponseDto<Void>> addFavoriteCoin(@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable String ticker) {

		if (oauth2User == null) {
			throw new UnauthorizedException("Authentication required");
		}

		try {
			Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
			log.info("관심 코인 등록 요청: kakaoId={}, ticker={}", kakaoId, ticker);

			// 사용자 정보 확인
			User user = userService.getUserByKakaoId(kakaoId);
			if (user == null) {
				throw new UserNotFoundException("User not found with kakaoId: " + kakaoId);
			}

			coinFavoriteService.addFavoriteCoin(user.getId(), ticker);
			return ResponseEntity.ok(new ApiResponseDto<>("Favorite coin added successfully", null));

		} catch (UserNotFoundException e) {
			throw e;
			// 구체적인 exception 구현해야함
		} catch (IllegalArgumentException e) {
			log.error("관심 코인 등록 중 잘못된 인자: {}", e.getMessage());
			throw e;
		} catch (IllegalStateException e) {
			log.error("관심 코인 등록 중 중복 오류: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("관심 코인 등록 중 오류 발생", e);
			throw e;
		}
	}

	// 관심 코인 삭제
	@DeleteMapping("/favorites/{ticker}")
	public ResponseEntity<ApiResponseDto<Void>> deleteFavoriteCoin(@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable String ticker) {

		if (oauth2User == null) {
			throw new UnauthorizedException("Authentication required");
		}

		try {
			Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
			log.info("관심 코인 삭제 요청: kakaoId={}, ticker={}", kakaoId, ticker);

			// 사용자 정보 확인
			User user = userService.getUserByKakaoId(kakaoId);
			if (user == null) {
				throw new UserNotFoundException("User not found with kakaoId: " + kakaoId);
			}

			coinFavoriteService.deleteFavoriteCoin(user.getId(), ticker);
			return ResponseEntity.ok(new ApiResponseDto<>("Favorite coin removed successfully", null));

		} catch (UserNotFoundException e) {
			throw e;
		} catch (IllegalArgumentException e) {
			log.error("관심 코인 삭제 중 잘못된 인자: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("관심 코인 삭제 중 오류 발생", e);
			throw e;
		}
	}
	// 회원 탈퇴
	@DeleteMapping("/withdraw")
	public ResponseEntity<Void> withdrawUser(@AuthenticationPrincipal OAuth2User oauth2User, HttpSession session) {
		if (oauth2User == null) {
			return ResponseEntity.status(401).build();
		}

		try {
			Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
			log.info("회원탈퇴 요청: kakaoId={}", kakaoId);
			// 사용자의 존재여부 확인
			User user = userService.getUserByKakaoId(kakaoId);
			if (user == null) {
				throw new UserNotFoundException("User not found with kakaoId: " + kakaoId);
			}
			// 탈퇴 권한을 갖고있지 않을떄 (탈퇴하려는 회원이 자신인지 검토)
			if (!userService.hasWithdrawPermission(user)) {
				throw new PermissionDeniedException("Permission required");
			}

			// UserService의 withdrawUser 메서드 호출
			userService.withdrawUser(kakaoId);

			// 세션 무효화
			session.invalidate();

			// 204 No Content 응답
			return ResponseEntity.noContent().build();

		} catch (Exception e) {
			log.error("회원탈퇴 처리 중 오류 발생", e);
			return ResponseEntity.status(500).build();
		}
	}

	// 로그아웃
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpSession session) {
		try {
			// 세션 무효화
			if (session != null) {
				log.info("로그아웃 요청: 세션 ID={}", session.getId());
				session.invalidate();
			}

			return ResponseEntity.ok().build();
		} catch (Exception e) {
			log.error("로그아웃 처리 중 오류 발생", e);
			return ResponseEntity.status(500).build();
		}
	}
}
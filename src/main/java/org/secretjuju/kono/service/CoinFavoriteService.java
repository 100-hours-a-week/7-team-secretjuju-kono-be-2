package org.secretjuju.kono.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.secretjuju.kono.dto.response.CoinInfoResponseDto;
import org.secretjuju.kono.entity.CoinFavorite;
import org.secretjuju.kono.entity.CoinInfo;
import org.secretjuju.kono.entity.User;
import org.secretjuju.kono.exception.UserNotFoundException;
import org.secretjuju.kono.repository.CoinFavoriteRepository;
import org.secretjuju.kono.repository.CoinInfoRepository;
import org.secretjuju.kono.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CoinFavoriteService {
	private final UserRepository userRepository;
	private final CoinFavoriteRepository coinFavoriteRepository;
	private final CoinInfoRepository coinInfoRepository;

	public CoinFavoriteService(UserRepository userRepository, CoinFavoriteRepository coinFavoriteRepository,
			CoinInfoRepository coinInfoRepository) {
		this.userRepository = userRepository;
		this.coinFavoriteRepository = coinFavoriteRepository;
		this.coinInfoRepository = coinInfoRepository;
	}

	@Transactional(readOnly = true)
	public List<CoinInfoResponseDto> getFavoriteCoinsDto(Integer userId) {
		// 사용자 존재 확인
		boolean userExists = userRepository.existsById(userId);
		if (!userExists) {
			throw new UserNotFoundException("User not found with id: " + userId);
		}

		// 관심 코인 조회 (메서드 이름 변경됨)
		List<CoinInfo> favoriteCoins = coinFavoriteRepository.findAllCoinInfosByUserId(userId);

		// Entity를 DTO로 변환
		return favoriteCoins.stream().map(coin -> new CoinInfoResponseDto(coin.getTicker(), coin.getKrCoinName()))
				.collect(Collectors.toList());
	}
	public boolean isFavorite(Integer userId, String ticker) {
		// ticker로 코인 정보 조회
		CoinInfo coinInfo = coinInfoRepository.findByTicker(ticker)
				.orElseThrow(() -> new RuntimeException("Coin not found with ticker: " + ticker));

		// 해당 사용자가 이 코인을 관심 목록에 추가했는지 확인
		return coinFavoriteRepository.existsByUserIdAndCoinInfoId(userId, coinInfo.getId());
	}

	@Transactional(readOnly = true)
	public List<CoinInfo> findFavoriteCoinsByUserId(Integer userId) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null");
		}

		return coinFavoriteRepository.findAllCoinInfosByUserId(userId);
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void addFavoriteCoin(Integer userId, String ticker) {
		try {
			// 사용자 존재 여부 확인
			User user = userRepository.findById(userId)
					.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

			// 코인 존재 여부 확인
			CoinInfo coinInfo = coinInfoRepository.findByTicker(ticker)
					.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코인입니다."));

			// 비관적 락으로 동시 등록 방지
			Optional<CoinFavorite> existingFavorite = coinFavoriteRepository.findByUserIdAndCoinInfoIdWithLock(userId,
					coinInfo.getId());

			if (existingFavorite.isPresent()) {
				throw new IllegalStateException("이미 관심 코인으로 등록되어 있습니다.");
			}

			// 관심 코인 등록
			CoinFavorite coinFavorite = new CoinFavorite();
			coinFavorite.setUser(user);
			coinFavorite.setCoinInfo(coinInfo);

			coinFavoriteRepository.save(coinFavorite);
			log.info("관심 코인 등록 완료: userId={}, ticker={}", userId, ticker);

		} catch (Exception e) {
			log.error("관심 코인 등록 실패: userId={}, ticker={}", userId, ticker, e);
			throw e;
		}
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void deleteFavoriteCoin(Integer userId, String ticker) {
		try {
			// 사용자 존재 여부 확인
			userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

			// 코인 존재 여부 확인
			CoinInfo coinInfo = coinInfoRepository.findByTicker(ticker)
					.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코인입니다."));

			// 비관적 락으로 동시 삭제 방지
			Optional<CoinFavorite> favoriteToDelete = coinFavoriteRepository.findByUserIdAndCoinInfoIdWithLock(userId,
					coinInfo.getId());

			if (favoriteToDelete.isEmpty()) {
				throw new IllegalStateException("등록되지 않은 관심 코인입니다.");
			}

			coinFavoriteRepository.delete(favoriteToDelete.get());
			log.info("관심 코인 삭제 완료: userId={}, ticker={}", userId, ticker);

		} catch (Exception e) {
			log.error("관심 코인 삭제 실패: userId={}, ticker={}", userId, ticker, e);
			throw e;
		}
	}
}

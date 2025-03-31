package org.secretjuju.kono.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.secretjuju.kono.dto.response.CashBalanceResponseDto;
import org.secretjuju.kono.dto.response.CoinHoldingDetailResponseDto;
import org.secretjuju.kono.dto.response.CoinHoldingResponseDto;
import org.secretjuju.kono.dto.response.TransactionHistoryResponseDto;
import org.secretjuju.kono.entity.CashBalance;
import org.secretjuju.kono.entity.CoinHolding;
import org.secretjuju.kono.entity.CoinInfo;
import org.secretjuju.kono.entity.CoinTransaction;
import org.secretjuju.kono.entity.User;
import org.secretjuju.kono.exception.PermissionDeniedException;
import org.secretjuju.kono.exception.UserNotFoundException;
import org.secretjuju.kono.repository.CoinRepository;
import org.secretjuju.kono.repository.CoinTransactionRepository;
import org.secretjuju.kono.repository.UserRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

	private final CoinTransactionRepository coinTransactionRepository;
	private final CoinRepository coinInfoRepository;
	private final UserService userService;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<TransactionHistoryResponseDto> getTransactionHistory() {
		User currentUser = userService.getCurrentUser();
		List<CoinTransaction> transactions = coinTransactionRepository.findByUserOrderByCreatedAtDesc(currentUser);

		return transactions.stream().map(this::convertToTransactionsResponse).collect(Collectors.toList());
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ)
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	public CashBalanceResponseDto getCashBalance() {
		User currentUser = userService.getCurrentUser();

		// 동시성 처리를 위한 락 추가
		if (currentUser.getCashBalance() == null) {
			CashBalance cashBalance = new CashBalance();
			cashBalance.setBalance(0L);
			cashBalance.setUser(currentUser);
			currentUser.setCashBalance(cashBalance);
			userRepository.save(currentUser);
		}

		Long balance = currentUser.getCashBalance().getBalance();
		return new CashBalanceResponseDto(balance);
	}

	@Transactional(readOnly = true)
	public List<CoinHoldingResponseDto> getCoinHoldings(Long kakaoId) {
		// 사용자 조회
		User user = userService.getUserByKakaoId(kakaoId);
		if (user == null) {
			throw new UserNotFoundException("User not found with kakaoId: " + kakaoId);
		}

		// 권한 확인 (필요한 경우)
		if (!hasPermission(user)) {
			throw new PermissionDeniedException("Permission required to view coin holdings");
		}

		return user.getCoinHoldings().stream().map(this::convertToCoinHoldingResponse).collect(Collectors.toList());
	}

	private boolean hasPermission(User user) {
		// 권한 확인 로직 구현
		return true; // 기본적으로 허용
	}

	// 특정코인 보유여부와 보유하고있는 개수를 조회
	@Transactional(readOnly = true)
	public CoinHoldingDetailResponseDto getCoinHoldingDetail(String ticker) {
		User currentUser = userService.getCurrentUser();

		// 코인 보유 정보가 null인 경우 처리
		if (currentUser.getCoinHoldings() == null || currentUser.getCoinHoldings().isEmpty()) {
			// 빈 정보 반환 또는 기본값 제공
			return new CoinHoldingDetailResponseDto(false, 0);
		}

		Optional<CoinInfo> coinInfo = coinInfoRepository.findByTicker(ticker);

		if (coinInfo.isEmpty()) {
			return new CoinHoldingDetailResponseDto(false, 0);
		}

		double totalQuantity = currentUser.getCoinHoldings().stream()
				.filter(h -> h.getCoinInfo().getId().equals(coinInfo.get().getId()))
				.mapToDouble(CoinHolding::getHoldingQuantity).sum();

		if (totalQuantity == 0) {
			return new CoinHoldingDetailResponseDto(false, 0);
		}

		return new CoinHoldingDetailResponseDto(true, totalQuantity);
	}

	private TransactionHistoryResponseDto convertToTransactionsResponse(CoinTransaction transaction) {
		return new TransactionHistoryResponseDto(transaction.getId(), transaction.getUser().getId(),
				transaction.getCoinInfo().getId(), transaction.getCoinInfo().getKrCoinName(),
				transaction.getCoinInfo().getTicker(), transaction.getOrderType(), transaction.getOrderQuantity(),
				transaction.getOrderPrice(), transaction.getOrderAmount(), transaction.getCreatedAt());
	}

	private CoinHoldingResponseDto convertToCoinHoldingResponse(CoinHolding holding) {
		return new CoinHoldingResponseDto(holding.getCoinInfo().getTicker(), holding.getCoinInfo().getKrCoinName(),
				holding.getHoldingQuantity(), holding.getHoldingPrice());
	}
}

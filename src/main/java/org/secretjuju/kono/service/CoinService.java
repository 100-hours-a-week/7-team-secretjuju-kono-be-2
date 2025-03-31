package org.secretjuju.kono.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.secretjuju.kono.dto.request.CoinRequestDto;
import org.secretjuju.kono.dto.request.CoinSellBuyRequestDto;
import org.secretjuju.kono.dto.response.CoinInfoResponseDto;
import org.secretjuju.kono.dto.response.CoinResponseDto;
import org.secretjuju.kono.dto.response.TickerResponse;
import org.secretjuju.kono.entity.CashBalance;
import org.secretjuju.kono.entity.CoinHolding;
import org.secretjuju.kono.entity.CoinInfo;
import org.secretjuju.kono.entity.CoinTransaction;
import org.secretjuju.kono.entity.User;
import org.secretjuju.kono.exception.CustomException;
import org.secretjuju.kono.repository.CashBalanceRepository;
import org.secretjuju.kono.repository.CoinHoldingRepository;
import org.secretjuju.kono.repository.CoinRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoinService {

	private final CoinHoldingRepository coinHoldingRepository;
	private final CashBalanceRepository cashBalanceRepository;
	private final CoinRepository coinRepository;
	private final UserService userService;
	private final UpbitService upbitService;

	public List<CoinInfoResponseDto> getAllCoinInfo() {
		List<CoinInfo> coinInfos = coinRepository.findAll();
		return coinInfos.stream().map(this::convertToCoinInfosResponse).collect(Collectors.toList());
	}

	public CoinResponseDto getCoinByName(CoinRequestDto coinRequestDto) {
		Optional<CoinInfo> coinInfo = coinRepository.findByTicker(coinRequestDto.getTicker());
		CoinResponseDto coinResponseDto = new CoinResponseDto(coinInfo.orElse(null));
		return coinResponseDto;
	}

	// 현재가 조회 메소드
	public Double getCurrentPrice(String ticker) {
		// 이미 "KRW-"로 시작하는 경우 그대로 사용, 아니면 "KRW-" 접두사 추가
		String market = ticker.startsWith("KRW-") ? ticker : "KRW-" + ticker;
		TickerResponse tickerResponse = upbitService.getTicker(market);
		return tickerResponse.getTrade_price();
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void createCoinOrder(CoinSellBuyRequestDto coinSellBuyRequestDto) {
		// 현재 로그인한 사용자 정보를 가져옵니다.
		User currentUser = userService.getCurrentUser();

		// 해당 코인 정보를 가져옵니다.
		CoinInfo coinInfo = coinRepository.findByTicker(coinSellBuyRequestDto.getTicker())
				.orElseThrow(() -> new CustomException(404, "해당 코인을 찾을 수 없습니다."));

		try {
			// 업비트에서 현재가 조회
			Double currentPrice = getCurrentPrice(coinSellBuyRequestDto.getTicker());
			coinSellBuyRequestDto.setOrderPrice(currentPrice);

			// orderAmount 계산
			if (coinSellBuyRequestDto.getOrderAmount() == null) {
				if (coinSellBuyRequestDto.getOrderQuantity() != null && coinSellBuyRequestDto.getOrderQuantity() > 0) {
					Long calculatedAmount = Math.round(coinSellBuyRequestDto.getOrderQuantity() * currentPrice);
					coinSellBuyRequestDto.setOrderAmount(calculatedAmount);
				} else {
					coinSellBuyRequestDto.setOrderAmount(100000L);
				}
			}

			// 주문 수량 계산
			Double orderQuantity = coinSellBuyRequestDto.getOrderAmount() / currentPrice;
			orderQuantity = Math.floor(orderQuantity * 100000000) / 100000000;
			coinSellBuyRequestDto.setOrderQuantity(orderQuantity);

			if ("buy".equalsIgnoreCase(coinSellBuyRequestDto.getOrderType())) {
				processBuyOrder(currentUser, coinInfo, coinSellBuyRequestDto);
			} else if ("sell".equalsIgnoreCase(coinSellBuyRequestDto.getOrderType())) {
				processSellOrder(currentUser, coinInfo, coinSellBuyRequestDto);
			} else {
				throw new CustomException(401, "유효하지 않은 거래 타입입니다.");
			}

		} catch (Exception e) {
			log.error("주문 처리 중 오류 발생: {}", e.getMessage());
			throw new CustomException(500, "주문 처리 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ)
	private void processBuyOrder(User user, CoinInfo coinInfo, CoinSellBuyRequestDto request) {
		// 현금 잔액 락 획득
		CashBalance cashBalance = cashBalanceRepository.findByUserWithLock(user)
				.orElseThrow(() -> new CustomException(404, "현금 잔액 정보가 없습니다."));

		if (cashBalance.getBalance() < request.getOrderAmount()) {
			throw new CustomException(403, "현금 잔액이 부족합니다.");
		}

		// 코인 보유 정보 락 획득
		CoinHolding holding = coinHoldingRepository.findByUserAndCoinInfoWithLock(user, coinInfo).orElseGet(() -> {
			CoinHolding newHolding = new CoinHolding();
			newHolding.setUser(user);
			newHolding.setCoinInfo(coinInfo);
			newHolding.setHoldingQuantity(0.0);
			return newHolding;
		});

		try {
			// 4. 거래 처리
			cashBalance.setBalance(cashBalance.getBalance() - request.getOrderAmount());

			// 평균 매수가 계산
			double newQuantity = holding.getHoldingQuantity() + request.getOrderQuantity();
			double newHoldingPrice = calculateNewHoldingPrice(holding.getHoldingQuantity(), holding.getHoldingPrice(),
					request.getOrderQuantity(), request.getOrderPrice());

			holding.setHoldingQuantity(newQuantity);
			holding.setHoldingPrice(newHoldingPrice); // 평균 매수가 설정

			// 저장
			cashBalanceRepository.save(cashBalance);
			coinHoldingRepository.save(holding);

			// 거래 내역 기록
			recordTransaction(user, coinInfo, request);
		} catch (Exception e) {
			log.error("Buy order processing failed", e);
			throw new CustomException(500, "거래 처리 중 오류가 발생했습니다.");
		}
	}

	// 평균 매수가 계산 메서드
	private double calculateNewHoldingPrice(double oldQuantity, double oldPrice, double newQuantity, double newPrice) {
		if (oldQuantity <= 0) {
			return newPrice;
		}

		double totalCost = (oldQuantity * oldPrice) + (newQuantity * newPrice);
		double totalQuantity = oldQuantity + newQuantity;
		return totalCost / totalQuantity;
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ)
	private void processSellOrder(User user, CoinInfo coinInfo, CoinSellBuyRequestDto request) {
		// 코인 보유 정보 락 획득
		CoinHolding holding = coinHoldingRepository.findByUserAndCoinInfoWithLock(user, coinInfo)
				.orElseThrow(() -> new CustomException(403, "판매할 코인을 보유하고 있지 않습니다."));

		if (holding.getHoldingQuantity() < request.getOrderQuantity()) {
			throw new CustomException(403, "코인 보유량이 부족합니다.");
		}

		// 현금 잔액 락 획득
		CashBalance cashBalance = cashBalanceRepository.findByUserWithLock(user)
				.orElseThrow(() -> new CustomException(404, "현금 잔액 정보가 없습니다."));

		try {
			// 4. 거래 처리
			cashBalance.setBalance(cashBalance.getBalance() + request.getOrderAmount());

			double remainingQuantity = holding.getHoldingQuantity() - request.getOrderQuantity();

			if (remainingQuantity <= 0.00000001) {
				coinHoldingRepository.delete(holding);
			} else {
				holding.setHoldingQuantity(remainingQuantity);
				// 매도 시에는 평균 매수가가 변경되지 않음
				coinHoldingRepository.save(holding);
			}

			// 저장
			cashBalanceRepository.save(cashBalance);

			// 거래 내역 기록
			recordTransaction(user, coinInfo, request);
		} catch (Exception e) {
			log.error("Sell order processing failed", e);
			throw new CustomException(500, "거래 처리 중 오류가 발생했습니다.");
		}
	}

	private void recordTransaction(User user, CoinInfo coinInfo, CoinSellBuyRequestDto request) {
		CoinTransaction transaction = new CoinTransaction(user, coinInfo, request.getOrderType(),
				request.getOrderQuantity(), request.getOrderPrice(), request.getOrderAmount(), LocalDateTime.now());

		user.addTransaction(transaction);
	}

	private CoinInfoResponseDto convertToCoinInfosResponse(CoinInfo coinInfo) {
		return new CoinInfoResponseDto(coinInfo.getTicker(), coinInfo.getKrCoinName());
	}
}

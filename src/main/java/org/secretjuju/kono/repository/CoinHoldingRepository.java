package org.secretjuju.kono.repository;

import java.util.List;
import java.util.Optional;

import org.secretjuju.kono.entity.CoinHolding;
import org.secretjuju.kono.entity.CoinInfo;
import org.secretjuju.kono.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface CoinHoldingRepository extends JpaRepository<CoinHolding, Long> {
	List<CoinHolding> findByUser(User user);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT ch FROM CoinHolding ch WHERE ch.user = :user AND ch.coinInfo = :coinInfo")
	Optional<CoinHolding> findByUserAndCoinInfoWithLock(@Param("user") User user, @Param("coinInfo") CoinInfo coinInfo);
}

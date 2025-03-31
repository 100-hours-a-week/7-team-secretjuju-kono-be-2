package org.secretjuju.kono.repository;

import java.util.Optional;

import org.secretjuju.kono.entity.CashBalance;
import org.secretjuju.kono.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface CashBalanceRepository extends JpaRepository<CashBalance, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT cb FROM CashBalance cb WHERE cb.user = :user")
	Optional<CashBalance> findByUserWithLock(@Param("user") User user);
}

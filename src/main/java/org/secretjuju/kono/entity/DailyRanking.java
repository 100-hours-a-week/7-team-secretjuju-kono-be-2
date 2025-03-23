package org.secretjuju.kono.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "daily_ranking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRanking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "last_day_money", nullable = false, columnDefinition = "BIGINT UNSIGNED DEFAULT 10000000")
	private Long lastDayMoney = 10000000L; // 전일 총자산

	@Column(name = "day_rank", columnDefinition = "INT DEFAULT 0")
	private Integer dayRank = 0; // 일간 랭킹

	@Column(name = "profit_rate", columnDefinition = "DOUBLE DEFAULT 0.0")
	private Double profitRate = 0.0; // 수익률

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime createdAt;
}

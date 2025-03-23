package org.secretjuju.kono.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "nickname", nullable = false, length = 50, unique = true)
	private String nickname;

	@Column(name = "profile_image_url", length = 255)
	private String profileImageUrl;

	@Column(name = "kakao_id", unique = true, columnDefinition = "BIGINT UNSIGNED")
	private Long kakaoId;

	@Column(name = "role", length = 20)
	private String role;

	// 양방향 관계 설정
	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private List<CoinTransaction> transactions = new ArrayList<>();

	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private List<Badge> badges = new ArrayList<>();

	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private List<CoinHolding> coinHoldings = new ArrayList<>();

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
	private CashBalance cashBalance;

	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private List<CoinFavorite> coinFavorites = new ArrayList<>();

	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private List<DailyRanking> dailyRankings = new ArrayList<>();

	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private List<TotalRanking> totalRankings = new ArrayList<>();

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	// 양방향 관계를 위한 편의 메서드
	public void addTransaction(CoinTransaction transaction) {
		this.transactions.add(transaction);
		transaction.setUser(this);
	}

	public void addBadge(Badge badge) {
		this.badges.add(badge);
		badge.setUser(this);
	}

	public void addCoinHolding(CoinHolding coinHolding) {
		this.coinHoldings.add(coinHolding);
		coinHolding.setUser(this);
	}

	public void setCashBalance(CashBalance cashBalance) {
		this.cashBalance = cashBalance;
		cashBalance.setUser(this);
	}

	public void addCoinFavorite(CoinFavorite coinFavorite) {
		this.coinFavorites.add(coinFavorite);
		coinFavorite.setUser(this);
	}

	public void addDailyRanking(DailyRanking dailyRanking) {
		this.dailyRankings.add(dailyRanking);
		dailyRanking.setUser(this);
	}
}

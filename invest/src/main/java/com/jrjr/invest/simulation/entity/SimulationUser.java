package com.jrjr.invest.simulation.entity;

import com.jrjr.invest.simulation.dto.RedisSimulationUserDTO;
import com.jrjr.invest.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "simulation_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class SimulationUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long seq;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "simul_seq", nullable = false)
	private Simulation simulation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_seq", nullable = false)
	private User user;

	@Column(nullable = false)
	private Long seedMoney;

	private Long currentMoney;

	private Integer previousRank;

	private Integer currentRank;

	private Boolean isExited;

	@Builder
	public SimulationUser(Simulation simulation, User user, Long seedMoney, Long currentMoney, Integer previousRank,
		Integer currentRank, Boolean isExited) {
		this.simulation = simulation;
		this.user = user;
		this.seedMoney = seedMoney;
		this.currentMoney = currentMoney;
		this.previousRank = previousRank;
		this.currentRank = currentRank;
		this.isExited = isExited;
	}

	@Override
	public String toString() {
		return "SimulationUser{" +
			"simulation=" + simulation.getSeq() +
			", user=" + user.getSeq() +
			", seedMoney=" + seedMoney +
			", currentMoney=" + currentMoney +
			", previousRank=" + previousRank +
			", currentRank=" + currentRank +
			", isExited=" + isExited +
			'}';
	}

	public void update(RedisSimulationUserDTO simulationUserDTO) {
		this.currentMoney = simulationUserDTO.getCurrentMoney();
		this.currentRank = simulationUserDTO.getCurrentRank();
		this.previousRank = simulationUserDTO.getPreviousRank();
		this.isExited = simulationUserDTO.getIsExited();
	}
}

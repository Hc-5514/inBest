package com.jrjr.invest.rank.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import com.jrjr.invest.rank.dto.RedisSimulationUserRankingDTO;
import com.jrjr.invest.rank.dto.RedisStockDTO;
import com.jrjr.invest.rank.dto.RedisStockUserDTO;
import com.jrjr.invest.rank.dto.RedisUserDTO;
import com.jrjr.invest.rank.dto.TopStockDTO;
import com.jrjr.invest.simulation.dto.RedisSimulationUserDTO;
import com.jrjr.invest.simulation.repository.SimulationRepository;
import com.jrjr.invest.trading.entity.FinancialDataCompany;
import com.jrjr.invest.trading.repository.FinancialDataCompanyRepository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class SimulationRankRedisRepository {

	private final HashOperations<String, String, RedisUserDTO> userHash; // key: USER_HASH_KEY
	private final HashOperations<String, String, RedisSimulationUserDTO> simulationUserHash; // key: simulation_simulationSeq
	private final HashOperations<String, String, RedisStockDTO> stockHash; // key: STOCK_HASH_KEY
	private final HashOperations<String, String, RedisStockUserDTO> stockUserHash; // key: simulation_simulationSeq_user_userSeq
	private final ZSetOperations<String, RedisSimulationUserRankingDTO> simulationUserRankingZSet; // key: simulation_simulationSeq_sort
	private final FinancialDataCompanyRepository financialDataCompanyRepository;
	private final SimulationRepository simulationRepository;
	static final String USER_HASH_KEY = "user";

	static final String STOCK_HASH_KEY = "stock";

	@Autowired
	public SimulationRankRedisRepository(RedisTemplate<String, RedisUserDTO> userRedisTemplate,
		RedisTemplate<String, RedisSimulationUserDTO> simulationUserRedisTemplate,
		RedisTemplate<String, RedisStockDTO> stockRedisTemplate,
		RedisTemplate<String, RedisStockUserDTO> stockUserRedisTemplate,
		RedisTemplate<String, RedisSimulationUserRankingDTO> simulationUserRankingRedisTemplate,
		FinancialDataCompanyRepository financialDataCompanyRepository,
		SimulationRepository simulationRepository
	) {
		this.userHash = userRedisTemplate.opsForHash();
		this.simulationUserHash = simulationUserRedisTemplate.opsForHash();
		this.stockHash = stockRedisTemplate.opsForHash();
		this.stockUserHash = stockUserRedisTemplate.opsForHash();
		this.simulationUserRankingZSet = simulationUserRankingRedisTemplate.opsForZSet();
		this.financialDataCompanyRepository = financialDataCompanyRepository;
		this.simulationRepository = simulationRepository;
	}

	/*
		시뮬레이션의 참가자 정보 가져오기
	 */
	public RedisSimulationUserDTO getSimulationUserInfo(Long simulationSeq, String userSeq) {
		String simulationUserHashKey = "simulation_" + simulationSeq;
		return simulationUserHash.get(simulationUserHashKey, userSeq);
	}

	/*
		시뮬레이션 별 정보 가져오기
	 */
	public Map<String, RedisSimulationUserDTO> getSimulationUserInfoMap(Long simulationSeq) {
		String simulationUserHashKey = "simulation_" + simulationSeq;
		return simulationUserHash.entries(simulationUserHashKey);
	}

	/*
		참가자의 회원 정보 가져오기
	 */
	public RedisUserDTO getUserInfo(String userSeq) {
		return userHash.get(USER_HASH_KEY, userSeq);
	}

	/*
		보유 주식 정보 가져오기
	 */
	public Map<String, RedisStockUserDTO> getStockUserInfoMap(Long simulationSeq, String userSeq) {
		String stockUserHashKey = "simulation_" + simulationSeq + "_user_" + userSeq;
		return stockUserHash.entries(stockUserHashKey);
	}

	/*
		주식의 현재 시가 정보 가져오기
	 */
	public String getStockMarketPrice(Integer stockType, String stockCode) {
		String stockHashKey = stockType + "_" + stockCode;
		RedisStockDTO redisStockDto = stockHash.get(STOCK_HASH_KEY, stockHashKey);
		if (redisStockDto == null) {
			log.info("getStockMarketPrice: 해당 주식의 정보가 없음");
			return null;
		}
		log.info(redisStockDto.toString());
		return redisStockDto.getMarketPrice();
	}

	/*
		시뮬레이션 별 전체 참가자 랭킹 정보 불러오기
	 */
	public Set<RedisSimulationUserRankingDTO> getSimulationUserRankingInfoSet(Long simulationSeq, long start,
		long end) {
		String simulationUserRankingKey = "simulation_" + simulationSeq + "_sort";
		return simulationUserRankingZSet.reverseRange(simulationUserRankingKey, start, end);
	}

	/*
		시뮬레이션 별 내 랭킹 정보 불러오기
	 */
	public RedisSimulationUserRankingDTO getSimulationUserRankingInfo(Long simulationSeq, Long userSeq) {
		Set<RedisSimulationUserRankingDTO> simulationUserRankingInfo
			= this.getSimulationUserRankingInfoSet(simulationSeq, 0, -1);

		for (RedisSimulationUserRankingDTO redisSimulationUserRankingDto : simulationUserRankingInfo) {
			if (redisSimulationUserRankingDto.getUserSeq().equals(userSeq)) {
				log.info(redisSimulationUserRankingDto.toString());
				return redisSimulationUserRankingDto;
			}
		}
		log.info("해당 시뮬레이션에 없는 참가자 정보");
		return null;
	}

	/*
		시뮬레이션 별 랭킹 정보 삭제
	 */
	public void deleteSimulationUserRankingInfo(Long simulationSeq) {
		String simulationUserRankingKey = "simulation_" + simulationSeq + "_sort";
		simulationUserRankingZSet.removeRange(simulationUserRankingKey, 0, -1);
	}

	/*
		시뮬레이션 별 참가자 랭킹 정보 산정
	 */
	public void updateSimulationUserRankingInfo(Long simulationSeq) {
		log.info("===== 시뮬레이션 별 참가자 랭킹 정보 산정 시작 ====");
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// simulation_seq_sort 기존 랭킹 정보 초기화
		this.deleteSimulationUserRankingInfo(simulationSeq);
		String simulationUserRankingKey = "simulation_" + simulationSeq + "_sort";

		// 시뮬레이션의 참가자 전체 정보 가져오기
		Map<String, RedisSimulationUserDTO> simulationUserDtoMap = this.getSimulationUserInfoMap(simulationSeq);
		for (String userSeq : simulationUserDtoMap.keySet()) {
			// 참가자 정보 가져오기
			RedisSimulationUserDTO simulationUserDto = this.getSimulationUserInfo(simulationSeq, userSeq);
			log.info("참가자 정보: {}", simulationUserDto.toString());

			// (1) 게임을 떠났다면
			if (simulationUserDto.getIsExited()) {
				log.info("게임을 떠난 유저: {}", simulationUserDto.getUserSeq());
				long totalMoney = simulationUserDto.getCurrentRank() * -1L;

				RedisSimulationUserRankingDTO simulationUserRankingDto
					= RedisSimulationUserRankingDTO.builder()
					.userSeq(Long.parseLong(userSeq))
					.totalMoney(totalMoney)
					.build();

				simulationUserRankingZSet.add(simulationUserRankingKey, simulationUserRankingDto, totalMoney);
				log.info(simulationUserRankingDto.toString());
				continue;
			}

			// (2) 게임을 떠나지 않았다면
			// 1. 총 자산 계산하기
			long totalMoney = simulationUserDto.getCurrentMoney();
			// 참가자가 보유 중인 주식 정보 리스트: 탑N 주식 정보 추출 목적
			List<TopStockDTO> stockInfoList = new ArrayList<>();
			// 보유 주식 정보 가져오기
			log.info("--- 보유 중인 주식 정보 가져오기 ---");
			Map<String, RedisStockUserDTO> stockUserDtoMap = this.getStockUserInfoMap(simulationSeq, userSeq);
			for (String stockTypeCode : stockUserDtoMap.keySet()) {
				RedisStockUserDTO redisStockUserDto = stockUserDtoMap.get(stockTypeCode);
				Long amount = redisStockUserDto.getAmount(); // 주식 보유량
				String stockMarketPrice = this.getStockMarketPrice(redisStockUserDto.getType(),
					redisStockUserDto.getStockCode()); // 주식 시가 정보
				long totalStockPrice = amount * Long.parseLong(stockMarketPrice); // 보유 중인 주식 가격

				FinancialDataCompany financialDataCompanyEntity
					= financialDataCompanyRepository.findByCompanyStockTypeAndCompanyStockCode(
					String.valueOf(redisStockUserDto.getType()), redisStockUserDto.getStockCode());

				// 참가자가 보유 중인 주식 정보 추가
				TopStockDTO topStockDto = TopStockDTO.builder()
					.stockName(redisStockUserDto.getName())
					.stockMarketPrice(stockMarketPrice)
					.totalStockPrice(totalStockPrice)
					.stockImgSearchName(financialDataCompanyEntity.getImgUrl())
					.build();

				log.info("보유 주식 정보: {}", topStockDto.toString());
				stockInfoList.add(topStockDto);
				totalMoney += totalStockPrice; // 총 자산 합산
			}
			log.info("--------------------");
			log.info("총 자산: {}", totalMoney);

			// 2. 수익률 계산하기
			long seedMoney = simulationUserDto.getSeedMoney();
			double rate = (totalMoney - seedMoney) * 1.0 / seedMoney * 100;
			log.info("수익률: {}%", rate);

			// 3. 보유 중인 주식 정보 정렬하기 (기준: 보유 중인 주식 가격)
			stockInfoList.sort((o1, o2) -> o2.getTotalStockPrice().compareTo(o1.getTotalStockPrice()));

			log.info("--- 정렬된 주식 정보 ---");
			for (TopStockDTO topStockDto : stockInfoList) {
				log.info(topStockDto.toString());
			}
			log.info("--------------------");

			List<TopStockDTO> top3StockInfoList = new ArrayList<>();
			for (int i = 0; i < stockInfoList.size() && i < 3; i++) {
				top3StockInfoList.add(stockInfoList.get(i));
			}

			RedisSimulationUserRankingDTO simulationUserRankingDto
				= RedisSimulationUserRankingDTO.builder()
				.simulationSeq(simulationSeq)
				.userSeq(Long.parseLong(userSeq))
				.totalMoney(totalMoney)
				.rate(rate)
				.topNStockInfo(top3StockInfoList)
				.build();

			simulationUserRankingZSet.add(simulationUserRankingKey, simulationUserRankingDto, totalMoney);
			log.info(simulationUserRankingDto.toString());
		}

		// 랭킹 구하기
		int rank = 0;
		int index = 0;
		long previousTotalMoney = Integer.MAX_VALUE;

		// 시뮬레이션 별 전체 참가자 랭킹 정보 불러오기
		Set<RedisSimulationUserRankingDTO> simulationUserRankingSet
			= this.getSimulationUserRankingInfoSet(simulationSeq, 0, -1);

		for (RedisSimulationUserRankingDTO simulationUserRankingDto : simulationUserRankingSet) {
			log.info(simulationUserRankingDto.toString());
			index++;
			Long totalMoney = simulationUserRankingDto.getTotalMoney();

			if (totalMoney != previousTotalMoney) {
				rank = index;
			}
			previousTotalMoney = totalMoney;

			// simulation_seq_sort 의 기존 랭킹 정보 삭제
			simulationUserRankingZSet.remove(simulationUserRankingKey, simulationUserRankingDto);

			// simulation_seq_sort 에 새로운 랭킹 정보 추가
			Long userSeq = simulationUserRankingDto.getUserSeq();
			log.info("userSeq: {}", userSeq);

			RedisSimulationUserDTO simulationUserDto =
				this.getSimulationUserInfo(simulationSeq, String.valueOf(userSeq));
			log.info(simulationUserDto.toString());

			RedisUserDTO userDto = this.getUserInfo(String.valueOf(userSeq));
			log.info(userDto.toString());

			// 랭킹 정보 추가
			simulationUserRankingDto.setPreviousRank(simulationUserDto.getCurrentRank());
			simulationUserRankingDto.setCurrentRank(rank);
			simulationUserDto.setPreviousRank(simulationUserRankingDto.getPreviousRank());
			simulationUserDto.setCurrentRank(simulationUserRankingDto.getCurrentRank());

			// 회원 정보 추가 from RedisUserDTO
			simulationUserRankingDto.setUserSeq(userSeq);
			simulationUserRankingDto.setNickname(userDto.getNickname());
			simulationUserRankingDto.setProfileImgSearchName(userDto.getProfileImgSearchName());

			// 시뮬레이션 정보 추가 from RedisSimulationUserDTO
			simulationUserRankingDto.setIsExited(simulationUserDto.getIsExited());
			simulationUserRankingDto.setCurrentMoney(simulationUserDto.getCurrentMoney());

			simulationUserRankingZSet.add(simulationUserRankingKey, simulationUserRankingDto, totalMoney);
			log.info(simulationUserRankingDto.toString());

			String simulationUserHashKey = "simulation_" + simulationSeq;
			simulationUserHash.put(simulationUserHashKey, String.valueOf(userSeq), simulationUserDto);
			log.info(simulationUserDto.toString());
		}

		stopWatch.stop();
		log.info("랭킹 재산정 소요 시간: {} milliseconds", stopWatch.getTotalTimeMillis());
		log.info("===== 시뮬레이션 별 참가자 랭킹 정보 산정 완료 ====");
	}

	/*
		시뮬레이션의 평균 티어 정보 가져오기
	 */
	public Integer getSimulationAvgTierInfo(Long simulationSeq) {
		List<Long> simulationUserTier = simulationRepository.getSimulationUserTier(simulationSeq);
		if (simulationUserTier.isEmpty()) {
			log.info("{} 시뮬레이션의 정보가 없습니다.", simulationSeq);
			return null;
		}

		long totalTier = 0;
		for (Long tier : simulationUserTier) {
			totalTier += tier;
		}
		log.info("티어 점수 총합: {}", totalTier);

		long avgTier = totalTier / simulationUserTier.size();
		log.info("평균 티어 점수: {}", avgTier);

		return (int)avgTier;
	}
}

package com.jrjr.invest.rank.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.springframework.stereotype.Service;

import com.jrjr.invest.global.exception.NotFoundException;
import com.jrjr.invest.rank.dto.RedisSimulationUserRankingDTO;
import com.jrjr.invest.rank.dto.SimulationRankingDTO;
import com.jrjr.invest.rank.dto.TopIndustryDTO;
import com.jrjr.invest.rank.dto.TopStockDTO;
import com.jrjr.invest.rank.repository.SimulationRankRedisRepository;
import com.jrjr.invest.simulation.entity.Simulation;
import com.jrjr.invest.simulation.repository.SimulationRepository;
import com.jrjr.invest.trading.entity.FinancialDataCompany;
import com.jrjr.invest.trading.entity.Trading;
import com.jrjr.invest.trading.repository.FinancialDataCompanyRepository;
import com.jrjr.invest.trading.repository.TradingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationRankServiceImpl implements SimulationRankService {

	private final SimulationRankRedisRepository simulationRankRedisRepository;
	private final SimulationRepository simulationRepository;
	private final TradingRepository tradingRepository;
	private final FinancialDataCompanyRepository financialDataCompanyRepository;

	/*
		시뮬레이션 별 참가자 랭킹 정보 산정
	 */
	@Override
	public void updateSimulationUserRankingInfo(Long simulationSeq) {
		simulationRankRedisRepository.updateSimulationUserRankingInfo(simulationSeq);
	}

	/*
		시뮬레이션 별 전체 참가자 랭킹 정보 불러오기
	 */
	@Override
	public Set<RedisSimulationUserRankingDTO> getSimulationUserRankingInfo(Long simulationSeq, Long start, Long end) {
		return simulationRankRedisRepository.getSimulationUserRankingInfoSet(simulationSeq, start, end);
	}

	/*
		시뮬레이션 별 내 랭킹 정보 불러오기
	 */
	@Override
	public RedisSimulationUserRankingDTO getSimulationUserRankingInfo(Long simulationSeq, Long userSeq) {
		return simulationRankRedisRepository.getSimulationUserRankingInfo(simulationSeq, userSeq);
	}

	/*
		전체 시뮬레이션 랭킹 정보 불러오기 (종료된 시뮬레이션)
	 */
	@Override
	public List<SimulationRankingDTO> getSimulationRankingInfo() {
		List<Simulation> simulationList = simulationRepository.findByFinishedDateIsNotNullOrderByRevenuRateDesc();
		List<SimulationRankingDTO> simulationRankingList = new ArrayList<>();

		// 랭킹 구하기
		int rank = 0;
		int index = 0;
		int previousRevenuRate = Integer.MAX_VALUE;

		for (Simulation simulation : simulationList) {
			index++;
			Integer revenuRate = simulation.getRevenuRate();

			if (revenuRate != previousRevenuRate) {
				rank = index;
			}
			previousRevenuRate = revenuRate;

			SimulationRankingDTO simulationRankingDto
				= SimulationRankingDTO.builder()
				.simulationSeq(simulation.getSeq())
				.title(simulation.getTitle())
				.currentRank(rank)
				.period(simulation.getPeriod())
				.memberNum(simulation.getMemberNum())
				.revenuRate(simulation.getRevenuRate())
				.build();

			log.info(simulationRankingDto.toString());
			simulationRankingList.add(simulationRankingDto);
		}
		return simulationRankingList;
	}

	/*
		시뮬레이션 이름으로 시뮬레이션 랭킹 정보 검색하기
	 */
	@Override
	public List<SimulationRankingDTO> getSimulationRankingInfoByTitle(Long simulationSeq) {
		// simulation table 에서 simulationSeq 로 종료된 시뮬레이션이 있는지 확인
		if (!simulationRepository.existsBySeqAndFinishedDateIsNotNull(simulationSeq)) {
			throw new NotFoundException(simulationSeq + " 시뮬레이션 이름으로 검색된 시뮬레이션 정보가 없음");
		}

		List<Simulation> simulationList = simulationRepository.findByFinishedDateIsNotNullOrderByRevenuRateDesc();
		List<SimulationRankingDTO> simulationRankingList = new ArrayList<>();

		// 랭킹 구하기
		int rank = 0;
		int index = 0;
		int previousRevenuRate = Integer.MAX_VALUE;
		int findRank = 0;

		for (Simulation simulation : simulationList) {
			index++;
			Integer revenuRate = simulation.getRevenuRate();

			if (revenuRate != previousRevenuRate) {
				rank = index;
			}
			previousRevenuRate = revenuRate;

			// 검색한 시뮬레이션의 등수 찾기
			if (simulation.getSeq().equals(simulationSeq)) {
				findRank = rank;
				log.info("검색한 시뮬레이션의 랭킹: {}", rank);
			}

			SimulationRankingDTO simulationRankingDto
				= SimulationRankingDTO.builder()
				.simulationSeq(simulation.getSeq())
				.title(simulation.getTitle())
				.currentRank(rank)
				.period(simulation.getPeriod())
				.memberNum(simulation.getMemberNum())
				.revenuRate(simulation.getRevenuRate())
				.build();

			log.info(simulationRankingDto.toString());
			simulationRankingList.add(simulationRankingDto);
		}

		// +- 10 등 범위 산정
		int start = findRank > 10 ? findRank - 10 : 1;
		int end = Math.min(findRank + 10, simulationRankingList.size());
		log.info("조회 랭킹 범위: " + start + " ~ " + end);

		// 해당 범위 랭킹 정보 복사
		List<SimulationRankingDTO> resultSimulationRankingList = simulationRankingList.subList(start - 1, end);
		log.info(resultSimulationRankingList.toString());

		return resultSimulationRankingList;
	}

	/*
		시뮬레이션 별 BEST PICK 구하기
		(1) 시뮬레이션에서 가장 높은 수익을 낸 주식 3가지
		(2) 시뮬레이션에서 가장 높은 손실을 낸 주식 3가지
		(3) 시뮬레이션에서 가장 인기가 많은 산업군 3가지
	*/
	@Override
	public Map<String, List<Object>> getSimulationStockRankingInfo(Long simulationSeq) {
		log.info("========== 시뮬레이션에서 거래된 주식 정보 산정 시작 ==========");
		StringTokenizer st;
		Map<String, TradingStock> stockInfoMap = new HashMap<>(); // 거래 및 보유 주식 정보 저장
		Map<String, Long> industryInfoMap = new HashMap<>(); // 산업군 별 거래량 저장

		// tradingInfoList: 시뮬레이션의 거래 성사 주식 내역 정보
		List<Trading> tradingInfoList
			= tradingRepository.findBySimulationSeqAndConclusionType(simulationSeq, 1);

		for (Trading trading : tradingInfoList) {
			log.info("trading: {}", trading.toString());

			String stockType = trading.getStockType();
			String stockCode = trading.getStockCode();
			String stockInfoKey = stockType + "_" + stockCode;
			log.info("stockInfoKey: {}", stockInfoKey);

			long cnt = trading.getAmount(); // 거래 개수
			long totalPrice = trading.getPrice() + trading.getAmount(); // 거래 금액

			TradingStock tradingStock;
			if (stockInfoMap.containsKey(stockInfoKey)) {
				tradingStock = stockInfoMap.get(stockInfoKey);
			} else {
				tradingStock = new TradingStock(trading.getStockName(), 0, 0, 0);
			}

			// 판매
			if (trading.getTradingType() == 0) {
				tradingStock.sellCnt += cnt;
				tradingStock.totalPrice += totalPrice;
			}

			// 구매
			if (trading.getTradingType() == 1) {
				tradingStock.buyCnt += cnt;
				tradingStock.totalPrice -= totalPrice;
			}

			log.info("tradingStock: {}", tradingStock.toString());
			stockInfoMap.put(stockInfoKey, tradingStock);

			// companyIndustryInfoMap 에 산업군 별 주식 거래량을 저장
			FinancialDataCompany financialdataCompany
				= financialDataCompanyRepository.findByCompanyStockTypeAndCompanyStockCode(stockType, stockCode);

			String industryInfoKey = financialdataCompany.getCompanyIndustry();
			log.info("CompanyIndustry : {}", industryInfoKey);
			log.info("Amount: {}", cnt);

			if (industryInfoMap.containsKey(industryInfoKey)) {
				industryInfoMap.put(industryInfoKey, industryInfoMap.get(industryInfoKey) + cnt);
			} else {
				industryInfoMap.put(industryInfoKey, cnt);
			}
		}

		// stockInfoMap 내림차순 정렬
		List<String> sortedStockInfoKeyList = new ArrayList<>(stockInfoMap.keySet());
		sortedStockInfoKeyList.sort(
			(o1, o2) -> (int)(stockInfoMap.get(o2).totalPrice - stockInfoMap.get(o1).totalPrice));
		log.info("----- stockInfoMap 정렬 후 -----");
		for (String key : sortedStockInfoKeyList) {
			TradingStock tradingStock = stockInfoMap.get(key);
			log.info(tradingStock.stockName + ": " + tradingStock.totalPrice);
		}
		log.info("------------------------------");

		log.info("========== 시뮬레이션에서 거래된 주식 정보 산정 완료 ==========");

		Map<String, List<Object>> simulationStockRankingInfo = new HashMap<>();
		List<TopStockDTO> topNProfitList = new ArrayList<>();
		List<TopStockDTO> topNLossList = new ArrayList<>();

		log.info("===== topNProfitList, topNLossList 만들기 =====");
		for (int i = 0; i < sortedStockInfoKeyList.size() && i < 3; i++) {
			// topNProfitList 만들기
			st = new StringTokenizer(sortedStockInfoKeyList.get(i), "_");
			String stockType = st.nextToken();
			String stockCode = st.nextToken();

			String key = sortedStockInfoKeyList.get(i);
			Long totalCnt = stockInfoMap.get(key).buyCnt + stockInfoMap.get(key).sellCnt; // 주식 총 거래량 가져오기
			topNProfitList.add(makeTopStockDto(stockType, stockCode, totalCnt));

			// topNLossList 만들기
			st = new StringTokenizer(sortedStockInfoKeyList.get(sortedStockInfoKeyList.size() - 1 - i), "_");
			stockType = st.nextToken();
			stockCode = st.nextToken();

			key = sortedStockInfoKeyList.get(i);
			totalCnt = stockInfoMap.get(key).buyCnt + stockInfoMap.get(key).sellCnt; // 주식 총 거래량 가져오기
			topNLossList.add(makeTopStockDto(stockType, stockCode, totalCnt));
		}
		simulationStockRankingInfo.put("topNProfitList", Collections.singletonList(topNProfitList));
		simulationStockRankingInfo.put("topNLossList", Collections.singletonList(topNLossList));

		List<String> sortedIndustryInfoKeyList = new ArrayList<>(industryInfoMap.keySet());
		sortedIndustryInfoKeyList.sort((s1, s2) -> industryInfoMap.get(s2).compareTo(industryInfoMap.get(s1)));

		log.info("----- industryInfoMap 정렬 후 -----");
		for (String key : sortedIndustryInfoKeyList) {
			log.info(key + ": " + industryInfoMap.get(key));
		}
		log.info("------------------------------");

		// topNIndustryList 만들기
		List<TopIndustryDTO> topNIndustryList = new ArrayList<>();
		log.info("===== topNIndustryList 만들기 =====");
		for (int i = 0; i < sortedIndustryInfoKeyList.size() && i < 3; i++) {
			String industry = sortedIndustryInfoKeyList.get(i);
			TopIndustryDTO topIndustryDto
				= TopIndustryDTO.builder()
				.industry(industry)
				.amount(industryInfoMap.get(industry))
				.build();

			log.info(topIndustryDto.toString());
			topNIndustryList.add(topIndustryDto);
		}
		simulationStockRankingInfo.put("topNIndustryList", Collections.singletonList(topNIndustryList));

		return simulationStockRankingInfo;
	}

	private TopStockDTO makeTopStockDto(String stockType, String stockCode, Long totalCnt) {
		log.info("----- TopStockDTO 만들기 시작 -----");
		log.info("stockType: {}", stockType);
		log.info("stockCode: {}", stockCode);
		log.info("totalCnt: {}", totalCnt);

		// 주식 이름 가져오기
		FinancialDataCompany financialdataCompany
			= financialDataCompanyRepository.findByCompanyStockTypeAndCompanyStockCode(stockType, stockCode);
		log.info(financialdataCompany.toString());

		// 주식 현재 시가 가져오기
		String stockMarketPrice
			= simulationRankRedisRepository.getStockMarketPrice(Integer.parseInt(stockType), stockCode);
		log.info("stockMarketPrice: {}", stockMarketPrice);

		TopStockDTO topStockDto = TopStockDTO.builder()
			.stockName(financialdataCompany.getCompanyName())
			.stockMarketPrice(stockMarketPrice)
			.totalStockPrice(totalCnt) // 주식 총 거래량 (구매 개수 + 판매 개수)
			.stockImgSearchName(financialdataCompany.getImgUrl())
			.build();
		log.info(topStockDto.toString());

		log.info("----- TopStockDTO 만들기 성공 -----");
		return topStockDto;
	}

	/*
		시뮬레이션의 평균 티어 정보 가져오기
	 */
	@Override
	public Integer getSimulationAvgTierInfo(Long simulationSeq) {
		return simulationRankRedisRepository.getSimulationAvgTierInfo(simulationSeq);
	}

	private static class TradingStock {
		String stockName;
		long buyCnt;
		long sellCnt;
		long totalPrice;

		public TradingStock(String stockName, long buyCnt, long sellCnt, long totalPrice) {
			this.stockName = stockName;
			this.buyCnt = buyCnt;
			this.sellCnt = sellCnt;
			this.totalPrice = totalPrice;
		}
	}
}

package com.jrjr.invest.trading.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.jrjr.invest.rank.dto.RedisStockDTO;
import com.jrjr.invest.rank.dto.RedisStockUserDTO;
import com.jrjr.invest.simulation.dto.ResponseUserStockDTO;
import com.jrjr.invest.trading.constant.TradingResultType;
import com.jrjr.invest.trading.dto.ResponseTradingDTO;
import com.jrjr.invest.trading.dto.TradingDTO;
import com.jrjr.invest.trading.entity.FinancialDataCompany;
import com.jrjr.invest.trading.entity.Trading;
import com.jrjr.invest.trading.repository.FinancialDataCompanyRepository;
import com.jrjr.invest.trading.repository.TradingRepository;
import com.jrjr.invest.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class TradingService {
	private final UserRepository userRepository;
	private final TradingRepository tradingRepository;
	private final RedisTemplate<String, RedisStockUserDTO> stockUserRedisTemplate;
	private final RedisTemplate<String, RedisStockDTO> stockRedisTemplate;
	private final FinancialDataCompanyRepository financialDataCompanyRepository;

	public List<TradingDTO> findAllSuccessTrading(Long userSeq, Long simulationSeq) {
		List<Trading> tradingList = tradingRepository.findBySimulationSeqAndUserSeqAndConclusionTypeOrderByCreatedDateAsc(
			simulationSeq, userSeq,
			TradingResultType.SUCCESS);
		List<TradingDTO> tradingDTOList = new ArrayList<>();

		for (Trading trading : tradingList) {
			TradingDTO tradingDTO = trading.toTradingDto();
			tradingDTOList.add(tradingDTO);
		}
		return tradingDTOList;
	}

	public List<ResponseTradingDTO> findAllSuccessTrading(Long userSeq, Long simulationSeq, Integer pageNo,
		Integer pageSize) throws Exception {
		Pageable topCount = PageRequest.of(pageNo - 1, pageSize);
		Page<Trading> pageTradingList = tradingRepository.findBySimulationSeqAndUserSeqAndConclusionTypeOrderByCreatedDateDesc(
			simulationSeq, userSeq,
			TradingResultType.SUCCESS, topCount);

		List<Trading> tradingList = pageTradingList.getContent();
		List<ResponseTradingDTO> tradingDTOList = new ArrayList<>();
		//로고 이미지 url 저장
		for (Trading trading : tradingList) {
			ResponseTradingDTO responseTradingDTO
				= ResponseTradingDTO.builder()
				.seq(trading.getSeq())
				.conclusionType(trading.getConclusionType())
				.createdDate(trading.getCreatedDate())
				.tradingType(trading.getTradingType())
				.nickname(trading.getNickname())
				.amount(trading.getAmount())
				.simulationType(0)
				.lastModifiedDate(trading.getLastModifiedDate())
				.stockName(trading.getStockName())
				.stockType(trading.getStockType())
				.simulationSeq(trading.getSimulationSeq())
				.price(trading.getPrice())
				.userSeq(trading.getUserSeq())
				.stockCode(trading.getStockCode())
				.build();
			FinancialDataCompany company = financialDataCompanyRepository
				.findByCompanyStockTypeAndCompanyStockCode(trading.getStockType(),trading.getStockCode());

			responseTradingDTO.setLogoUrl(company.getImgUrl());

			tradingDTOList.add(responseTradingDTO);
		}
		return tradingDTOList;
	}

	public List<ResponseUserStockDTO> findAllUserStocks(Long userSeq, Long simulationSeq, Integer pageNo,
		Integer pageSize) {
		HashOperations<String, String, RedisStockUserDTO> hashOperations = stockUserRedisTemplate.opsForHash();
		String key = "simulation_" + simulationSeq + "_user_" + userSeq;

		Map<String, RedisStockUserDTO> stockMap = hashOperations.entries(key);

		PriorityQueue<RedisStockUserDTO> stockPQ = new PriorityQueue<>(new Comparator<RedisStockUserDTO>() {
			@Override
			public int compare(RedisStockUserDTO o1, RedisStockUserDTO o2) {
				if (o1.getLastModifiedDate().isBefore(o2.getLastModifiedDate())) {
					return 1;
				} else {
					return -1;
				}
			}
		});

		//정렬
		for (String stockKey : stockMap.keySet()) {
			stockPQ.add(stockMap.get(stockKey));
		}

		List<ResponseUserStockDTO> pagedStocks = new ArrayList<>();
		int currentNo = 1;
		int start = (pageNo - 1) * pageSize + 1;
		int end = (pageNo - 1) * pageSize + pageSize;

		HashOperations<String,String,RedisStockDTO> redisStockDTOHashOperations = stockRedisTemplate.opsForHash();

		while (!stockPQ.isEmpty()) {
			RedisStockUserDTO stock = stockPQ.poll();

			//이전 페이지인 경우
			if (currentNo < start) {
				currentNo++;
				continue;
			}
			//다음 페이지인 경우
			else if (currentNo > end) {
				break;
			}

			ResponseUserStockDTO userStockDTO
				= ResponseUserStockDTO.builder()
				.amount(stock.getAmount())
				.stockCode(stock.getStockCode())
				.name(stock.getName())
				.type(stock.getType())
				.lastModifiedDate(stock.getLastModifiedDate())
				.price(0L)
				.build();

			//시가 저장
			RedisStockDTO stockDTO = redisStockDTOHashOperations.get("stock",userStockDTO.getType()+"_"+userStockDTO.getStockCode());

			if(stockDTO != null){
				userStockDTO.setPrice(Long.valueOf(stockDTO.getMarketPrice()));
			}
			FinancialDataCompany financialDataCompany =
				financialDataCompanyRepository
					.findByCompanyStockTypeAndCompanyStockCode(String.valueOf(userStockDTO.getType())
						,userStockDTO.getStockCode());

			if(financialDataCompany == null){
				userStockDTO.setLogoUrl("https://jpassets.jobplanet.co.kr/assets/default_logo_share-12e4cb8f87fe87d4c2316feb4cb33f42d7f7584f2548350d6a42e47688a00bd0.png");
			}else{
				userStockDTO.setLogoUrl(financialDataCompany.getImgUrl());
			}

			pagedStocks.add(userStockDTO);
			currentNo++;
		}

		return pagedStocks;
	}
	public ResponseUserStockDTO findUserStock(Long userSeq, Long simulationSeq, String stockCode,Integer stockType) throws
		Exception {
		HashOperations<String, String, RedisStockUserDTO> hashOperations = stockUserRedisTemplate.opsForHash();
		String key = "simulation_" + simulationSeq + "_user_" + userSeq;
		String hashKey = stockType+"_"+stockCode;
		RedisStockUserDTO stock = hashOperations.get(key,hashKey);

		HashOperations<String,String,RedisStockDTO> redisStockDTOHashOperations = stockRedisTemplate.opsForHash();

		if(stock == null){
			throw new Exception("보유하지 않는 주식입니다.");
		}

		ResponseUserStockDTO userStockDTO
			= ResponseUserStockDTO.builder()
			.amount(stock.getAmount())
			.stockCode(stock.getStockCode())
			.name(stock.getName())
			.type(stock.getType())
			.lastModifiedDate(stock.getLastModifiedDate())
			.price(0L)
			.build();

			//시가 저장
			RedisStockDTO stockDTO = redisStockDTOHashOperations.get("stock",userStockDTO.getType()+"_"+userStockDTO.getStockCode());

			if(stockDTO != null){
				userStockDTO.setPrice(Long.valueOf(stockDTO.getMarketPrice()));
			}
			FinancialDataCompany financialDataCompany =
				financialDataCompanyRepository
					.findByCompanyStockTypeAndCompanyStockCode(String.valueOf(userStockDTO.getType())
						,userStockDTO.getStockCode());

			if(financialDataCompany == null){
				userStockDTO.setLogoUrl("https://jpassets.jobplanet.co.kr/assets/default_logo_share-12e4cb8f87fe87d4c2316feb4cb33f42d7f7584f2548350d6a42e47688a00bd0.png");
			}else{
				userStockDTO.setLogoUrl(financialDataCompany.getImgUrl());
			}

		return userStockDTO;
	}
}

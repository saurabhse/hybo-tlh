package com.hack17.hybo.service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.Fund;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.Recommendation;
import com.hack17.hybo.domain.TLHAdvice;
import com.hack17.hybo.repository.FundRepository;
import com.hack17.hybo.repository.ReferenceDataRepository;
import com.hack17.hybo.repository.TLHAdvisorRepository;
import com.hack17.poc.service.strategy.TLHStrategy;
import com.hack17.poc.service.strategy.TLHThresholdBasedStrategy;

@Service
public class TLHAdvisorService {
	
	private Map<String, TLHStrategy> tlhStrategyMap;
	
	@Autowired
	private ReferenceDataRepository refDataRepo;
	
	@Autowired
	private TLHAdvisorRepository tlhAdvisorRepo;
	
	@Autowired
	private FundRepository fundRepo;
	
	public TLHAdvice advise(Portfolio portfolio, Date date){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		//Date financialYearFromDate = getFinancialYearDate("from", cal);
		TLHAdvice tlhAdvice = new TLHAdvice();
		TLHStrategy tlhStrategy=tlhStrategyMap.get("threshold");
		List<Recommendation> recommendations = tlhStrategy.execute(portfolio, date);
		
		tlhAdvice.setRecommendations(recommendations);
		tlhAdvice.setPortfolio(portfolio);
		tlhAdvice.setAdvisedOnDate(date);
		return tlhAdvice;
	}
	

	@PostConstruct
	private void init(){
		tlhStrategyMap = new HashMap<>();
		tlhStrategyMap.put("threshold", new TLHThresholdBasedStrategy(3000d, refDataRepo, tlhAdvisorRepo));
		
		
	}
	
	public void execute(TLHAdvice tlhAdvice, Portfolio portfolio){
		Date adviceDate = tlhAdvice.getAdvisedOnDate();
		tlhAdvice.getRecommendations().forEach(recommendation->{
			Optional<Allocation> optionAllocation = portfolio.getAllocations().stream().filter(alloc->alloc.getFund().getTicker().equals(recommendation.getTicker1())).findFirst();
			if(optionAllocation.isPresent() && optionAllocation.get().getQuantity() >= recommendation.getQuantity()){
				double currPrice = refDataRepo.getPriceOnDate(optionAllocation.get().getFund().getTicker(), adviceDate);
				double soldFor = recommendation.getQuantity()*currPrice;
				optionAllocation.get().setQuantity(optionAllocation.get().getQuantity()-recommendation.getQuantity());
				Fund allocatedFund = fundRepo.findFund(recommendation.getTicker2());
				double currPriceAllocatedFund = refDataRepo.getPriceOnDate(recommendation.getTicker2(), adviceDate);
				int quantityBought = new Double(soldFor/currPriceAllocatedFund).intValue();
				Allocation allocation = new Allocation(allocatedFund,currPriceAllocatedFund,quantityBought,50d, adviceDate, .04,0);
				portfolio.addAllocation(allocation);
			}
		});
	}
}

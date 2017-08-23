package com.hack17.hybo.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.hack17.hybo.domain.Action;
import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.CreatedBy;
import com.hack17.hybo.domain.Fund;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.Recommendation;
import com.hack17.hybo.domain.TLHAdvice;
import com.hack17.hybo.repository.FundRepository;
import com.hack17.hybo.repository.IncomeTaxSlabRepository;
import com.hack17.hybo.repository.PortfolioRepository;
import com.hack17.hybo.repository.ReferenceDataRepository;
import com.hack17.hybo.repository.TLHAdvisorRepository;
import com.hack17.hybo.repository.TransactionRepository;
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
	private DBLoggerService dbLoggerService;
	
	@Autowired
	private FundRepository fundRepo;
	
	@Autowired
	private PortfolioRepository portfolioRepo;
	
	@Autowired
	private TransactionRepository transactionRepository;
	
	@Autowired
	private IncomeTaxSlabRepository incomeTaxSlabRepo;
	
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
		tlhStrategyMap.put("threshold", new TLHThresholdBasedStrategy(100d, refDataRepo, tlhAdvisorRepo, transactionRepository, fundRepo, incomeTaxSlabRepo));
		
		
	}
	
	@Transactional
	public void execute(TLHAdvice tlhAdvice){
		Date adviceDate = tlhAdvice.getAdvisedOnDate();
		Portfolio portfolio =  tlhAdvice.getPortfolio();
		boolean portfolioCloned = false;
		Map<Allocation, Allocation> activeAllocMap = new HashMap<>();
		for(Recommendation recommendation:tlhAdvice.getRecommendations()){
			//tlhAdvice.getRecommendations().forEach(recommendation->{
			if(!portfolioCloned){
				portfolioCloned=true;
				clonePortfolio(portfolio, activeAllocMap);
			}
			Optional<Allocation> optionAllocation =portfolio.getAllocations().stream().filter(alloc->alloc.equals(recommendation.getAllocation()) && "Y".equals(alloc.getIsActive())).findFirst();
			if(optionAllocation.isPresent() && optionAllocation.get().getQuantity() >= recommendation.getQuantity()){
				double currPrice = refDataRepo.getPriceOnDate(optionAllocation.get().getFund().getTicker(), adviceDate);
				double soldFor = recommendation.getQuantity()*currPrice;
				Allocation activeAllocation = activeAllocMap.get(optionAllocation.get());
				activeAllocation.setQuantity(activeAllocation.getQuantity()-recommendation.getQuantity());
				activeAllocation.setCreatedBy(CreatedBy.TLH.toString());
				logTransaction(activeAllocation, currPrice, adviceDate, recommendation.getQuantity(), recommendation.getAction());
				Fund allocatedFund = fundRepo.findFund(recommendation.getTicker2());
				double currPriceAllocatedFund = refDataRepo.getPriceOnDate(recommendation.getTicker2(), adviceDate);
				int quantityBought = new Double(soldFor/currPriceAllocatedFund).intValue();
				Allocation allocation = new Allocation(allocatedFund,currPriceAllocatedFund,quantityBought,50d, adviceDate, .04,0, CreatedBy.TLH.toString(), adviceDate);
				portfolio.addAllocation(allocation);
				logTransaction(allocation, 0, null, quantityBought, Action.BUY);
			}
		}
		portfolioRepo.persist(portfolio);
	}
	
	private void clonePortfolio(Portfolio portfolio, Map<Allocation, Allocation> activeAllocMap) {
		List<Allocation> newAllocations = new ArrayList<>();
		for(Allocation allocation:portfolio.getAllocations()){
			if("N".equals(allocation.getIsActive()))
				continue;
			Allocation newAllocation = new Allocation(allocation.getFund(), allocation.getCostPrice(), allocation.getQuantity(), allocation.getPercentage(), allocation.getTransactionDate(), allocation.getExpenseRatio(), allocation.getInvestment(), allocation.getCreatedBy().toString());
			newAllocation.setPortfolio(allocation.getPortfolio());
			newAllocation.setBuyDate(allocation.getBuyDate());
			allocation.setIsActive("N");
			activeAllocMap.put(allocation, newAllocation);
			newAllocations.add(newAllocation);
		}
		portfolio.getAllocations().addAll(newAllocations);
	}


	private void logTransaction(Allocation allocation, double sellPrice, Date sellDate, double quantity, Action action){
		dbLoggerService.logTransaction(allocation, sellPrice, sellDate, quantity, action, CreatedBy.TLH);
	}
}

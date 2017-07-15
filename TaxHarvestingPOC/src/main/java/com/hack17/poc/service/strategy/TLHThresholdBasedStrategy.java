package com.hack17.poc.service.strategy;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import lombok.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.repository.ReferenceDataRepository;
import com.hack17.poc.domain.Action;
import com.hack17.poc.domain.Recommendation;

@Data
public class TLHThresholdBasedStrategy implements TLHStrategy {

	private final Double thresholdDollarValue;
	
	//@Autowired
	final private ReferenceDataRepository refDataRepo;
	
	//TODO introduce a date on which to execute this strategy
	
	
	@Override
	public Recommendation execute(Allocation allocation) {
		String ticker = allocation.getFund().getTicker();
		double currPrice = refDataRepo.getLatestPrice(allocation.getFund().getTicker());
		if(isThresholdPass(allocation, currPrice) && isWashSaleRulePass(allocation)){
			String alternateTicker = refDataRepo.getCorrelatedTicker(ticker);
			return new Recommendation(ticker, alternateTicker, Action.SELL);
		}
		return new Recommendation(ticker, null, Action.HOLD);
	}


	private boolean isWashSaleRulePass(Allocation allocation) {
		Date testDate = new Date();
		long diffInMilliSec = testDate.getTime()-allocation.getTransactionDate().getTime();
		TimeUnit.DAYS.convert(diffInMilliSec, TimeUnit.MILLISECONDS);
		return TimeUnit.DAYS.convert(diffInMilliSec, TimeUnit.MILLISECONDS)>30;
	}


	private boolean isThresholdPass(Allocation allocation, double currPrice) {
		return (currPrice-allocation.getCostPrice())*allocation.getQuantity()<-(thresholdDollarValue);
	}

}

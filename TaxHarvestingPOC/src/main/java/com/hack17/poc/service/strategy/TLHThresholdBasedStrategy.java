package com.hack17.poc.service.strategy;

import static com.hack17.hybo.util.DateTimeUtil.getFinancialYearDate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hack17.hybo.domain.Action;
import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.Recommendation;
import com.hack17.hybo.domain.TLHAdvice;
import com.hack17.hybo.repository.ReferenceDataRepository;
import com.hack17.hybo.repository.TLHAdvisorRepository;
import com.hack17.hybo.util.DateTimeUtil;

@Data
public class TLHThresholdBasedStrategy implements TLHStrategy {

	private final Double thresholdDollarValue;
	
	//@Autowired
	final private ReferenceDataRepository refDataRepo;
	
	//TODO introduce a date on which to execute this strategy
	
	final private TLHAdvisorRepository tlhAdvisorRepo;
	
	@Override
	public List<Recommendation> execute(Portfolio portfolio, Date date) {
		List<Recommendation> recommendations = new ArrayList<>();
		
		if(alreadyAdvisedOn(portfolio, date)){
			return recommendations;
		}
		portfolio.getAllocations().forEach(allocation-> {
			String ticker = allocation.getFund().getTicker();
			double currPrice = refDataRepo.getPriceOnDate(allocation.getFund().getTicker(), date);
			if(currPrice!=0d && isThresholdPass(allocation, currPrice) && isWashSaleRulePass(allocation)){
				String alternateTicker = refDataRepo.getCorrelatedTicker(ticker);
				if(alternateTicker!=null){
					int quantity = calculateQuantityToSell(allocation, currPrice);
					recommendations.add(new Recommendation(ticker, alternateTicker, Action.SELL, quantity));
				}
			}
		});
		return recommendations;
	}


	private int calculateQuantityToSell(Allocation allocation, double currPrice) {
		
		return new Double(thresholdDollarValue/currPrice).intValue();
	}


	private boolean alreadyAdvisedOn(Portfolio portfolio, Date date) {
		Date fromDate = getFinancialYearDate(DateTimeUtil.FROM, date);
		Date toDate = getFinancialYearDate(DateTimeUtil.TO, date);
		List<TLHAdvice> tlhAdviceList = tlhAdvisorRepo.findTLHAdviceInDateRange(portfolio, fromDate, toDate);
		return !tlhAdviceList.isEmpty();
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

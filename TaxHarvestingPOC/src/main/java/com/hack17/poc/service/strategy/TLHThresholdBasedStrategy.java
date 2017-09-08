package com.hack17.poc.service.strategy;

import static com.hack17.hybo.util.DateTimeUtil.getFinancialYearDate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hack17.hybo.domain.Action;
import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.CapitalGainLoss;
import com.hack17.hybo.domain.CapitalGainLoss.CapitalGainLossType;
import com.hack17.hybo.domain.CreatedBy;
import com.hack17.hybo.domain.Fund;
import com.hack17.hybo.domain.IncomeTaxSlab;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.Recommendation;
import com.hack17.hybo.domain.TLHAdvice;
import com.hack17.hybo.domain.Transaction;
import com.hack17.hybo.repository.FundRepository;
import com.hack17.hybo.repository.IncomeTaxSlabRepository;
import com.hack17.hybo.repository.ReferenceDataRepository;
import com.hack17.hybo.repository.TLHAdvisorRepository;
import com.hack17.hybo.repository.TransactionRepository;
import com.hack17.hybo.service.TLHAdvisorService;
import com.hack17.hybo.util.DateTimeUtil;

@Data
public class TLHThresholdBasedStrategy implements TLHStrategy {

	private static Logger logger = LoggerFactory.getLogger(TLHThresholdBasedStrategy.class);
	private final Double thresholdDollarValue;
	
	private final Double wagesBasedUpperThreshold = 3000d;
	
	//@Autowired
	final private ReferenceDataRepository refDataRepo;
	
	final private TLHAdvisorRepository tlhAdvisorRepo;
	
	final private TransactionRepository transactionRepo;
	
	final private FundRepository fundRepository;
	
	final private IncomeTaxSlabRepository incomeTaxSlabRepo;
	
	final private TLHAdvisorService tlhAdvisorService;
	
	@Override
	public List<Recommendation> execute(Portfolio portfolio, Date date) {
		List<Recommendation> recommendations = new ArrayList<>();
		
//		if(alreadyAdvisedOn(portfolio, date)){
//			return recommendations;
//		}
		NetCapitalGainLoss netCapitalGainLoss = tlhAdvisorService.calculateNetCapitalGainLoss(portfolio, date);
		logger.info("Net capital gain loss {}", netCapitalGainLoss);
		//double upperTLHBound = calculateUpperTLHBound(portfolio, date, netCapitalGainLoss);
		double remainingTLHBenefitOnGains = netCapitalGainLoss.getLongTermCapitalGain()+netCapitalGainLoss.getShortTermCapitalGain();
		double remainingTLHBenefitOnWage = wagesBasedUpperThreshold;
		double remainingUpperBoundForTLH = remainingTLHBenefitOnGains+remainingTLHBenefitOnWage;
		logger.info("remainingTLHBenefitOnGains - {}, remainingTLHBenefitOnWage - {}, remainingTotalUpperBoundForTLH - {}",remainingTLHBenefitOnGains,remainingTLHBenefitOnWage, remainingUpperBoundForTLH);
		//if(netCapitalGainLoss.getNetCapitalLoss()!=null)
//			remainingTLHBenefitOnWage-=netCapitalGainLoss.getNetCapitalLoss();
		//loop for all possible recommendations
		for(Allocation allocation: portfolio.getAllocations()) {
			if(remainingUpperBoundForTLH <= 0){
				break;
			}
			if("N".equals(allocation.getIsActive()))
				continue;
			String ticker = allocation.getFund().getTicker();
			double currPrice = refDataRepo.getPriceOnDate(allocation.getFund().getTicker(), date);
			
			
			String alternateTicker = refDataRepo.getCorrelatedTicker(allocation.getFund().getTicker());
			if(alternateTicker == null){
				logger.info("No correlated fund found for ticker {}", allocation.getFund().getTicker());
				continue;
			}
			logger.info("correlated fund is {}", alternateTicker);
			Fund correlatedFund = fundRepository.findFund(alternateTicker);
			if(correlatedFund == null){
				logger.info("correlated fund data found for {}", alternateTicker);
				continue;
			}
			if(currPrice!=0d && isWashSaleRulePass(allocation, date, allocation.getFund(), correlatedFund) ){
				//&& isTLHConditionPass(portfolio, allocation, currPrice, upperTLHBound, date)
				int quantityToSell = 0;
				
				double availableLossFortlhBenefit = calculateLossForTLHBenefit(allocation, currPrice, remainingUpperBoundForTLH);
				logger.info("available loss for tlh benefits {}", availableLossFortlhBenefit);
				if(availableLossFortlhBenefit>0){
					remainingUpperBoundForTLH-=availableLossFortlhBenefit;
					quantityToSell = calculateQuantityToSellOnWages(allocation, currPrice, availableLossFortlhBenefit);
				}
				if(quantityToSell==0){
					logger.info("no quantity available for selling for allocation {}", allocation.getId());
					continue;
				}
				if(quantityToSell>allocation.getQuantity())
					quantityToSell= allocation.getQuantity();
				
				if(alternateTicker!=null && quantityToSell>0){
					//int quantity = calculateQuantityToSell(allocation, currPrice);
					recommendations.add(new Recommendation(allocation, alternateTicker, Action.SELL, quantityToSell));
				}
			}
		}
		
		/*for(Allocation allocation: portfolio.getAllocations()) {
			if("N".equals(allocation.getIsActive()))
				continue;
			String ticker = allocation.getFund().getTicker();
			double currPrice = refDataRepo.getPriceOnDate(allocation.getFund().getTicker(), date);
			
			if(remainingTLHBenefitOnWage<=0 && remainingTLHBenefitOnGains<=0){
				logger.info("There are no remainingTLHBenefits On Gains and Wage");
				break;
			}
			String alternateTicker = refDataRepo.getCorrelatedTicker(allocation.getFund().getTicker());
			if(alternateTicker == null){
				logger.info("No correlated fund found for ticker {}", allocation.getFund().getTicker());
				continue;
			}
			logger.info("correlated fund is {}", alternateTicker);
			Fund correlatedFund = fundRepository.findFund(alternateTicker);
			if(correlatedFund == null){
				logger.info("correlated fund data found for {}", alternateTicker);
				continue;
			}
			if(currPrice!=0d && isWashSaleRulePass(allocation, date, allocation.getFund(), correlatedFund) ){
				//&& isTLHConditionPass(portfolio, allocation, currPrice, upperTLHBound, date)
				int quantityToSell = 0;
				if(remainingTLHBenefitOnGains>0){
					double[] tlhBenefitOnGains = calculateTLHBenefitOnGains(allocation, currPrice, remainingTLHBenefitOnGains, date);
					logger.info("available tlh benefits on gains {}", tlhBenefitOnGains[0]);
					if(tlhBenefitOnGains[0]>0){
						remainingTLHBenefitOnGains-=tlhBenefitOnGains[0];
						quantityToSell+= calculateQuantityToSellOnGains(allocation, currPrice, date, tlhBenefitOnGains[1]);
					}
				}
				if(remainingTLHBenefitOnWage>0){
					double tlhBenefitOnWages = calculateTLHBenefitOnWages(allocation, currPrice, remainingTLHBenefitOnWage);
					logger.info("available tlh benefits on wages {}", tlhBenefitOnWages);
					if(tlhBenefitOnWages>0){
						remainingTLHBenefitOnWage-=tlhBenefitOnWages;
						quantityToSell+= calculateQuantityToSellOnWages(allocation, currPrice, tlhBenefitOnWages);
					}
				}
				if(quantityToSell==0){
					logger.info("no quantity available for selling for allocation {}", allocation.getId());
					continue;
				}
				if(quantityToSell>allocation.getQuantity())
					quantityToSell= allocation.getQuantity();
				
				if(alternateTicker!=null && quantityToSell>0){
					//int quantity = calculateQuantityToSell(allocation, currPrice);
					recommendations.add(new Recommendation(allocation, alternateTicker, Action.SELL, quantityToSell));
				}
				logger.info("upperTLHBound - {}, remainingTLHBenefitOnGains - {}, remainingTLHBenefitOnWage - {}",upperTLHBound,remainingTLHBenefitOnGains,remainingTLHBenefitOnWage);
			}
		}*/
		return recommendations;
	}



	private double calculateUpperTLHBound(Portfolio portfolio, Date date, NetCapitalGainLoss netCapitalGainLoss) {
		
		double annualIncome = portfolio.getInvestorProfile().getAnnualIncome();
		Double totalTaxOnShortCapitalGains = calulateTaxOnShortCapitalGains(annualIncome, netCapitalGainLoss);
		
		double taxOnCapitalGains = totalTaxOnShortCapitalGains;
		if(netCapitalGainLoss.getLongTermCapitalGain()==null){
			return taxOnCapitalGains;
		}
		double shortTermCaptialGains = netCapitalGainLoss.getShortTermCapitalGain()!=null?netCapitalGainLoss.getShortTermCapitalGain():0;
		double totalIncome = annualIncome+shortTermCaptialGains;
		IncomeTaxSlab incomeTaxSlab = incomeTaxSlabRepo.getSlabForIncomeAndProfile(null, totalIncome);
		
		if(incomeTaxSlab.getTaxRate()==10 ||incomeTaxSlab.getTaxRate()==15){
			//do nothing
		}else if(incomeTaxSlab.getTaxRate()==39.6){
			taxOnCapitalGains+=netCapitalGainLoss.getLongTermCapitalGain()*20/100;
		}else{
			taxOnCapitalGains+=netCapitalGainLoss.getLongTermCapitalGain()*15/100;
		}
		
		
		return taxOnCapitalGains;
		
	}


	private double calulateTaxOnShortCapitalGains(double annualIncome, NetCapitalGainLoss netCapitalGainLoss) {
		if(netCapitalGainLoss.getShortTermCapitalGain()==null)
			return 0;
		double totalIncome = annualIncome+(netCapitalGainLoss.getShortTermCapitalGain());
		List<IncomeTaxSlab> incomeTaxSlabs = incomeTaxSlabRepo.getSlabsForIncomeAndProfile(null, totalIncome);
		double tax = 0;
		//double remainingIncome = totalIncome;
		for(IncomeTaxSlab taxSlab:incomeTaxSlabs){
			
			if(taxSlab.getSlabEnd()!=null && taxSlab.getSlabEnd()<annualIncome)
				continue;
			
			if(tax==0 && taxSlab.getSlabEnd()==null){
				return netCapitalGainLoss.getShortTermCapitalGain()*taxSlab.getTaxRate()/100;
			}
			
			if(tax==0){
				double taxableGains = 0;
				if(taxSlab.getSlabEnd() < totalIncome){
					taxableGains = (taxSlab.getSlabEnd()-taxSlab.getSlabStart())-(annualIncome - taxSlab.getSlabStart());
				}else{
					taxableGains = netCapitalGainLoss.getShortTermCapitalGain();
				}
				tax+= taxableGains*taxSlab.getTaxRate()/100;
				//remainingIncome-=taxSlab.getSlabEnd();
				continue;
			}
			
			if(taxSlab.getSlabEnd() == null){
				tax += taxSlab.getTaxRate()*(totalIncome-taxSlab.getSlabStart())/100;
			}else if(taxSlab.getSlabEnd()<totalIncome){
				tax += taxSlab.getTaxRate()*(taxSlab.getSlabEnd()-taxSlab.getSlabStart())/100;
				//remainingIncome-=taxSlab.getSlabEnd();
			}else{
				tax += taxSlab.getTaxRate()*(totalIncome-taxSlab.getSlabStart())/100;
			}
		}
		return tax;
	}

	private int calculateQuantityToSell(Allocation allocation, double currPrice) {
		double currLoss = calculateLoss(allocation, currPrice); 
		return new Double(currLoss/currPrice).intValue();
	}


	private boolean alreadyAdvisedOn(Portfolio portfolio, Date date) {
		Date fromDate = getFinancialYearDate(DateTimeUtil.FROM, date);
		Date toDate = getFinancialYearDate(DateTimeUtil.TO, date);
		List<TLHAdvice> tlhAdviceList = tlhAdvisorRepo.findTLHAdviceInDateRange(portfolio, fromDate, toDate);
		return !tlhAdviceList.isEmpty();
	}


	private boolean isWashSaleRulePass(Allocation allocation, Date date, Fund fund, Fund correlatedFund) {
		
		Date date30DaysBack = DateTimeUtil.add(date, Calendar.DATE, -30);
		List<Transaction> fundTransactions = transactionRepo.getTransactions(fund, allocation.getPortfolio(), date30DaysBack, date, Action.BUY);
		List<Transaction> correlatedFundTransactions = transactionRepo.getTransactions(correlatedFund, allocation.getPortfolio(), date30DaysBack, date, Action.SELL);
		boolean washSalesRulePasses = fundTransactions.size()==0 && correlatedFundTransactions.size()==0;
		logger.info("wash sale run for allocation {} passes? {}", allocation.getId(), washSalesRulePasses);
		return washSalesRulePasses;
//		long diffInMilliSec = date.getTime()-allocation.getTransactionDate().getTime();
//		TimeUnit.DAYS.convert(diffInMilliSec, TimeUnit.MILLISECONDS);
//		long timeInDaysSinceBuy = TimeUnit.DAYS.convert(diffInMilliSec, TimeUnit.MILLISECONDS);
//		if(CreatedBy.TLH.equals(CreatedBy.valueOf(allocation.getCreatedBy())) && timeInDaysSinceBuy <= 30)
//			return false;
//		return true;
	}


	private boolean isTLHConditionPass(Portfolio portfolio, Allocation allocation, double currPrice, double upperTLHBound, Date date) {
		double currLoss = calculateLoss(allocation, currPrice);
		double taxBenefit = 0;
		if(isLongTermCapitalLoss(allocation, date)){
			taxBenefit=currLoss*20/100;
		}else{
			taxBenefit=currLoss*40/100;
		}
		return currLoss > thresholdDollarValue;
	}
	
	private double[] calculateTLHBenefitOnGains(Allocation allocation, double currPrice, double remainingTLHBenefitOnGains, Date date) {
		double[] tlhBenefitOnGains = new double[2];
		double currLoss = calculateLoss(allocation, currPrice);
		boolean longTerm=false;
		
		if(currLoss<thresholdDollarValue)
			return tlhBenefitOnGains;
		
		double taxBenefit = 0;
		
		if(isLongTermCapitalLoss(allocation, date)){
			taxBenefit=currLoss*20/100;
			longTerm=true;
		}else{
			taxBenefit=currLoss*40/100;
		}
		tlhBenefitOnGains[0] = taxBenefit;
		tlhBenefitOnGains[1] = currLoss;
		if(taxBenefit>remainingTLHBenefitOnGains){
			if(longTerm){
				tlhBenefitOnGains[0] = remainingTLHBenefitOnGains;
				tlhBenefitOnGains[1] = remainingTLHBenefitOnGains*100/20;
			}else{
				tlhBenefitOnGains[0] = remainingTLHBenefitOnGains;
				tlhBenefitOnGains[1] = remainingTLHBenefitOnGains*100/40;
			}
				
		}
		return tlhBenefitOnGains;
	}
	
	private double calculateTLHBenefitOnWages(Allocation allocation, double currPrice, double remainingTLHBenefitOnWages) {
		double currLoss = calculateLoss(allocation, currPrice);
		
		if(currLoss<thresholdDollarValue)
			return 0;
		
		if(currLoss>remainingTLHBenefitOnWages)
			return remainingTLHBenefitOnWages;
		return currLoss;
	}
	
	private double calculateLossForTLHBenefit(Allocation allocation, double currPrice, double remainingTotalUpperBound) {
		double currLoss = calculateLoss(allocation, currPrice);
		
		if(currLoss<thresholdDollarValue)
			return 0;
		
		if(currLoss>remainingTotalUpperBound)
			return remainingTotalUpperBound;
		return currLoss;
	}
	
	private int calculateQuantityToSellOnGains(Allocation allocation, double currPrice, Date date, double tlhBenefitOnGain) {
		
		/*if(isLongTermCapitalLoss(allocation, date)){
			tlhBenefitOnGain=tlhBenefitOnGain*100/20;
		}else{
			tlhBenefitOnGain=tlhBenefitOnGain*100/40;
		}*/
		double diffInPrices = allocation.getCostPrice()-currPrice;
		int quantityToSell = new Double(tlhBenefitOnGain/diffInPrices).intValue();
		
		return quantityToSell;
	}
	
	private int calculateQuantityToSellOnWages(Allocation allocation, double currPrice, double tlhBenefitOnWages) {
		double diffInPrices = allocation.getCostPrice()-currPrice;
		int quantityToSell = new Double(tlhBenefitOnWages/diffInPrices).intValue();
		
		return quantityToSell;
	}


	private double calculateLoss(Allocation allocation, double currPrice) {
		double currLoss = (currPrice-allocation.getCostPrice())*allocation.getQuantity();
		return -currLoss;
	}
	
	private boolean isLongTermCapitalLoss(Allocation allocation, Date date){
		return DateTimeUtil.isMoreThanYearOld(allocation.getTransactionDate(), date);
	}
	

}

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
	
	@Override
	public List<Recommendation> execute(Portfolio portfolio, Date date) {
		List<Recommendation> recommendations = new ArrayList<>();
		
//		if(alreadyAdvisedOn(portfolio, date)){
//			return recommendations;
//		}
		NetCapitalGainLoss netCapitalGainLoss = calculateNetCapitalGainLoss(portfolio, date);
		logger.info("Net capital gain loss {}", netCapitalGainLoss);
		double upperTLHBound = calculateUpperTLHBound(portfolio, date, netCapitalGainLoss);
		double remainingTLHBenefitOnGains = upperTLHBound;
		double remainingTLHBenefitOnWage = wagesBasedUpperThreshold;
		if(netCapitalGainLoss.getNetCapitalLoss()!=null)
			remainingTLHBenefitOnWage-=netCapitalGainLoss.getNetCapitalLoss();
		
		logger.info("upperTLHBound - {}, remainingTLHBenefitOnGains - {}, remainingTLHBenefitOnWage - {}",upperTLHBound,remainingTLHBenefitOnGains,remainingTLHBenefitOnWage);
		for(Allocation allocation: portfolio.getAllocations()) {
			if("N".equals(allocation.getIsActive()))
				continue;
			String ticker = allocation.getFund().getTicker();
			double currPrice = refDataRepo.getPriceOnDate(allocation.getFund().getTicker(), date);
			
			if(remainingTLHBenefitOnWage<=0 && remainingTLHBenefitOnGains<=0){
				logger.info("There are no emainingTLHBenefits On Gains and Wage");
				break;
			}
			String alternateTicker = refDataRepo.getCorrelatedTicker(allocation.getFund().getTicker());
			if(currPrice!=0d && isWashSaleRulePass(allocation, date, alternateTicker) ){
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
		}
		return recommendations;
	}


	private NetCapitalGainLoss calculateNetCapitalGainLoss(Portfolio portfolio,
			Date date) {
		NetCapitalGainLoss netCapitalGainLoss = new NetCapitalGainLoss();
		Date yearStartDate = getFinancialYearDate(DateTimeUtil.FROM, date);
		Date yearEndDate = getFinancialYearDate(DateTimeUtil.TO, date);
		List<Transaction> transactions = transactionRepo.getTransactions(portfolio, yearStartDate, yearEndDate);
		List<CapitalGainLoss> capitalGainLosses = calculateCapitalGainLoss(transactions);
		double longTermCapitalGains = 0;
		double shortTermCapitalGains = 0;
		
		for(CapitalGainLoss gainLoss:capitalGainLosses){
			if(CapitalGainLoss.CapitalGainLossType.STCG.equals(gainLoss.getGainLossType()) || CapitalGainLoss.CapitalGainLossType.STCL.equals(gainLoss.getGainLossType())){
				shortTermCapitalGains+=gainLoss.getCapitalGainLoss();
				continue;
			}
			longTermCapitalGains+=gainLoss.getCapitalGainLoss();
		}
		if(longTermCapitalGains>0 && shortTermCapitalGains>0){
			netCapitalGainLoss.setLongTermCapitalGain(longTermCapitalGains);
			netCapitalGainLoss.setShortTermCapitalGain(shortTermCapitalGains);
		}else if(longTermCapitalGains>0 && shortTermCapitalGains<=0){
			longTermCapitalGains+=shortTermCapitalGains;
			if(longTermCapitalGains>=0)
				netCapitalGainLoss.setLongTermCapitalGain(longTermCapitalGains);
			else{
				netCapitalGainLoss.setNetCapitalLoss(longTermCapitalGains);
			}
		}else if(longTermCapitalGains<0 && shortTermCapitalGains<0){
			netCapitalGainLoss.setNetCapitalLoss(shortTermCapitalGains);
		}else if(shortTermCapitalGains!=0){
			netCapitalGainLoss.setShortTermCapitalGain(shortTermCapitalGains);
		}
		return netCapitalGainLoss;
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


	private List<CapitalGainLoss> calculateCapitalGainLoss(List<Transaction> transactions) {
		List<CapitalGainLoss> captialGainLosses = new ArrayList<>();
		transactions.stream().filter(transaction->Action.SELL.equals(transaction.getAction())).forEach(transaction->{
			Date buyDate = transaction.getBuyDate();
			Date sellDate = transaction.getSellDate();
			Double buyPrice = transaction.getBuyPrice();
			Double sellPrice = transaction.getSellPrice();
			Double quantity = transaction.getQuantity();
			boolean isMoreThanYearOld = DateTimeUtil.isMoreThanYearOld(buyDate, sellDate);
			Double profit = (sellPrice*quantity) - (buyPrice*quantity);
			captialGainLosses.add(new CapitalGainLoss(profit, calculateCapitalGainLossType(isMoreThanYearOld, profit)));
		});
		return captialGainLosses;
	}


	private CapitalGainLossType calculateCapitalGainLossType(
			boolean isMoreThanYearOld, Double profit) {
		if(isMoreThanYearOld)
			return profit >=0? CapitalGainLossType.LTCG: CapitalGainLossType.LTCL;
		else
			return profit >=0? CapitalGainLossType.STCG: CapitalGainLossType.STCL;
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


	private boolean isWashSaleRulePass(Allocation allocation, Date date, String alternateTicker) {
		Fund correlatedFund = fundRepository.findFund(alternateTicker);
		Date date30DaysBack = DateTimeUtil.add(date, Calendar.DATE, -30);
		List<Transaction> transactions = transactionRepo.getTransactions(correlatedFund, allocation.getPortfolio(), date30DaysBack, date);
		logger.info("wash sale run for allocation {} {}", allocation.getId(), transactions.size()==0?"passes":"fails");
		return transactions.size()==0;
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

package com.hack17.poc.util;

import static com.hack17.hybo.util.DateTimeUtil.format2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;





import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;



import org.springframework.data.convert.JodaTimeConverters.DateTimeToDateConverter;
import org.springframework.stereotype.Component;

import com.hack17.hybo.domain.Action;
import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.CreatedBy;
import com.hack17.hybo.domain.IncomeTaxSlab;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.PortfolioTaxAlphaHistory;
import com.hack17.hybo.domain.TLHRunAllocationHistory;
import com.hack17.hybo.domain.TLHRunPortfolioHistory;
import com.hack17.hybo.domain.Transaction;
import com.hack17.hybo.repository.IncomeTaxSlabRepository;
import com.hack17.hybo.repository.PortfolioRepository;
import com.hack17.hybo.repository.ReferenceDataRepository;
import com.hack17.hybo.repository.TransactionRepository;
import com.hack17.hybo.service.TLHAdvisorService;
import com.hack17.hybo.util.DateTimeUtil;
import com.hack17.poc.service.strategy.NetCapitalGainLoss;

@Component
public class ReportUtil implements BeanFactoryAware{
	public final static Logger logger = LoggerFactory.getLogger(ReportUtil.class);
	
	private static BeanFactory context;
	
	public static String report(Portfolio portfolio, Date date, TLHRunPortfolioHistory tlhRunHistory){
		List<TLHRunAllocationHistory> tlhAllocationHist = new ArrayList<>();
		StringBuilder strBld = new StringBuilder();
		strBld.append(String.format("\n\n\nPortfolio id - %d",portfolio.getId()));
		
		char [] dash = new char[50];
		Arrays.fill(dash, '-');
		Map<Allocation, Double[]> currValueMap = calculateCurrentValues(
				portfolio, date);
		strBld.append(String.format("\n%-70s\n", new String(dash)));
		strBld.append(String.format("|%-5s|%10s|%10s|%10s|%10s|", "Ticker", "Cost Price", "Quantity", "Buy Date","Current Price"));
		for(int index=0; index<portfolio.getAllocations().size();index++){
			Allocation alloc = portfolio.getAllocations().get(index);
			if("N".equals(alloc.getIsActive())){
				continue;
			}
			TLHRunAllocationHistory tlhAllocationHistRec = new TLHRunAllocationHistory(alloc);
			tlhAllocationHistRec.setCurrentPrice(currValueMap.get(alloc)[0]);
			tlhAllocationHist.add(tlhAllocationHistRec);
			strBld.append(String.format("\n%-70s\n", new String(dash)));
			strBld.append(format(alloc, currValueMap.get(alloc)[0]));
		}
		tlhRunHistory.setAllocations(tlhAllocationHist);
		double portfolioValue = getCurrentTotalValue(currValueMap);
		tlhRunHistory.setPortfolioId(portfolio.getId());
		tlhRunHistory.setPortfolioValue(portfolioValue);
		tlhRunHistory.setRunDate(date);
		Date fromDate = DateTimeUtil.getFinancialYearDate(DateTimeUtil.FROM, date);
		double tlh = getTLHForDates(portfolio, fromDate, date);
		double totalIncome = getTotalIncome(portfolio, date);
		double taxRate = getApplicableTax(totalIncome);
		tlhRunHistory.setTlhValue(tlh*taxRate/100);
		List<PortfolioTaxAlphaHistory> portfolioTaxAlphaHistoryList =  getPortfolioRepository().getPortfolioTaxAlphaHistory(portfolio, fromDate);
		if(portfolioTaxAlphaHistoryList.size()==1){
			tlhRunHistory.setTaxAlpha((tlh*taxRate/100)/portfolioTaxAlphaHistoryList.get(0).getPortfolioValue());
		}
		strBld.append(String.format("\n\nValue on %s - %s",format2(date),portfolioValue));
//		strBld.append(String.format("\nTax Alpha - %s", "not available"));
		return strBld.toString();
	}

	private static double getApplicableTax(double totalIncome) {
		List<IncomeTaxSlab> incomeTaxSlabs = getIncomeTaxSlabRepository().getSlabsForIncomeAndProfile(null, totalIncome);
		return incomeTaxSlabs.get(incomeTaxSlabs.size()-1).getTaxRate();
	}

	private static double getTotalIncome(Portfolio portfolio, Date date) {
		NetCapitalGainLoss netCapitalGL = getTLHAdvisorService().calculateNetCapitalGainLoss(portfolio, date);
		double personIncome = portfolio.getInvestorProfile().getAnnualIncome();
		if(netCapitalGL.getShortTermCapitalGain()<=0)
			return personIncome;
		if(netCapitalGL.getLongTermCapitalGain()>0)
			return personIncome+netCapitalGL.getShortTermCapitalGain();
		return personIncome+netCapitalGL.getShortTermCapitalGain()+netCapitalGL.getLongTermCapitalGain();
	}

	private static double getCurrentTotalValue(
			Map<Allocation, Double[]> currValueMap) {
		return currValueMap.values().stream().mapToDouble(d-> d[1]).sum();
	}

	private static Map<Allocation, Double[]> calculateCurrentValues(
			Portfolio portfolio, Date date) {
		ReferenceDataRepository refDataRepo = getRefDataRepository();
		Map<Allocation, Double[]> currValueMap = new HashMap<>();
		portfolio.getAllocations().stream().filter(alloc-> "Y".equals(alloc.getIsActive())).forEach(alloc->{
			double currVal = refDataRepo.getPriceOnDate(alloc.getFund().getTicker(), date);
			Double[] currPrices = new Double[2];
			currPrices[0]= currVal;
			currPrices[1]= currVal*alloc.getQuantity();
			currValueMap.put(alloc, currPrices);
		});
		return currValueMap;
	}

	private static ReferenceDataRepository getRefDataRepository() {
		ReferenceDataRepository refDataRepo = context.getBean(ReferenceDataRepository.class);
		return refDataRepo;
	}
	
	private static PortfolioRepository getPortfolioRepository() {
		PortfolioRepository portfolioRepo = context.getBean(PortfolioRepository.class);
		return portfolioRepo;
	}
	
	private static TransactionRepository getTransactionRepository() {
		TransactionRepository transactionRepo = context.getBean(TransactionRepository.class);
		return transactionRepo;
	}
	
	private static IncomeTaxSlabRepository getIncomeTaxSlabRepository() {
		IncomeTaxSlabRepository incomeTaxSlabRepo = context.getBean(IncomeTaxSlabRepository.class);
		return incomeTaxSlabRepo;
	}
	
	private static TLHAdvisorService getTLHAdvisorService() {
		TLHAdvisorService tlhAdvisorService = context.getBean(TLHAdvisorService.class);
		return tlhAdvisorService;
	}
	
	public static String format(Allocation allocation, double currentPrice){
		StringBuilder strBld = new StringBuilder();
		strBld.append(String.format("|%-5s|%10s|%10s|%10s|%10s|", allocation.getFund().getTicker(), allocation.getCostPrice(), allocation.getQuantity(), format2(allocation.getBuyDate()),currentPrice));
		return strBld.toString();
	}
	
	@Override
	public void setBeanFactory(BeanFactory arg0) throws BeansException {
		context = arg0;
		
	}

	public static void createTLHHistory(Portfolio portfolio, Date currDate, Date runDate) {
		if(getPortfolioRepository().getPortfolioTaxAlphaHistory(portfolio, runDate).size()!=0){
			logger.info("tlh history already exists for portfolio {} for date {}", portfolio.getId(), runDate);
			return;
		}
		logger.info("creating year start tlh stats for portfolio id {} on date {}", portfolio.getId(), runDate);
		Map<Allocation, Double[]> currValueMap = calculateCurrentValues(
				portfolio, runDate);
		double portfolioValue = getCurrentTotalValue(currValueMap);
		PortfolioTaxAlphaHistory taxAlphaHist = new PortfolioTaxAlphaHistory();
		taxAlphaHist.setPortfolio(portfolio);
		taxAlphaHist.setPortfolioValue(portfolioValue);
		taxAlphaHist.setAsOfDate(runDate);
		if(DateTimeUtil.isMonth(currDate, 9) && DateTimeUtil.isDay(currDate, 30)){
			Date fromDate = DateTimeUtil.getFinancialYearDate(DateTimeUtil.FROM, runDate);
			Date toDate = DateTimeUtil.getFinancialYearDate(DateTimeUtil.TO, runDate);
			double tlh = getTLHForDates(portfolio, fromDate, toDate);
			Date fromDateNextWorkDay = DateTimeUtil.getNextWorkingDay(fromDate);
			double portfolioValueYearStart = getPortfolioValueYearStart(portfolio, fromDateNextWorkDay);
			taxAlphaHist.setTotalTLH(tlh);
			if(portfolioValueYearStart==-1){
				taxAlphaHist.setTaxAlpha(-1);
			}else{
				taxAlphaHist.setTaxAlpha((tlh*40/100)/portfolioValueYearStart);
				
			}
		}
		getPortfolioRepository().persist(taxAlphaHist);
	}

	private static double getTLHForDates(Portfolio portfolio, Date fromDate,
			Date toDate) {
		List<Transaction> transactions = getTransactionRepository().getTransactions(portfolio, fromDate, toDate, CreatedBy.TLH);
		double tlh = transactions.stream().filter(tran-> tran.getAction().equals(Action.SELL)).mapToDouble(tran->(tran.getBuyPrice()-tran.getSellPrice())*tran.getQuantity()).sum();
		return tlh;
	}

	private static double getPortfolioValueYearStart(Portfolio portfolio,
			Date fromDateNextWorkDay) {
		List<PortfolioTaxAlphaHistory> taxAlphaHistYearStartList = getPortfolioRepository().getPortfolioTaxAlphaHistory(portfolio, fromDateNextWorkDay);
		if(taxAlphaHistYearStartList.size()!= 1)
			return -1;
		PortfolioTaxAlphaHistory taxAlphaHistYearStart = taxAlphaHistYearStartList.get(0);
		double portfolioValueYearStart = taxAlphaHistYearStart.getPortfolioValue();
		return portfolioValueYearStart;
	}
}

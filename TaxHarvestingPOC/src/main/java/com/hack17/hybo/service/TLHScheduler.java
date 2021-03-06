package com.hack17.hybo.service;


import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hack17.hybo.domain.CurrentDate;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.TLHAdvice;
import com.hack17.hybo.domain.TLHRunPortfolioHistory;
import com.hack17.hybo.repository.PortfolioRepository;
import com.hack17.hybo.repository.ReferenceDataRepository;
import com.hack17.hybo.repository.TLHAdvisorRepository;
import com.hack17.hybo.util.DateTimeUtil;
import com.hack17.poc.util.ReportUtil;


@Component
public class TLHScheduler {
	private static Logger logger = LoggerFactory.getLogger(TLHScheduler.class);
	
	
	@Autowired
	private PortfolioRepository portfolioRepo;
	
	@Autowired
	TLHAdvisorService tlhAdvisorService;
	
	@Autowired
	private TLHAdvisorRepository tlhAdvisorRepo;
	
	@Autowired
	private ReferenceDataRepository refDataRepo;
	
	//@Scheduled(cron="0 0/1 * * * ?")
	//@Scheduled(initialDelay=0, fixedDelay=300000)
//	public void run(){
//		List<Portfolio> portfolios = portfolioRepo.getAllPortfolios();
//		portfolios.forEach(portfolio->{
//			TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio);
//			tlhAdvisorRepo.saveTLHAdvice(tlhAdvice);
//		});
//		
//	}
	//@Scheduled(initialDelay=0, fixedDelay=10000)
	public void runOnDateRange(){
		Portfolio portfolio = portfolioRepo.getAllPortfolios().get(0);
		
		Date stopDate = DateTimeUtil.getDateMMMddyyyy("Jun 01, 2013");
		Date today = DateTimeUtil.getDateMMMddyyyy("Nov 01, 2012");
		today = DateTimeUtil.add(today, Calendar.MONTH, 1);
		//logger.info(ReportUtil.report(portfolio, today));
		while(today.before(stopDate)){
			portfolio = portfolioRepo.getAllPortfolios().get(0);
			TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio,today);
			if(tlhAdvice.getRecommendations().size()!=0){
				tlhAdvisorService.execute(tlhAdvice);
				tlhAdvisorRepo.saveTLHAdvice(tlhAdvice);
				//logger.info(ReportUtil.report(portfolio, today));
			}
			today = DateTimeUtil.add(today, Calendar.MONTH, 1);
		}
		
		
		//TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio);
		//tlhAdvisorRepo.saveTLHAdvice(tlhAdvice);

		
	}
	
	@Scheduled(initialDelay=0, fixedDelay=10000)
	public void runOnDate(){
		
		List<CurrentDate> dates = refDataRepo.getAll(CurrentDate.class);
		if(dates.size()==0)
			return;
		CurrentDate currDate = dates.get(0);
		 
		if(DateTimeUtil.isMonth(currDate.getDate(), 10) && DateTimeUtil.isDay(currDate.getDate(), 1)){
			Date nextWeekDay = DateTimeUtil.getNextWorkingDay(currDate.getDate());
			for(Portfolio portfolio : portfolioRepo.getAllPortfolios()){
				ReportUtil.createTLHHistory(portfolio, currDate.getDate(), nextWeekDay);
			}
			//return;
		}
		
		if(DateTimeUtil.isMonth(currDate.getDate(), 9) && DateTimeUtil.isDay(currDate.getDate(), 30)){
			Date previousWeekDay = DateTimeUtil.getPreviousWorkingDay(currDate.getDate());
			for(Portfolio portfolio : portfolioRepo.getAllPortfolios()){
				logger.info("creating year end tlh stats for portfolio id {} on date {}", portfolio.getId(), previousWeekDay);
				ReportUtil.createTLHHistory(portfolio, currDate.getDate(), previousWeekDay);
			}
			//return;
		}
		
		if(!isDayCheckPass(currDate)){
			logger.info("tlh not configured to run on this day {}", currDate.getDate());
			return;
		}
		
		
		List<Portfolio> portfolios = portfolioRepo.getAllPortfolios();
		for(Portfolio portfolio: portfolios){
			if(tlhAdvisorRepo.findTLHAdviceOnDate(portfolio, currDate.getDate()).size()!=0){
				logger.info("TLH already ran for portfolio id {} on date {}", portfolio.getId(), currDate.getDate());
				continue;
			}
			TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio,currDate.getDate());
			tlhAdvisorService.execute(tlhAdvice);
			tlhAdvisorRepo.saveTLHAdvice(tlhAdvice);
			if(tlhAdvice.getRecommendations().size()>0){
				TLHRunPortfolioHistory tlhHistory = new TLHRunPortfolioHistory();
				logger.info(ReportUtil.report(portfolio, currDate.getDate(),tlhHistory));
				portfolioRepo.persist(tlhHistory);
			}
		}
		
	}

	private boolean isDayCheckPass(CurrentDate currDate) {
		Date today = currDate.getDate();
		if(DateTimeUtil.isWeekend(today))
				return false;
		if(isQ1End(today))
			return true;
		if(isQ2End(today))
			return true;
		if(isQ3End(today))
			return true;
		if(isQ4End(today))
			return true;
		return false;
	}

	private boolean isQ1End(Date today) {
		return DateTimeUtil.isMonth(today, 3) && DateTimeUtil.isDay(today, 31);
	}
	private boolean isQ2End(Date today) {
		return DateTimeUtil.isMonth(today, 6) && DateTimeUtil.isDay(today, 30);
	}
	private boolean isQ3End(Date today) {
		return DateTimeUtil.isMonth(today, 9) && DateTimeUtil.isDay(today, 30);
	}
	private boolean isQ4End(Date today) {
		return DateTimeUtil.isMonth(today, 12) && DateTimeUtil.isDay(today, 31);
	}
}

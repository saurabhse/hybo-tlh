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
import com.hack17.hybo.repository.PortfolioRepository;
import com.hack17.hybo.repository.ReferenceDataRepository;
import com.hack17.hybo.repository.TLHAdvisorRepository;
import com.hack17.hybo.util.DateTimeUtil;
import com.hack17.hybo.util.ReportUtil;


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
		logger.info(ReportUtil.format(portfolio, today));
		while(today.before(stopDate)){
			portfolio = portfolioRepo.getAllPortfolios().get(0);
			TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio,today);
			if(tlhAdvice.getRecommendations().size()!=0){
				tlhAdvisorService.execute(tlhAdvice);
				tlhAdvisorRepo.saveTLHAdvice(tlhAdvice);
				logger.info(ReportUtil.format(portfolio, today));
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
			Portfolio portfolio = portfolioRepo.getAllPortfolios().get(0);
			ReportUtil.createTLHHistory(portfolio, currDate.getDate());
		}
		
		if(DateTimeUtil.isMonth(currDate.getDate(), 9) && DateTimeUtil.isDay(currDate.getDate(), 30)){
			Portfolio portfolio = portfolioRepo.getAllPortfolios().get(0);
			ReportUtil.createTLHHistory(portfolio, currDate.getDate());
		}
		
		if(!DateTimeUtil.isDay(currDate.getDate(), 15))
			return;
		
		
		Portfolio portfolio = portfolioRepo.getAllPortfolios().get(0);
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio,currDate.getDate());
		if(tlhAdvice.getRecommendations().size()!=0){
			tlhAdvisorService.execute(tlhAdvice);
			tlhAdvisorRepo.saveTLHAdvice(tlhAdvice);
			logger.info(ReportUtil.format(portfolio, currDate.getDate()));
		}
		
	}
}

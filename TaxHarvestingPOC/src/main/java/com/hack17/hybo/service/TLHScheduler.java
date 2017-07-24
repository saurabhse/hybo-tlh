package com.hack17.hybo.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.TLHAdvice;
import com.hack17.hybo.repository.PortfolioRepository;
import com.hack17.hybo.repository.TLHAdvisorRepository;


@Component
public class TLHScheduler {
	
	@Autowired
	private PortfolioRepository portfolioRepo;
	
	@Autowired
	TLHAdvisorService tlhAdvisorService;
	
	@Autowired
	private TLHAdvisorRepository tlhAdvisorRepo;
	
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
	@Scheduled(initialDelay=0, fixedDelay=300000)
	public void runOnDateRange(){
		Portfolio portfolio = portfolioRepo.getAllPortfolios().get(0);
		printPortfolio(portfolio);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 2017);
		cal.set(Calendar.MONTH, 6);
		cal.set(Calendar.DAY_OF_MONTH, 3);
		Date today = new Date();
//		System.out.println(cal.getTime());
		while(cal.getTime().before(today)){
			TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, cal.getTime());
			if(tlhAdvice.getRecommendations().size()!=0)
			tlhAdvisorRepo.saveTLHAdvice(tlhAdvice);
			cal.add(Calendar.DATE, 1);
		}
		
		
		//TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio);
		//tlhAdvisorRepo.saveTLHAdvice(tlhAdvice);

		
	}

	private void printPortfolio(Portfolio portfolio) {
		System.out.printf("Portfolio is \n%s", portfolio);
	}

}

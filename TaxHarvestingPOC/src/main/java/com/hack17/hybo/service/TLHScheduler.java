package com.hack17.hybo.service;

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
	
	@Scheduled(cron="0 0/1 * * * ?")
	public void run(){
		List<Portfolio> portfolios = portfolioRepo.getAllPortfolios();
		portfolios.forEach(portfolio->{
			TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio);
			tlhAdvisorRepo.saveTLHAdvice(tlhAdvice);
		});
		
	}

}

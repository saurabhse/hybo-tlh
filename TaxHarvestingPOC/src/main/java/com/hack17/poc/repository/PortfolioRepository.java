package com.hack17.poc.repository;

import static com.hack17.poc.util.DateTimeUtil.getDate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hack17.poc.domain.Allocation;
import com.hack17.poc.domain.Fund;
import com.hack17.poc.domain.Portfolio;


@Repository
public class PortfolioRepository {
	
	
	
	@Autowired
	private ProfileRepository profileRepository;
	private Map<Long, Portfolio> portfolioData = new HashMap<>();
	public Portfolio loadPortfolio(long portfolioId) {
		
		return portfolioData.get(portfolioId);
	}
	
	@PostConstruct
	private void init(){
		Portfolio portfolio = new Portfolio();
		portfolio.addAllocation(new Allocation(new Fund("VTI"),120.4,1000,50d, getDate("APR 01, 2017"), .04));
		portfolio.addAllocation(new Allocation(new Fund("VTV"),44.01,1200,50d, getDate("APR 01, 2017"), .06));
		portfolio.setInvestorProfile(profileRepository.getProfile(201));
		portfolioData.put(101l, portfolio);
		portfolio = new Portfolio();
		portfolio.addAllocation(new Allocation(new Fund("VTI"),120.4,1000,50d, getDate("APR 01, 2017"), .04));
		portfolio.addAllocation(new Allocation(new Fund("VEA"),105.01,1200,50d, getDate("APR 01, 2017"), .06));
		portfolio.setInvestorProfile(profileRepository.getProfile(201));
		portfolioData.put(102l, portfolio);
		portfolio = new Portfolio();
		portfolio.addAllocation(new Allocation(new Fund("VTI"),120.4,1000,50d, getDate("APR 01, 2017"), .04));
		portfolio.addAllocation(new Allocation(new Fund("VEA"),105.01,1200,50d, getDate("JUL 01, 2017"), .06));
		portfolio.setInvestorProfile(profileRepository.getProfile(201));
		portfolioData.put(103l, portfolio);
	}

}

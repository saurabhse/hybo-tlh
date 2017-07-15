package com.hack17.poc.repository;

import static com.hack17.poc.util.DateTimeUtil.getDate;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Repository;

import com.hack17.poc.domain.InvestorProfile;
import com.hack17.poc.domain.RiskTolerance;
import com.hack17.poc.domain.InvestmentTimeHorizon;


@Repository
public class ProfileRepository {

	private Map<Long, InvestorProfile> profilesRepo = new HashMap<>();
	
	public InvestorProfile getProfile(long investorId) {
		return profilesRepo.get(investorId);
	}
	
	@PostConstruct
	public void init(){
		InvestorProfile investorProfile = new InvestorProfile(getDate("Apr 7, 1972"), RiskTolerance.MEDIUM, new InvestmentTimeHorizon(26, getDate("Jan 1, 2017")));
		profilesRepo.put(201l, investorProfile);
	}
	
	

}

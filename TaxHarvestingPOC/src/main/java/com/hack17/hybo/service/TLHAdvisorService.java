package com.hack17.hybo.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.Recommendation;
import com.hack17.hybo.domain.TLHAdvice;
import com.hack17.poc.service.strategy.TLHStrategy;
import com.hack17.poc.service.strategy.TLHThresholdBasedStrategy;
import com.hack17.hybo.repository.ReferenceDataRepository;

@Service
public class TLHAdvisorService {
	
	private Map<String, TLHStrategy> tlhStrategyMap;
	
	@Autowired
	private ReferenceDataRepository refDataRepo;
	
	public TLHAdvice advise(Portfolio portfolio){
		TLHAdvice tlhAdvice = new TLHAdvice();
		TLHStrategy tlhStrategy=tlhStrategyMap.get("threshold");
		List<Recommendation> recommendations = new ArrayList<Recommendation>();
		portfolio.getAllocations().forEach(allocation-> {
			recommendations.add(tlhStrategy.execute(allocation));
		});
		tlhAdvice.setRecommendations(recommendations);
		tlhAdvice.setPortfolio(portfolio);
		tlhAdvice.setAdvisedOnDate(new Date());
		return tlhAdvice;
	}
	
	@PostConstruct
	private void init(){
		tlhStrategyMap = new HashMap<>();
		tlhStrategyMap.put("threshold", new TLHThresholdBasedStrategy(5000d, refDataRepo));
		
		
	}
}

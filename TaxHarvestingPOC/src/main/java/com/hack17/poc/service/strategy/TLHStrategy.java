package com.hack17.poc.service.strategy;

import java.util.Date;
import java.util.List;

import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.Recommendation;

public interface TLHStrategy {
	
	public List<Recommendation> execute(Portfolio portfolio, Date date);

}

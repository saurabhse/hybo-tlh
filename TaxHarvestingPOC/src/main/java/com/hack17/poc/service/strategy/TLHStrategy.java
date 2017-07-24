package com.hack17.poc.service.strategy;

import java.util.Date;

import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.Recommendation;

public interface TLHStrategy {
	
	public Recommendation execute(Allocation allocation, Date date);

}

package com.hack17.poc.service.strategy;

import com.hack17.hybo.domain.Allocation;
import com.hack17.poc.domain.Recommendation;

public interface TLHStrategy {
	
	public Recommendation execute(Allocation allocation);

}

package com.hack17.poc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hack17.poc.service.strategy.TLHStrategy;
import com.hack17.poc.service.strategy.TLHThresholdBasedStrategy;

@Configuration
public class TLHStrategyConfiguration {
	
	
	/*@Bean
	public TLHThresholdBasedStrategy getTLHThresholdBasedStrategy(){
		TLHThresholdBasedStrategy tlhStrategy = new TLHThresholdBasedStrategy(3000d);
		return tlhStrategy;
	}*/
}

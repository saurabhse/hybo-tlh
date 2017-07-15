package com.hack17.hybo.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ReferenceDataService {
	
	private Map<String, Double> currentPrices = new HashMap<>();
	
	private Map<String, String> alernateCorrelatedFund = new HashMap<>();
	
	{
		currentPrices.put("VTI", 124d);
		currentPrices.put("VTV", 96d);
		currentPrices.put("VEA", 41d);
	}
	
	{
		alernateCorrelatedFund.put("VTI", "SCHB");
		alernateCorrelatedFund.put("VEA", "SCHF");
		alernateCorrelatedFund.put("VWO", "IEMG");
		alernateCorrelatedFund.put("VIG", "SCHD");
		alernateCorrelatedFund.put("XLE", "VDE");
		alernateCorrelatedFund.put("SCHP", "VTIP");
		alernateCorrelatedFund.put("MUB", "TFI");
	}
	
	public Double getCurrentPrice(String ticker){
		return currentPrices.get(ticker);
	}
	
	public String getAlternateCorrelatedFund(String ticker){
		return alernateCorrelatedFund.get(ticker);
	}
}

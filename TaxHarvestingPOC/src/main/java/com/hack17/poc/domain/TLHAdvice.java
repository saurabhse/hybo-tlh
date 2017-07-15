package com.hack17.poc.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TLHAdvice {
	
	private List<Recommendation> recommendations = new ArrayList<>();
	
	public void addRecommendation(Recommendation recommendation){
		recommendations.add(recommendation);
	}
	

}

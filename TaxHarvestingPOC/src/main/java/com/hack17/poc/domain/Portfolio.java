package com.hack17.poc.domain;

import java.util.ArrayList;
import java.util.List;

public class Portfolio {
	private List<Allocation> allocations = new ArrayList<>();
	private InvestorProfile investorProfile;

	public List<Allocation> getAllocations() {
		// TODO Auto-generated method stub
		return allocations;
	}

	public void addAllocation(Allocation allocation) {
		allocations.add(allocation);
		
	}

	public InvestorProfile getInvestorProfile() {
		return investorProfile;
	}

	public void setInvestorProfile(InvestorProfile investorProfile) {
		this.investorProfile = investorProfile;
	}
	
	

}

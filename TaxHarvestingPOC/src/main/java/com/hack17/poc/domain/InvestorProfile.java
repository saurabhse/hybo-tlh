package com.hack17.poc.domain;

import java.util.Date;

import lombok.Data;

@Data
public class InvestorProfile {

	final private Date dateOfBirth;
	final private RiskTolerance riskTolerance;
	final private InvestmentTimeHorizon timeHorizon;

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public RiskTolerance getRiskTolerance() {
		return riskTolerance;
	}

	public InvestmentTimeHorizon getTimeHorizon() {
		return timeHorizon;
	}

}

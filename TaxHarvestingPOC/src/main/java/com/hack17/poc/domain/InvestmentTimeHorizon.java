package com.hack17.poc.domain;

import java.util.Date;

import lombok.Data;

@Data
public class InvestmentTimeHorizon {

	final private Integer months;
	final private Date asOf;
	
	public int getYears() {
		return months/12;
	}

}

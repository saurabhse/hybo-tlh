package com.hack17.poc.domain;

import java.util.Date;

import lombok.Data;

@Data
public class Allocation {
	
	final private Fund fund;
	final private double costPrice;
	final private int quantity;
	final private double percentage;
	final private Date transactionDate;
	final private double expenseRatio;
	
	


}

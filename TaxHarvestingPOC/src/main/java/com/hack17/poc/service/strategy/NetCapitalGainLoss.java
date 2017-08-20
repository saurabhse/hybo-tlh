package com.hack17.poc.service.strategy;

import lombok.Data;

@Data
public class NetCapitalGainLoss {
	private Double shortTermCapitalGain;
	private Double longTermCapitalGain;
	private Double netCapitalLoss;
}

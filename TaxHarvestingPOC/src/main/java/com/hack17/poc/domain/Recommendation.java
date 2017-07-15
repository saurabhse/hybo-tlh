package com.hack17.poc.domain;

import lombok.Data;

@Data
public class Recommendation {
	final private String ticker1;
	final private String ticker2;
	final private Action action;
}

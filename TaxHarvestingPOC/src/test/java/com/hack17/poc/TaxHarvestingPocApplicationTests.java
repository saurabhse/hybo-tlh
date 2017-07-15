package com.hack17.poc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(Suite.class)
//@SpringBootTest
@ContextConfiguration(classes=TaxHarvestingPocApplication.class)
@SuiteClasses({InvestorProfileTests.class, PortfolioValidationTests.class, ReferenceDataServiceTests.class, SimpleSellAndBuyThresholdStrategyTests.class})
public class TaxHarvestingPocApplicationTests {

	@Test
	public void contextLoads() {
	}

}

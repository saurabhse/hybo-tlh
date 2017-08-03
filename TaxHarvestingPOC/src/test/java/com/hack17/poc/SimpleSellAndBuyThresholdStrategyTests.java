package com.hack17.poc;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;




import javax.transaction.Transactional;

//import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.hack17.hybo.domain.Action;
import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.TLHAdvice;
import com.hack17.hybo.repository.PortfolioRepository;
import com.hack17.hybo.repository.TLHAdvisorRepository;
import com.hack17.hybo.service.TLHAdvisorService;
import com.hack17.hybo.util.DateTimeUtil;
import com.hack17.hybo.util.ReportUtil;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes={TestTaxHarvestingPocApplication.class})
public class SimpleSellAndBuyThresholdStrategyTests {
	private static Logger logger = LoggerFactory.getLogger(SimpleSellAndBuyThresholdStrategyTests.class);
	//private static Logger logger = Logger.getLogger(SimpleSellAndBuyThresholdStrategyTests.class);
	
	@Autowired
	PortfolioRepository portfolioRepository;
	@Autowired
	private TLHAdvisorService tlhAdvisorService;
	@Autowired
	private TLHAdvisorRepository tlhAdivsorRepo;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void noStockInlossBeyondThreshold() {
		Date date1 = DateTimeUtil.getDate2("01-NOV-2012");
		Date date2 = DateTimeUtil.getDate2("01-MAY-2013");
		Portfolio portfolio = loadPortfolio();
		logger.info(ReportUtil.format(portfolio, date1));
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, date2);
		assertEquals(0,  tlhAdvice.getRecommendations().size());
		logger.info(ReportUtil.format(portfolio, date2));
	}
	
	@Test
	@Transactional
	public void recommendationForSell() {
		Date date1 = DateTimeUtil.getDate2("30-OCT-2013");
		Portfolio portfolio = loadPortfolio();
		//logger.info(ReportUtil.format(portfolio, date1));
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, date1);
		assertEquals(1,  tlhAdvice.getRecommendations().size());
		assertEquals(Action.SELL, tlhAdvice.getRecommendations().get(0).getAction());
		tlhAdvisorService.execute(tlhAdvice, portfolio);
		logger.info(ReportUtil.format(portfolio, date1));
		Date date2 = DateTimeUtil.getDate2("1-APR-2014");
		tlhAdvice = tlhAdvisorService.advise(portfolio, date2);
		//assertEquals(1,  tlhAdvice.getRecommendations().size());
		//assertEquals(Action.SELL, tlhAdvice.getRecommendations().get(0).getAction());
		tlhAdvisorService.execute(tlhAdvice, portfolio);
		logger.info(ReportUtil.format(portfolio, date2));
	}
	
	@Test
	@Ignore
	public void recommendationForSellWithWashSale() {
		long portfolioId = 103;
		Portfolio portfolio = portfolioRepository.getPortfolio(portfolioId);
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, new Date());
		assertEquals(2,  tlhAdvice.getRecommendations().size());
		assertEquals(Action.HOLD, tlhAdvice.getRecommendations().get(0).getAction());
		assertEquals(Action.HOLD, tlhAdvice.getRecommendations().get(1).getAction());

	}
	
	private List<Allocation> getAllocations(long portfolioId) {
		Portfolio portfolio = portfolioRepository.getPortfolio(portfolioId);
		List<Allocation> allocations = portfolio.getAllocations();
		return allocations;
	}
	
	private Portfolio loadPortfolio(){
		return portfolioRepository.getAllPortfolios().get(0);
	}

}
